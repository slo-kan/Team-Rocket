package com.teamrocket.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.teamrocket.app.R;
import com.teamrocket.app.model.BirdSighting;

import java.util.ArrayList;
import java.util.List;

public class HomeAdapter extends Adapter<HomeAdapter.BirdSightingViewHolder> {

    private ArrayList<BirdSighting> sightings = new ArrayList<>();

    @NonNull
    @Override
    public BirdSightingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_home_item, parent, false);
        return new BirdSightingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BirdSightingViewHolder holder, int position) {
        holder.bind(sightings.get(position));
    }

    @Override
    public int getItemCount() {
        return sightings.size();
    }

    public void update(List<BirdSighting> sightings) {
        this.sightings.clear();
        this.sightings.addAll(sightings);
        notifyDataSetChanged();
    }

    public void addSighting(BirdSighting sighting) {
        this.sightings.add(sighting);
        notifyItemInserted(this.sightings.size());
    }

    static class BirdSightingViewHolder extends ViewHolder {
        private TextView textTitle;

        BirdSightingViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitleItemSighting);
        }

        void bind(BirdSighting birdSighting) {
            textTitle.setText(birdSighting.getBird().getName());
        }
    }
}
