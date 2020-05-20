package com.teamrocket.app.model;

import android.content.Context;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.teamrocket.app.R;

@Entity
public class Category {

    @PrimaryKey(autoGenerate = true)
    public long primaryKey;

    private int id;
    private String name;
    private boolean isDefault;

    public Category() {

    }

    public Category(String name) {
        this(name, false);
    }

    public Category(String name, boolean isDefault) {
        this(name.hashCode(), name, isDefault);
    }

    public Category(int id, String name, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.isDefault = isDefault;
    }

    public static Category[] getDefaultCategories(Context context) {
        String[] names = context.getResources().getStringArray(R.array.categories);
        Category[] categories = new Category[names.length];
        for (int i = 0; i < names.length; i++) {
            categories[i] = new Category(i, names[i], true);
        }
        return categories;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
