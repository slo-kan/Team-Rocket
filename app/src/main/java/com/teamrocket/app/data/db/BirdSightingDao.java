package com.teamrocket.app.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.teamrocket.app.model.BirdSighting;

import java.util.List;

@Dao
public interface BirdSightingDao {

    @Query("SELECT * FROM birdsighting")
    List<BirdSighting> getAll();

    @Insert
    void insert(BirdSighting... sightings);

}
