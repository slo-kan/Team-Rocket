package com.teamrocket.app.ui.settings;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.teamrocket.app.R;

public class PreferenceFragment extends PreferenceFragmentCompat {

    //Initialisation of this fragment might take some time and until then findPreference()
    //cannot be called because it will return null.
    //This listener will notify SettingsFragment when initialisation is done.
    public OnCreatedListener listener;


    public PreferenceFragment() {
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditTextPreference autoDeletePref = findPreference("autoDeleteSightings");
        autoDeletePref.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setHint(R.string.settings_hint_auto_delete);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        });

        this.listener.onCreated();
    }

    public interface OnCreatedListener {
        void onCreated();
    }
}
