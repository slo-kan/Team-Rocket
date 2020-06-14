package com.teamrocket.app.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.teamrocket.app.model.Bird;
import com.teamrocket.app.model.BirdSighting;

import java.util.ArrayList;
import java.util.List;

@Dao
public abstract class BirdSightingDao {

    private List<Listener> listeners = new ArrayList<>();

    @Query("SELECT * FROM birdsighting")
    public abstract List<BirdSighting> getAll();

    @Insert
    abstract long _insert(BirdSighting sighting);

    @Query("SELECT * FROM birdsighting WHERE name LIKE :name AND family LIKE :family")
    abstract List<BirdSighting> _findSimilar(String name, String family);

    @Query("SELECT * FROM birdsighting WHERE sightingId = :sightingId LIMIT 1")
     public abstract BirdSighting getSighting(long sightingId);

    @RawQuery
    public abstract List<BirdSighting> filter(SupportSQLiteQuery query);

    public List<BirdSighting> findSimilar(Bird bird) {
        return _findSimilar(bird.getName(), bird.getFamily());
    }

    public void insert(BirdSighting sighting) {
        sighting.sightingId = _insert(sighting);
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
