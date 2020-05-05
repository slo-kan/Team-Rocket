package com.teamrocket.app.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.teamrocket.app.R;
import com.teamrocket.app.ui.home.HomeFragment;
import com.teamrocket.app.ui.map.MapFragment;
import com.teamrocket.app.ui.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment;
    private MapFragment mapFragment;
    private SearchFragment searchFragment;

    private String currentFragmentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        homeFragment = new HomeFragment();
        mapFragment = new MapFragment();
        searchFragment = new SearchFragment();

        BottomNavigationView bottomNavBar = findViewById(R.id.bottomNavBar);
        bottomNavBar.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.main_nav_home) showFragment(HomeFragment.TAG);
            else if (itemId == R.id.main_nav_map) showFragment(MapFragment.TAG);
            else showFragment(SearchFragment.TAG);

            return true;
        });

        showFragment(HomeFragment.TAG);
    }

    private void showFragment(String tag) {
        if (tag.equals(currentFragmentTag)) {
            return;
        }

        Fragment oldFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
        if (oldFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .hide(oldFragment)
                    .commit();
        }

        currentFragmentTag = tag;
        Fragment newFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);

        if (newFragment == null) {
            newFragment = currentFragmentTag.equals(HomeFragment.TAG) ? homeFragment
                    : currentFragmentTag.equals(MapFragment.TAG) ? mapFragment
                    : searchFragment;

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.mainContent, newFragment, currentFragmentTag)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .show(newFragment)
                    .commit();
        }
    }
}
