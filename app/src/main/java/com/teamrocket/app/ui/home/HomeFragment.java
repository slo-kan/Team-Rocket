package com.teamrocket.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    public static final String TAG = "homeFragment";

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
                .setPositiveButton(android.R.string.ok, null)
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
}
