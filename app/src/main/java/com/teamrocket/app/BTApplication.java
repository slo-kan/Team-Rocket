package com.teamrocket.app;

import android.app.Application;

import androidx.room.Room;

import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.data.db.CategoryDao;
import com.teamrocket.app.data.db.Database;

public class BTApplication extends Application {

    private static final String DB_NAME = "bird_tracking.db";

    private Database database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(this, Database.class, DB_NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
    }

    public BirdSightingDao getBirdSightingDao() {
        return database.birdSightingDao();
    }

    public CategoryDao getCategoryDao() {
        return database.categoryDao();
    }

}
