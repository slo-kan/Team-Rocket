package com.teamrocket.app.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
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

    @Query("SELECT * FROM birdsighting WHERE lat > -1 AND lon > -1")
    public abstract List<BirdSighting> getAllWithLocations();

    @Insert
    abstract long _insert(BirdSighting sighting);

    @Delete
    abstract void _delete(BirdSighting sighting);

    @Query("SELECT * FROM birdsighting WHERE name LIKE :name AND family LIKE :family")
    abstract List<BirdSighting> _findSimilar(String name, String family);

    @Query("SELECT * FROM birdsighting WHERE name LIKE :name AND family LIKE :family AND lat > -1 AND lon > -1")
    abstract List<BirdSighting> _findSimilarWithLocations(String name, String family);

    @Query("DELETE FROM birdsighting WHERE time < :beforeTime")
    public abstract void deleteBefore(long beforeTime);

    @Query("SELECT * FROM birdsighting WHERE sightingId = :sightingId LIMIT 1")
    public abstract BirdSighting getSighting(long sightingId);

    @RawQuery
    public abstract List<BirdSighting> filter(SupportSQLiteQuery query);

    public List<BirdSighting> findSimilar(Bird bird) {
        return _findSimilar(bird.getName(), bird.getFamily());
    }

    public List<BirdSighting> findSimilarWithLocations(Bird bird) {
        return _findSimilarWithLocations(bird.getName(), bird.getFamily());
    }

    public void insert(BirdSighting sighting) {
        sighting.sightingId = _insert(sighting);
        for (Listener listener : listeners) {
            listener.onUpdate(Listener.ADDED, sighting);
        }
    }

    public void delete(BirdSighting sighting) {
        _delete(sighting);
        for (Listener listener : listeners) {
            listener.onUpdate(Listener.DELETED, sighting);
        }
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public interface Listener {
        int ADDED = 0;
        int DELETED = 1;

        void onUpdate(int state, BirdSighting sighting);
    }
}
