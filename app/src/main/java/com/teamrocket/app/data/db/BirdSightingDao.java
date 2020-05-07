package com.teamrocket.app.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.teamrocket.app.model.BirdSighting;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class BirdSightingDao {

    private List<Listener> listeners = new ArrayList<>();

    @Query("SELECT * FROM birdsighting")
    public abstract List<BirdSighting> getAll();

    @Insert
    abstract void _insert(BirdSighting sighting);

    public void insert(BirdSighting sighting) {
        _insert(sighting);
        for (Listener listener : listeners) {
            listener.onAdded(sighting);
        }
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public interface Listener {
        void onAdded(BirdSighting sighting);
    }
}
