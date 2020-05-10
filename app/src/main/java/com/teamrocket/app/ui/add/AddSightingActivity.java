package com.teamrocket.app.ui.add;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;
import com.teamrocket.app.R;
import com.teamrocket.app.util.Utils;

import java.io.File;
import java.io.IOException;

import static android.os.Environment.DIRECTORY_PICTURES;
import static android.widget.Toast.LENGTH_SHORT;

public class AddSightingActivity extends AppCompatActivity {

    private static final int RC_PHOTO = 122;
    private static final int RC_LOCATION = 333;

    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;

    private TextInputEditText editLocation;
    private TextInputEditText editDateTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        locationProvider = LocationServices.getFusedLocationProviderClient(this);

        editLocation = findViewById(R.id.editLocationAddSighting);
        editDateTime = findViewById(R.id.editDateTimeAddSighting);

        if (!Utils.isLocationPermissionGranted(this)) {
            Utils.requestLocationPermission(this, RC_LOCATION);
        }

        ImageButton btnAddImage = findViewById(R.id.btnAddImageAddSighting);
        btnAddImage.setOnClickListener(v -> launchImageCaptureIntent());

        editDateTime.setText(Utils.formatDate(System.currentTimeMillis()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Utils.isLocationPermissionGranted(this)) {
            startLocationUpdates();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_PHOTO || resultCode != RESULT_OK) {
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == RC_LOCATION && Utils.isLocationPermissionGranted(grantResults)) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(120 * 1000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location last = locationResult.getLastLocation();
                    editLocation.setText(last.getLatitude() + ", " + last.getLongitude());
                }
            }
        };

        locationProvider.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            locationProvider.removeLocationUpdates(locationCallback);
        }
    }

    private void launchImageCaptureIntent() {
        Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (photoIntent.resolveActivity(this.getPackageManager()) == null) {
            Toast.makeText(this, R.string.add_sighting_no_camera, LENGTH_SHORT).show();
            return;
        }

        File photoFile = getPhotoFile();
        if (photoFile == null) {
            Toast.makeText(this, R.string.add_sighting_image_file_error, LENGTH_SHORT).show();
            return;
        }

        Uri photoUri = FileProvider.getUriForFile(this, "com.teamrocket.app.fileprovider", photoFile);
        photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(photoIntent, RC_PHOTO);

    }

    private File getPhotoFile() {
        String fileName = "IMG_" + System.currentTimeMillis();

        File tempFile = null;
        File storageDir = getExternalFilesDir(DIRECTORY_PICTURES);

        try {
            tempFile = File.createTempFile(fileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tempFile;
    }

}
