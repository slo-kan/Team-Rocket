package com.teamrocket.app.data.db;

import android.content.Context;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.teamrocket.app.model.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Dao
public abstract class CategoryDao {

    private List<Listener> listeners = new ArrayList<>();

    @Query("SELECT * FROM category")
    abstract List<Category> _getAll();

    public List<Category> getAll(Context context) {
        List<Category> defaults = Arrays.asList(Category.getDefaultCategories(context));

        return _getAll().stream()
                .peek(category -> {
                    if (category.isDefault())
                        category.setName(defaults.get(category.getId()).getName());
                })
                .collect(Collectors.toList());
    }

    @Query("SELECT COUNT(id) FROM category WHERE isDefault = 1")
    abstract int getNumDefault();

    @Query("SELECT COUNT(id) FROM category")
    public abstract int getNumCategories();

    @Query("SELECT COUNT(id) from category WHERE name LIKE :name")
    public abstract int getNumCategories(String name);

    @Insert
    abstract void _insert(Category... category);

    public void insert(Category category) {
        _insert(category);
        for (Listener listener : listeners) {
            listener.onCategoryAdded(category);
        }
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public void populateDefaults(Context context) {
        if (getNumDefault() == 0) {
            _insert(Category.getDefaultCategories(context));
        }
    }

    interface Listener {
        void onCategoryAdded(Category category);
    }
}
