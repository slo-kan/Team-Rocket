package com.teamrocket.app.data.db;

import androidx.room.RoomDatabase;

import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.model.Category;

@androidx.room.Database(entities = {BirdSighting.class, Category.class}, version = 2)
public abstract class Database extends RoomDatabase {
    public abstract BirdSightingDao birdSightingDao();

    public abstract CategoryDao categoryDao();
}
