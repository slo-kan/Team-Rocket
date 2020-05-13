package com.teamrocket.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.teamrocket.app.BTApplication;
import com.teamrocket.app.R;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.model.Bird;
import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.ui.add.AddSightingActivity;
import com.teamrocket.app.ui.main.MainActivity;

import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment {

    public static final String TAG = "homeFragment";


    private BirdSightingDao dao;
    private HomeAdapter adapter;

    private BirdSightingDao.Listener listener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        listener = sighting -> adapter.addSighting(sighting);

        dao = ((BTApplication) getActivity().getApplication()).getBirdSightingDao();
        dao.addListener(listener);

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).setSupportActionBar(view.findViewById(R.id.toolbarHome));

        adapter = new HomeAdapter(sighting -> {
            List<BirdSighting> similarBirds = dao.findSimilar(sighting);
            MainActivity activity = (MainActivity) getActivity();
            activity.getMapFragment().setSightingData(similarBirds);
            activity.setBottomNavSelection(R.id.main_nav_map);
        });

        ExtendedFloatingActionButton btnAddSighting = view.findViewById(R.id.btnAddSighting);
        btnAddSighting.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddSightingActivity.class));

        });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerHome);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && btnAddSighting.isExtended()) btnAddSighting.shrink();
                else if (dy < 0 && !btnAddSighting.isExtended()) btnAddSighting.extend();
            }
        });

        adapter.update(dao.getAll());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dao.removeListener(listener);
    }

    private void addBirdSighting() {
        Bird bird = new Bird();

        Random random = new Random();

        bird.setName("Bird #" + random.nextInt(1000));
        bird.setImagePath("");
        bird.setFamily("Family #" + random.nextInt(25));
        bird.setColor("violet");
        bird.setSize(random.nextInt(3));

        BirdSighting.Location location = new BirdSighting.Location(0, 0);
        BirdSighting sighting = new BirdSighting(bird, location, System.currentTimeMillis());

        long before = System.currentTimeMillis();
        dao.insert(sighting);
        long after = System.currentTimeMillis();

//        Toast.makeText(getContext(), "Saving a bird sighting took " + (after - before) + " ms", Toast.LENGTH_SHORT).show();
    }

}
