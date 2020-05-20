package com.teamrocket.app.ui.map;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.teamrocket.app.BTApplication;
import com.teamrocket.app.R;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.model.Bird;
import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    public static final String TAG = "mapFragment";
    private static final int RC_LOCATION = 432;

    private LatLng lastLocation;
    private FusedLocationProviderClient locationProvider;
    private GoogleMap map;

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
            //TODO: Take into consideration the filters before updating markers
            LatLng location = new LatLng(sighting.getLocation().getLat(), sighting.getLocation().getLon());
            markers.add(new MarkerOptions()
                    .position(location));
            showMapMarkers();
        };
        dao.addListener(listener);
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dao.removeListener(listener);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity());

        filterBird(null, true);

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

            this.map.setOnMarkerClickListener(marker -> {
                onMarkerClicked(marker);
                return true;
            });

            showMapMarkers();
            getLocationAndZoom();
        });
    }

    private void showMapMarkers() {
        if (map == null) return;

        map.clear();
        for (MarkerOptions marker : markers) {
            map.addMarker(marker);
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

    private void getLastLocation(OnSuccessListener<Location> onSuccess) {
        this.locationProvider.getLastLocation().addOnSuccessListener(onSuccess);
    }

    private void getLocationAndZoom() {
        getLastLocation(loc -> {
            if (loc == null) return;
            this.lastLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
            map.setMyLocationEnabled(true);
            updateMapZoom();
        });
    }

    private void onMarkerClicked(Marker marker) {
        Toast.makeText(getContext(), "Marking a random location", Toast.LENGTH_SHORT).show();

        LatLng loc = Utils.getRandomLocation(marker.getPosition(), 1000);
        MarkerOptions options = new MarkerOptions().position(loc).title("Marker");
        markers.add(options);

        this.map.addMarker(options);
        updateMapZoom();
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

        List<BirdSighting> sightings = filterBird == null
                ? dao.getAll() : dao.findSimilar(filterBird);

        this.markers.clear();
        for (BirdSighting sighting : sightings) {
            BirdSighting.Location sLocation = sighting.getLocation();
            LatLng location = new LatLng(sLocation.getLat(), sLocation.getLon());
            markers.add(new MarkerOptions().position(location));
        }

        showMapMarkers();

        currentFilteredBird = filterBird;
    }
}
