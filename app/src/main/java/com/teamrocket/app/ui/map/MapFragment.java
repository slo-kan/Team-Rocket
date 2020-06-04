package com.teamrocket.app.ui.map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.squareup.picasso.Picasso;
import com.teamrocket.app.BTApplication;
import com.teamrocket.app.R;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.model.Bird;
import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static androidx.core.content.ContextCompat.getDrawable;

public class MapFragment extends Fragment {

    public static final String TAG = "mapFragment";
    private static final int RC_LOCATION = 432;

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

            showMapMarkers();
            getLocationAndZoom();
        });
    }

    private void showMapMarkers() {
        if (map == null) return;

        map.clear();
        for (int i = 0; i < markers.size(); i++) {
            Marker m = map.addMarker(markers.get(i));
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

        Picasso.get().load(birdSighting.getBird().getUriPath())
                .resize(iconSize, iconSize)
                .centerCrop()
                .into(new ImageMarker(requireContext(), marker));
    }
}
