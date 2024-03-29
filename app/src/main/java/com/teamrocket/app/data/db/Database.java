package com.teamrocket.app.data.db;

import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.model.Category;

@androidx.room.Database(entities = {BirdSighting.class, Category.class}, version = 3)
public abstract class Database extends RoomDatabase {

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            String create = "CREATE TABLE category " +
                    "(primaryKey INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "id INTEGER NOT NULL, " +
                    "name TEXT, " +
                    "isDefault INTEGER NOT NULL);";
            database.execSQL(create);
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE birdsighting ADD COLUMN notes TEXT DEFAULT ''");
        }
    };

    public abstract BirdSightingDao birdSightingDao();

    public abstract CategoryDao categoryDao();
}
