package com.teamrocket.app.data.db;

import androidx.room.RoomDatabase;

import com.teamrocket.app.model.BirdSighting;

@androidx.room.Database(entities = {BirdSighting.class}, version = 1)
public abstract class Database extends RoomDatabase {
    public abstract BirdSightingDao birdSightingDao();
}
