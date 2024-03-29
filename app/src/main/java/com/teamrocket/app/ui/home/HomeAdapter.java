package com.teamrocket.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.squareup.picasso.Picasso;
import com.teamrocket.app.R;
import com.teamrocket.app.model.BirdSighting;

import java.util.ArrayList;
import java.util.List;

public class HomeAdapter extends Adapter<HomeAdapter.BirdSightingViewHolder> {

    private ArrayList<BirdSighting> sightings = new ArrayList<>();
    private Listener listener;

    public HomeAdapter(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BirdSightingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_home_item, parent, false);
        return new BirdSightingViewHolder(view, listener);
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

    public void removeSighting(BirdSighting sighting) {
        int index = this.sightings.indexOf(sighting);
        if (index == -1) return;

        this.sightings.remove(index);
        notifyItemRemoved(index);
    }

    public interface Listener {
        void onClick(BirdSighting sighting);

        void onDeleteClick(BirdSighting sighting);

        void onShareClick(BirdSighting sighting);
    }

    static class BirdSightingViewHolder extends ViewHolder {
        private TextView textTitle;
        private ImageView imageBird;
        private ImageButton btnShare;
        private ImageButton btnDelete;

        private Listener listener;

        BirdSightingViewHolder(@NonNull View itemView, Listener listener) {
            super(itemView);
            this.listener = listener;
            textTitle = itemView.findViewById(R.id.textTitleItemSighting);
            imageBird = itemView.findViewById(R.id.imageItemSighting);
            btnDelete = itemView.findViewById(R.id.btnDeleteItemSighting);
            btnShare = itemView.findViewById(R.id.btnShareItemSighting);
        }

        void bind(BirdSighting birdSighting) {
            textTitle.setText(birdSighting.getBird().getName());
            Picasso.get().load(birdSighting.getBird().getUriPath()).fit().centerCrop().into(imageBird);
            btnDelete.setOnClickListener(v -> listener.onDeleteClick(birdSighting));
            btnShare.setOnClickListener(v -> listener.onShareClick(birdSighting));

            itemView.setOnClickListener(v -> listener.onClick(birdSighting));
        }
    }
}
