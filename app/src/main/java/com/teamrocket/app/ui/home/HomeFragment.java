package com.teamrocket.app.ui.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.teamrocket.app.BTApplication;
import com.teamrocket.app.R;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.data.db.CategoryDao;
import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.ui.add.AddSightingActivity;
import com.teamrocket.app.ui.main.MainActivity;
import com.teamrocket.app.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    public static final String TAG = "homeFragment";

    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;
    private Location lastLocation = null;

    private CategoryDao categoryDao;
    private CategoryDao.Listener categoryListener;

    private BirdSightingDao dao;
    private BirdSightingDao.Listener listener;

    private HomeAdapter adapter;

    private View emptyView;
    private View filterView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        listener = sighting -> {
            adapter.addSighting(sighting);
            emptyView.setVisibility(View.GONE);
        };
        dao = ((BTApplication) getActivity().getApplication()).getBirdSightingDao();
        dao.addListener(listener);

        categoryListener = category -> {
            Chip chip = (Chip) View.inflate(requireContext(), R.layout.home_dialog_filter_chip, null);
            chip.setText(category.getName());
            ((ChipGroup) filterView.findViewById(R.id.cgCategories)).addView(chip);
        };
        categoryDao = ((BTApplication) getActivity().getApplication()).getCategoryDao();
        categoryDao.addListener(categoryListener);

        locationProvider = LocationServices.getFusedLocationProviderClient(requireActivity());
        if (Utils.isLocationPermissionGranted(requireContext())) {
            startLocationUpdates();
        }

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).setSupportActionBar(view.findViewById(R.id.toolbarHome));

        adapter = new HomeAdapter(sighting -> {
            MainActivity activity = (MainActivity) getActivity();
            //Telling the map fragment to not reset filters when the navigation goes through
            //the Main activity.
            activity.getMapFragment().shouldResetFilters = false;

            activity.setBottomNavSelection(R.id.main_nav_map);
            activity.getMapFragment().filterBird(sighting.getBird());
        });

        emptyView = view.findViewById(R.id.viewNoSightings);
        filterView = View.inflate(requireContext(), R.layout.home_dialog_filter, null);
        ChipGroup cgCategories = filterView.findViewById(R.id.cgCategories);

        for (String category : categoryDao.getAll(requireActivity().getBaseContext())) {
            Chip chip = (Chip) View.inflate(requireContext(), R.layout.home_dialog_filter_chip, null);
            chip.setText(category);
            cgCategories.addView(chip);
        }

        ExtendedFloatingActionButton btnAddSighting = view.findViewById(R.id.btnAddSighting);
        btnAddSighting.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddSightingActivity.class));

        });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerHome);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && btnAddSighting.isExtended()) btnAddSighting.shrink();
                else if (dy < 0 && !btnAddSighting.isExtended()) btnAddSighting.extend();
            }
        });

        List<BirdSighting> sightings = dao.getAll();
        adapter.update(sightings);
        if (sightings.isEmpty()) emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dao.removeListener(listener);
        categoryDao.removeListener(categoryListener);
        stopLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(120 * 1000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) lastLocation = locationResult.getLastLocation();
            }
        };

        locationProvider.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            locationProvider.removeLocationUpdates(locationCallback);
        }
    }

    public void showFilterDialog() {
        ViewGroup parent = (ViewGroup) filterView.getParent();
        if (parent != null) parent.removeView(filterView);

        ChipGroup cgCategories = filterView.findViewById(R.id.cgCategories);
        ChipGroup cgDate = filterView.findViewById(R.id.cgDate);
        ChipGroup cgLocation = filterView.findViewById(R.id.cgLocation);

        boolean shouldShowLocationFilters = Utils.isLocationPermissionGranted(requireContext())
                && Utils.isGpsEnabled(requireContext());

        //TODO: Solve for when chip is enabled and then gps is turned off
        for (Chip chip : getChips(cgLocation)) chip.setEnabled(shouldShowLocationFilters);
        View locationWarning = filterView.findViewById(R.id.dialog_filter_location_warning);
        locationWarning.setVisibility(shouldShowLocationFilters ? View.GONE : View.VISIBLE);

        List<Chip> chips = getChips(cgCategories);
        List<Boolean> selectedCategories = chips
                .stream()
                .map(CompoundButton::isChecked)
                .collect(Collectors.toList());

        int selectedDateChip = cgDate.getCheckedChipId();
        int selectedLocationChip = cgLocation.getCheckedChipId();

        new AlertDialog.Builder(requireActivity())
                .setView(filterView)
                .setTitle("Filter sightings")
                .setNeutralButton(R.string.home_title_reset_filters, (d, w) -> {

                })
                .setPositiveButton(android.R.string.ok, (d, w) -> filter())
                .setNegativeButton(android.R.string.cancel, (d, w) -> {
                    cgDate.check(selectedDateChip);
                    cgLocation.check(selectedLocationChip);
                    for (int i = 0; i < chips.size(); i++) chips.get(i).setChecked(selectedCategories.get(i));
                })
                .setOnCancelListener(dialog -> {
                    cgDate.check(selectedDateChip);
                    cgLocation.check(selectedLocationChip);
                    for (int i = 0; i < chips.size(); i++) chips.get(i).setChecked(selectedCategories.get(i));
                })
                .show();
    }

    private List<Chip> getChips(ChipGroup chipGroup) {
        List<Chip> chips = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            chips.add((Chip) chipGroup.getChildAt(i));
        }
        return chips;
    }

    private void filter() {
        ChipGroup cgCategories = filterView.findViewById(R.id.cgCategories);
        ChipGroup cgDate = filterView.findViewById(R.id.cgDate);
        ChipGroup cgLocation = filterView.findViewById(R.id.cgLocation);

        List<String> selectedCategories = getChips(cgCategories).stream()
                .filter(CompoundButton::isChecked)
                .map(TextView::getText)
                .map(CharSequence::toString)
                .collect(Collectors.toList());

        int dateFilter = cgDate.getCheckedChipId();
        int locationFilter = cgLocation.getCheckedChipId();

        String query = getFilterQuery(selectedCategories, dateFilter, locationFilter);
    }

    private String getFilterQuery(List<String> categories, int dateFilter, int locationFilter) {
        String query = "SELECT * FROM birdsighting ";

        if ((categories != null && !categories.isEmpty()) || dateFilter != -1 || locationFilter != -1) {
            query += "WHERE ";
        }

        if (categories != null && !categories.isEmpty()) {
            query += "category in ";
            query += categories.stream().collect(Collectors.joining(", ", "(", ") "));
        }

        if (dateFilter != -1) {
            long timeDiff = dateFilter == R.id.filter_chip_day ? TimeUnit.HOURS.toMillis(24)
                    : dateFilter == R.id.filter_chip_week ? TimeUnit.DAYS.toMillis(7)
                    : TimeUnit.DAYS.toMillis(30);
            long currentTime = System.currentTimeMillis();

            long from = currentTime - timeDiff / 2;
            long to = currentTime + timeDiff / 2;

            query += "AND date > " + from + " AND date < " + to;
        }

        if (locationFilter != -1 && lastLocation != null) {
            LatLng loc = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            int distKm = locationFilter == R.id.filter_chip_1_km ? 1 : 10;
            double[] deviations = Utils.getCoordinateDeviations(loc, distKm);

            double latA = loc.latitude - deviations[0] / 2;
            double latB = loc.latitude + deviations[0] / 2;

            double lonA = loc.longitude - deviations[1] / 2;
            double lonB = loc.longitude + deviations[1] / 2;

            double latMax = Math.max(latA, latB);
            double latMin = Math.min(latA, latB);

            double lonMax = Math.max(lonA, lonB);
            double lonMin = Math.min(lonA, lonB);

            query += " AND lat BETWEEN " + latMin + " AND " + latMax;
            query += " AND lon BETWEEN " + lonMin + " AND " + lonMax;
        }

        return query;
    }
}
