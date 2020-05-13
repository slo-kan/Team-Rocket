package com.teamrocket.app.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.teamrocket.app.R;
import com.teamrocket.app.ui.home.HomeFragment;
import com.teamrocket.app.ui.map.MapFragment;
import com.teamrocket.app.ui.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment;
    private MapFragment mapFragment;
    private SearchFragment searchFragment;

    private Fragment currentFragment;

    private BottomNavigationView bottomNavBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeFragment = new HomeFragment();
        mapFragment = new MapFragment();
        searchFragment = new SearchFragment();

        bottomNavBar = findViewById(R.id.bottomNavBar);
        bottomNavBar.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.main_nav_home) showFragment(homeFragment);
            else if (itemId == R.id.main_nav_map) showFragment(mapFragment);
            else showFragment(searchFragment);

            return true;
        });

        showFragment(homeFragment);
    }

    private void showFragment(Fragment fragment) {
        if (currentFragment == fragment) return;

        boolean containsFragment = getSupportFragmentManager().getFragments().contains(fragment);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (currentFragment == null)
            ft.add(R.id.mainContent, fragment, fragment.getClass().getName());
        else if (!containsFragment)
            ft.hide(currentFragment).add(R.id.mainContent, fragment, fragment.getClass().getName());
        else
            ft.hide(currentFragment).show(fragment);

        ft.commit();
        currentFragment = fragment;
    }

    public MapFragment getMapFragment() {
        return this.mapFragment;
    }

    public void setBottomNavSelection(int id) {
        this.bottomNavBar.setSelectedItemId(id);
    }
}
