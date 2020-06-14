package com.teamrocket.app.ui.map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.squareup.picasso.Picasso;
import com.teamrocket.app.BTApplication;
import com.teamrocket.app.R;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.data.network.IWikiApi;
import com.teamrocket.app.model.Bird;
import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.model.WikiResponse;
import com.teamrocket.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.widget.Toast.LENGTH_SHORT;

public class MapFragment extends Fragment {

    public static final String TAG = "mapFragment";
    private static final int RC_LOCATION = 432;
    private static final String URL_WIKIPEDIA = "https://wikipedia.org/wiki/%s";

    private LatLng lastLocation;
    private FusedLocationProviderClient locationProvider;
    private GoogleMap map;

    private List<BirdSighting> sightings = new ArrayList<>();
    private List<MarkerOptions> markers = new ArrayList<>();

    //This variable is used to check whether filters can be reset when the user clicks 'Map'
    //from the bottom navigation bar. Since we are delegating list item clicks and bottom nav bar
    // clicks to the bottom nav bar itself, we need this variable to distinguish
    // which the user actually clicked.
    public boolean shouldResetFilters = true;
    private Bird currentFilteredBird = null;

    private BirdSightingDao dao;
    private BirdSightingDao.Listener listener;

    private IWikiApi wikiApi;
    private View dialogView;
    private View dialogContent;
    private ProgressBar dialogProgress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        dao = ((BTApplication) getActivity().getApplication()).getBirdSightingDao();
        listener = sighting -> {
            if (currentFilteredBird == null || sighting.getBird().equals(currentFilteredBird)) {
                this.sightings.add(sighting);
                BirdSighting.Location birdLoc = sighting.getLocation();
                LatLng location = new LatLng(birdLoc.getLat(), birdLoc.getLon());
                markers.add(new MarkerOptions().position(location));
                showMapMarkers();
            }
        };
        dao.addListener(listener);
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dao.removeListener(listener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity());

        filterBird(currentFilteredBird, true);

        if (!Utils.isLocationPermissionGranted(getContext())) {
            Utils.requestLocationPermission(this, RC_LOCATION);
        } else if (!Utils.isGpsEnabled(requireContext())) {
            Utils.showInfoDialog(requireContext(), R.string.map_gps_dialog_title, R.string.map_gps_dialog_message);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(map -> {
            this.map = map;
            if (Utils.isLocationPermissionGranted(getContext())) {
                this.map.setMyLocationEnabled(true);
                this.map.getUiSettings().setMyLocationButtonEnabled(false);
                updateMapZoom();
            }

            map.setOnMarkerClickListener(marker -> {
                long sightingId = (long) marker.getTag();
                BirdSighting sighting = dao.getSighting(sightingId);
                showSightingInfo(sighting);
                return true;
            });

            showMapMarkers();
            getLocationAndZoom();
        });

        wikiApi = new Retrofit.Builder()
                .baseUrl(IWikiApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(IWikiApi.class);

        dialogView = View.inflate(requireContext(), R.layout.map_dialog_info, null);
        dialogContent = dialogView.findViewById(R.id.map_dialog_content);
        dialogProgress = dialogView.findViewById(R.id.progress_dialog_info);
    }

    private void showMapMarkers() {
        if (map == null) return;

        map.clear();
        for (int i = 0; i < markers.size(); i++) {
            Marker m = map.addMarker(markers.get(i));
            m.setTag(sightings.get(i).sightingId);
            getMarkerIcon(m, sightings.get(i));
        }
        updateMapZoom();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (map == null) return;

        if (requestCode == RC_LOCATION && Utils.isLocationPermissionGranted(grantResults)) {
            getLocationAndZoom();
        }
    }

    private void showSightingInfo(BirdSighting sighting) {
        ViewGroup parent = (ViewGroup) dialogView.getParent();
        if (parent != null) {
            parent.removeView(dialogView);
        }

        ImageView image = dialogView.findViewById(R.id.map_dialog_image);
        TextView textName = dialogView.findViewById(R.id.map_dialog_name);
        TextView textFamily = dialogView.findViewById(R.id.map_dialog_family);
        TextView textDate = dialogView.findViewById(R.id.map_dialog_time);
        TextView textNotes = dialogView.findViewById(R.id.map_dialog_notes);

        View contentNotes = dialogView.findViewById(R.id.map_dialog_content_notes);

        dialogView.findViewById(R.id.map_dialog_read_on_wikipedia).setOnClickListener(v -> {
            String url = String.format(URL_WIKIPEDIA, sighting.getBird().getName());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(url));
            startActivity(browserIntent);
        });

        dialogProgress.setVisibility(View.VISIBLE);
        dialogContent.setVisibility(View.GONE);
        contentNotes.setVisibility(sighting.getNotes().isEmpty() ? View.GONE : View.VISIBLE);

        Picasso.get().load(sighting.getBird().getUriPath()).fit().centerCrop().into(image);
        textName.setText(sighting.getBird().getName());
        textFamily.setText(sighting.getBird().getFamily());
        textDate.setText(Utils.formatDate(sighting.getTime()));
        textNotes.setText(sighting.getNotes());

        AlertDialog infoDialog = new AlertDialog.Builder(requireActivity())
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        infoDialog.show();

        wikiApi.getBirdInformation(sighting.getBird().getName()).enqueue(new Callback<WikiResponse>() {
            @Override
            public void onResponse(Call<WikiResponse> call, Response<WikiResponse> response) {
                WikiResponse info = response.body();
                if (info == null) {
                    Toast.makeText(getContext(), R.string.add_sighting_error_wiki, LENGTH_SHORT).show();
                    dialogContent.setVisibility(View.GONE);
                    dialogProgress.setVisibility(View.GONE);
                    return;
                }

                if (infoDialog.isShowing()) {
                    ((TextView) dialogView.findViewById(R.id.map_dialog_description)).setText(info.getDescription());
                    dialogProgress.setVisibility(View.GONE);
                    dialogContent.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<WikiResponse> call, Throwable t) {
                dialogContent.setVisibility(View.GONE);
                dialogProgress.setVisibility(View.GONE);
            }
        });
    }

    private void updateMapZoom() {
        if (this.map == null) return;
        if (lastLocation == null && markers.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        if (lastLocation != null) {
            boundsBuilder.include(lastLocation);
        }

        for (MarkerOptions m : markers) {
            boundsBuilder.include(m.getPosition());
        }
        LatLngBounds bounds = boundsBuilder.build();

        int padding = Utils.toDp(196, requireContext());
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation(OnSuccessListener<Location> onSuccess) {
        this.locationProvider.getLastLocation().addOnSuccessListener(onSuccess);
    }

    @SuppressLint("MissingPermission")
    private void getLocationAndZoom() {
        getLastLocation(loc -> {
            if (loc == null) return;
            this.lastLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
            map.setMyLocationEnabled(true);
            updateMapZoom();
        });
    }

    public void filterBird(Bird filterBird) {
        filterBird(filterBird, false);
    }

    public void filterBird(Bird filterBird, boolean force) {
        if (!shouldResetFilters) {
            shouldResetFilters = true;
        }

        if (currentFilteredBird == filterBird && !force) {
            return;
        }

        //DAO might be null if the fragment is not attached yet.
        //Showing the markers will be handled by onCreateView when the fragment is attached
        if (dao == null) {
            currentFilteredBird = filterBird;
            return;
        }

        this.sightings = filterBird == null
                ? dao.getAll() : dao.findSimilar(filterBird);

        this.markers.clear();
        for (BirdSighting sighting : sightings) {
            BirdSighting.Location sLocation = sighting.getLocation();
            LatLng location = new LatLng(sLocation.getLat(), sLocation.getLon());
            MarkerOptions options = new MarkerOptions().position(location);
            markers.add(options);
        }

        showMapMarkers();

        currentFilteredBird = filterBird;
    }

    private void getMarkerIcon(Marker marker, BirdSighting birdSighting) {
        int iconSize = Utils.toPx(40, requireContext());

        Picasso picasso = Picasso.get();
        picasso.setLoggingEnabled(true);
        picasso.load(birdSighting.getBird().getUriPath())
                .resize(iconSize, iconSize)
                .centerCrop()
                //TODO: ImageMarker is being garbage collected. Store a reference.
                .into(new ImageMarker(requireContext(), marker));
    }
}
