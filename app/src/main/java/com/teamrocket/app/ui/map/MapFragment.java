package com.teamrocket.app.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.teamrocket.app.R;
import com.teamrocket.app.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    public static final String TAG = "mapFragment";

    private GoogleMap map;
    private List<MarkerOptions> markers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LatLng location = new LatLng(52.098361, 11.620763);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(map -> {
            this.map = map;
            this.map.setOnMarkerClickListener(marker -> {
                Toast.makeText(getContext(), "Marking a random location", Toast.LENGTH_SHORT).show();

                LatLng loc = Utils.getRandomLocation(marker.getPosition(), 1000);
                MarkerOptions options = new MarkerOptions().position(loc).title("Marker");
                markers.add(options);

                this.map.addMarker(options);
                updateMapZoom();

                return true;
            });

            MarkerOptions currentLocation = new MarkerOptions().position(location).title("Magdeburg");
            markers.add(currentLocation);

            this.map.addMarker(currentLocation);
            updateMapZoom();
        });
    }

    private void updateMapZoom() {
        if (this.map == null) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (MarkerOptions m : markers) {
            boundsBuilder.include(m.getPosition());
        }
        LatLngBounds bounds = boundsBuilder.build();

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80));
    }
}
