package com.teamrocket.app.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.sqlite.db.SimpleSQLiteQuery;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.reactivex.disposables.CompositeDisposable;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static com.teamrocket.app.data.db.BirdSightingDao.Listener.ADDED;
import static com.teamrocket.app.data.db.BirdSightingDao.Listener.DELETED;

public class HomeFragment extends Fragment {

    public static final String TAG = "homeFragment";

    private static final int RC_LOCATION = 243;

    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;
    private Location lastLocation = null;

    private CategoryDao categoryDao;
    private CategoryDao.Listener categoryListener;

    private BirdSightingDao dao;
    private BirdSightingDao.Listener listener;

    private HomeAdapter adapter;

    private ProgressBar progressBar;

    private View emptyView;
    private TextView emptyText;
    private View filterView;

    private CompositeDisposable compositeDisposable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        listener = (state, sighting) -> {
            if (state == ADDED) {
                adapter.addSighting(sighting);
                emptyView.setVisibility(View.GONE);
            } else if (state == DELETED) {
                adapter.removeSighting(sighting);
                if (adapter.getItemCount() == 0) emptyView.setVisibility(View.VISIBLE);
            }
        };
        dao = ((BTApplication) getActivity().getApplication()).getBirdSightingDao();
        dao.addListener(listener);

        compositeDisposable = new CompositeDisposable();

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

        adapter = new HomeAdapter(new HomeAdapter.Listener() {
            @Override
            public void onClick(BirdSighting sighting) {
                if (sighting.getLocation().getLat() == -1000) {
                    return;
                }

                MainActivity activity = (MainActivity) getActivity();
                //Telling the map fragment to not reset filters when the navigation goes through
                //the Main activity.
                activity.getMapFragment().shouldResetFilters = false;

                activity.setBottomNavSelection(R.id.main_nav_map);
                activity.getMapFragment().filterBird(sighting.getBird());
            }

            @Override
            public void onDeleteClick(BirdSighting sighting) {
                new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.home_title_delete)
                        .setMessage(R.string.home_msg_delete)
                        .setPositiveButton(android.R.string.ok, (d, w) -> dao.delete(sighting))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }

            @Override
            public void onShareClick(BirdSighting sighting) {
                String uriPath = sighting.getBird().getUriPath();
                Uri imageUri = uriPath.startsWith("file://") ? FileProvider.getUriForFile(requireActivity(),
                        "com.teamrocket.app.fileprovider", new File(sighting.getBird().getImagePath()))
                        : Uri.parse(sighting.getBird().getUriPath());

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.home_msg_share));
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setType("image/jpeg");

                startActivity(Intent.createChooser(shareIntent, null));
            }
        });

        progressBar = view.findViewById(R.id.progressHome);

        emptyView = view.findViewById(R.id.viewNoSightings);
        emptyText = emptyView.findViewById(R.id.textEmpty);

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

        boolean isPortrait = getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;

        RecyclerView recyclerView = view.findViewById(R.id.recyclerHome);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), isPortrait ? 2 : 4));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && btnAddSighting.isExtended()) btnAddSighting.shrink();
                else if (dy < 0 && !btnAddSighting.isExtended()) btnAddSighting.extend();
            }
        });

        progressBar.setVisibility(View.VISIBLE);
        compositeDisposable.add(
                dao.getAllAsync()
                        .subscribe(sightings -> {
                            progressBar.setVisibility(View.GONE);
                            adapter.update(sightings);
                            if (sightings.isEmpty()) emptyView.setVisibility(View.VISIBLE);
                        }, throwable -> {
                            //ignored
                        })
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (Utils.isLocationPermissionGranted(grantResults)) {
            boolean shouldShowLocationFilters = Utils.isGpsEnabled(requireContext());

            View locationWarning = filterView.findViewById(R.id.dialog_filter_location_warning);
            locationWarning.setVisibility(shouldShowLocationFilters ? View.GONE : View.VISIBLE);

            ChipGroup cgLocation = filterView.findViewById(R.id.cgLocation);
            for (Chip chip : getChips(cgLocation)) chip.setEnabled(shouldShowLocationFilters);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dao.removeListener(listener);
        categoryDao.removeListener(categoryListener);
        compositeDisposable.clear();
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
                && Utils.isGpsEnabled(requireContext()) && lastLocation != null;

//        for (Chip chip : getChips(cgLocation)) chip.setEnabled(shouldShowLocationFilters);
        View locationWarning = filterView.findViewById(R.id.dialog_filter_location_warning);
        locationWarning.setVisibility(shouldShowLocationFilters ? View.GONE : View.VISIBLE);
//
//        if (!Utils.isLocationPermissionGranted(requireContext())) {
//            Utils.requestLocationPermission(this, RC_LOCATION);
//        }

        List<Chip> chips = getChips(cgCategories);
        List<Boolean> selectedCategories = chips
                .stream()
                .map(CompoundButton::isChecked)
                .collect(Collectors.toList());

        int selectedDateChip = cgDate.getCheckedChipId();
        int selectedLocationChip = cgLocation.getCheckedChipId();

        new AlertDialog.Builder(requireActivity())
                .setView(filterView)
                .setTitle(R.string.home_title_filter_sightings)
                .setNeutralButton(R.string.home_title_reset_filters, (d, w) -> {
                    emptyText.setText(R.string.home_msg_no_sightings);
                    cgDate.check(View.NO_ID);
                    cgLocation.check(View.NO_ID);
                    for (int i = 0; i < chips.size(); i++) chips.get(i).setChecked(false);
                    filter();
                })
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    emptyText.setText(R.string.home_msg_no_filters);
                    filter();
                })
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

        List<BirdSighting> filteredSightings = dao.filter(new SimpleSQLiteQuery(query));
        adapter.update(filteredSightings);

        if (selectedCategories.isEmpty() && dateFilter == -1 && locationFilter == -1) {
            emptyText.setText(R.string.home_msg_no_sightings);
        }

        if (filteredSightings.isEmpty()) emptyView.setVisibility(View.VISIBLE);
        else emptyView.setVisibility(View.GONE);
    }

    private String getFilterQuery(List<String> categories, int dateFilter, int locationFilter) {
        String query = "SELECT * FROM birdsighting ";

        if ((categories != null && !categories.isEmpty()) || dateFilter != -1 || (locationFilter != -1 && lastLocation != null)) {
            query += "WHERE ";
        }

        if (categories != null && !categories.isEmpty()) {
            query += "family in ";
            query += categories.stream()
                    .map(this::getEnglishCategoryName)
                    .map(st -> "'" + st + "'")
                    .collect(Collectors.joining(", ", "(", ") "));
        }

        if (dateFilter != -1) {
            if (!query.trim().endsWith("WHERE")) {
                query += " AND ";
            }

            long timeDiff = dateFilter == R.id.filter_chip_day ? TimeUnit.HOURS.toMillis(24)
                    : dateFilter == R.id.filter_chip_week ? TimeUnit.DAYS.toMillis(7)
                    : TimeUnit.DAYS.toMillis(30);
            long currentTime = System.currentTimeMillis();

            long from = currentTime - timeDiff;
            query += " time > " + from + " AND time < " + currentTime;
        }

        if (locationFilter != -1) {

            int distKm = locationFilter == R.id.filter_chip_no_location ? -1
                    : locationFilter == R.id.filter_chip_any_location ? -2
                    : locationFilter == R.id.filter_chip_1_km ? 1 : 10;

            if (distKm == -1 || distKm == -2) {
                if (query.trim().endsWith("birdsighting")) {
                    query += "WHERE ";
                }

                if (!query.trim().endsWith("WHERE")) {
                    query += " AND ";
                }

                query += distKm == -1 ? "lat < -999 AND lon < -999" : "lat > -999 AND lon > -999";
            } else if (lastLocation != null) {
                if (!query.trim().endsWith("WHERE")) {
                    query += " AND ";
                }

                LatLng loc = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                double[] deviations = Utils.getCoordinateDeviations(loc, distKm);

                double latA = loc.latitude - deviations[0] / 2;
                double latB = loc.latitude + deviations[0] / 2;

                double lonA = loc.longitude - deviations[1] / 2;
                double lonB = loc.longitude + deviations[1] / 2;

                double latMax = Math.max(latA, latB);
                double latMin = Math.min(latA, latB);

                double lonMax = Math.max(lonA, lonB);
                double lonMin = Math.min(lonA, lonB);

                query += " lat > " + latMin + " AND lat < " + latMax;
                query += " AND lon > " + lonMin + " AND lon < " + lonMax;
            } else {
                Toast.makeText(requireContext(), R.string.home_msg_loc_filter_ignored, Toast.LENGTH_LONG).show();
                ChipGroup cgLocation = filterView.findViewById(R.id.cgLocation);
                cgLocation.check(View.NO_ID);
            }
        }

        return query;
    }

    public void showNoLocationBirds() {
        ChipGroup cgCategories = filterView.findViewById(R.id.cgCategories);
        ChipGroup cgDate = filterView.findViewById(R.id.cgDate);
        ChipGroup cgLocation = filterView.findViewById(R.id.cgLocation);

        List<Chip> chips = getChips(cgCategories);

        emptyText.setText(R.string.home_msg_no_sightings);
        cgDate.check(View.NO_ID);
        cgLocation.check(R.id.filter_chip_no_location);
        for (int i = 0; i < chips.size(); i++) chips.get(i).setChecked(false);
        filter();
    }

    private String getEnglishCategoryName(String localisedName) {
        List<String> localisedNames = Arrays.asList(requireActivity().getBaseContext().getResources().getStringArray(R.array.categories));

        Configuration conf = getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(new Locale("en"));
        Context localizedContext = requireActivity().createConfigurationContext(conf);
        Resources res = localizedContext.getResources();

        List<String> names = Arrays.asList(res.getStringArray(R.array.categories));

        return localisedNames.contains(localisedName)
                ? names.get(localisedNames.indexOf(localisedName))
                : localisedName;
    }
}
