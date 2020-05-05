package com.teamrocket.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

import java.util.Random;

public class HomeFragment extends Fragment {

    public static final String TAG = "homeFragment";

    private BirdSightingDao dao;
    private HomeAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        dao = ((BTApplication) getActivity().getApplication()).getBirdSightingDao();

        ((AppCompatActivity) getActivity()).setSupportActionBar(view.findViewById(R.id.toolbarHome));

        adapter = new HomeAdapter();

        ExtendedFloatingActionButton btnAddSighting = view.findViewById(R.id.btnAddSighting);
        btnAddSighting.setOnClickListener(v -> addBirdSighting());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerHome);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        adapter.update(dao.getAll());
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

        Toast.makeText(getContext(), "Saving a bird sighting took " + (after - before) + " ms", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
