package com.teamrocket.app.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.teamrocket.app.BTApplication;
import com.teamrocket.app.R;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.data.db.CategoryDao;

import java.io.FileOutputStream;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends Fragment {

    private static final int RC_CREATE_FILE = 3923;

    private PreferenceFragment preferenceFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferenceFragment = new PreferenceFragment(this::onPreferenceFragmentInitialised);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.settingsFragmentContainer, preferenceFragment)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CREATE_FILE && resultCode == RESULT_OK && data != null) {
            Uri resultUri = data.getData();
            exportData(resultUri);
        }
    }

    private void onPreferenceFragmentInitialised() {
        preferenceFragment.findPreference("export").setOnPreferenceClickListener(preference -> {
            String fileName = "export_" + System.currentTimeMillis();

            Intent createIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            createIntent.addCategory(Intent.CATEGORY_OPENABLE);
            createIntent.setType("text/plain");
            createIntent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(createIntent, RC_CREATE_FILE);
            return true;
        });
    }

    private void exportData(Uri uri) {
        BirdSightingDao sightingDao = ((BTApplication) getActivity().getApplication()).getBirdSightingDao();
        CategoryDao categoryDao = ((BTApplication) getActivity().getApplication()).getCategoryDao();
        Gson gson = new Gson();

        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("```sightings");
        dataBuilder.append(gson.toJson(sightingDao.getAll()));
        dataBuilder.append("```categories");
        dataBuilder.append(gson.toJson(categoryDao.getAll(requireContext())));

        try (ParcelFileDescriptor pfd = requireActivity().getContentResolver().openFileDescriptor(uri, "w");
             FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor())) {
            fos.write(dataBuilder.toString().getBytes());
            Toast.makeText(requireContext(), R.string.settings_msg_export_successful, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
