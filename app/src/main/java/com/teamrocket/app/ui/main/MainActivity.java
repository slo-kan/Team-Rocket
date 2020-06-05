package com.teamrocket.app.ui.main;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.teamrocket.app.R;
import com.teamrocket.app.ui.home.HomeFragment;
import com.teamrocket.app.ui.map.MapFragment;
import com.teamrocket.app.ui.search.SearchFragment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment;
    private MapFragment mapFragment;
    private SearchFragment searchFragment;

    private Fragment currentFragment;

    private BottomNavigationView bottomNavBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLanguage();
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

    @Override
    public void onBackPressed() {
        if (currentFragment != homeFragment) {
            setBottomNavSelection(R.id.main_nav_home);
            return;
        }

        super.onBackPressed();
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

        //We need to reset the filters for map fragment somehow, this is one approach to do that.
        //In this case, whatever wants to set filters should do that after this call.
        if (fragment == mapFragment && mapFragment.shouldResetFilters) {
            mapFragment.filterBird(null);
        }

        ft.commit();
        currentFragment = fragment;
    }

    public MapFragment getMapFragment() {
        return this.mapFragment;
    }

    public void setBottomNavSelection(int id) {
        this.bottomNavBar.setSelectedItemId(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_change_language) {
            showLanguageChangeDialog();
        } else if (item.getItemId() == R.id.main_menu_filter) {
            homeFragment.showFilterDialog();
        }
        return true;
    }

    private void showLanguageChangeDialog() {
        String[] languages = {"Default", "English", "German", "Spanish", "Hindi"};
        List<String> langValues = Arrays.asList("", "en-GB", "de", "es", "hi");

        String currentLang = PreferenceManager.getDefaultSharedPreferences(this).getString("lang", "");

        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.home_title_change_language)
                .setSingleChoiceItems(languages, langValues.indexOf(currentLang), null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    int pos = ((AlertDialog) d).getListView().getCheckedItemPosition();
                    String lang = langValues.get(pos);
                    if (currentLang.equals(lang)) return;

                    PreferenceManager.getDefaultSharedPreferences(this)
                            .edit()
                            .putString("lang", langValues.get(pos))
                            .apply();
                    restart();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();


    }

    private void setLanguage() {
        String language = PreferenceManager.getDefaultSharedPreferences(this).getString("lang", "");
        Locale locale = language.isEmpty() ? Locale.getDefault() : new Locale(language);

        Resources res = getBaseContext().getResources();
        Configuration config = new Configuration(res.getConfiguration());
        Locale.setDefault(locale);
        config.setLocale(locale);

        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public void restart() {
        Intent intent = getIntent();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent);
    }
}
