package com.teamrocket.app.ui.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.teamrocket.app.R;

public class PreferenceFragment extends PreferenceFragmentCompat {

    //Initialisation of this fragment might take some time and until then findPreference()
    //cannot be called because it will return null.
    //This listener will notify SettingsFragment when initialisation is done.
    private OnCreatedListener listener;

    public PreferenceFragment(OnCreatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.listener.onCreated();
    }

    public interface OnCreatedListener {
        void onCreated();
    }
}
