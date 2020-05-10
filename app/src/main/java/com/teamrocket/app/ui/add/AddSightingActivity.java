package com.teamrocket.app.ui.add;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.squareup.picasso.Picasso;
import com.teamrocket.app.BTApplication;
import com.teamrocket.app.R;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.model.Bird;
import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.util.TextChangedListener;
import com.teamrocket.app.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_PICTURES;
import static android.widget.Toast.LENGTH_SHORT;

public class AddSightingActivity extends AppCompatActivity {

    private static final int RC_PHOTO = 122;
    private static final int RC_LOCATION = 333;

    private BirdSightingDao dao;
    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;

    private String imagePath;

    private EditText editName;
    private AutoCompleteTextView editFamily;
    private EditText editLocation;
    private EditText editDateTime;

    private ImageButton btnMoreInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        setSupportActionBar(findViewById(R.id.toolbarAddSighting));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        dao = ((BTApplication) getApplication()).getBirdSightingDao();
        dao.addListener(sighting -> finish());

        locationProvider = LocationServices.getFusedLocationProviderClient(this);

        editName = findViewById(R.id.editNameAddSighting);
        editFamily = findViewById(R.id.editFamilyAddSighting);
        editLocation = findViewById(R.id.editLocationAddSighting);
        editDateTime = findViewById(R.id.editDateTimeAddSighting);

        if (!Utils.isLocationPermissionGranted(this)) {
            Utils.requestLocationPermission(this, RC_LOCATION);
        }

        ImageButton btnAddImage = findViewById(R.id.btnAddImageAddSighting);
        btnAddImage.setOnClickListener(v -> launchImageCaptureIntent());

        Button btnSave = findViewById(R.id.btnSaveAddSighting);
        btnSave.setOnClickListener(v -> addBird());

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
        if (requestCode != RC_PHOTO) {
            return;
        }

        if (resultCode != RESULT_OK) {
            this.imagePath = null;
            return;
        }

        Picasso.get().load(new File(imagePath)).fit().centerCrop()
                .into((ImageView) findViewById(R.id.imageAddSighting));
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
                    String locationMask = "%.05f, %.05f";
                    editLocation.setText(String.format(Locale.getDefault(), locationMask,
                            last.getLatitude(), last.getLongitude()));
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

    private boolean isFormValid() {
        return imagePath != null
                && !editLocation.getText().toString().isEmpty()
                && !editDateTime.getText().toString().isEmpty()
                && !editName.getText().toString().isEmpty()
                && !editFamily.getText().toString().isEmpty();
    }

    private void addBird() {
        if (!isFormValid()) {
            Toast.makeText(this, "Please fill all the details", LENGTH_SHORT).show();
            return;
        }

        Bird bird = new Bird();
        bird.setName(editName.getText().toString());
        bird.setFamily(editFamily.getText().toString());
        bird.setImagePath(imagePath);
        bird.setSize(Bird.SIZE_SMALL);
        bird.setColor("blue");

        String[] locationParts = editLocation.getText().toString().split(", ");
        double lat = Double.parseDouble(locationParts[0]);
        double lon = Double.parseDouble(locationParts[1]);
        BirdSighting.Location location = new BirdSighting.Location(lat, lon);

        String dateString = editDateTime.getText().toString();
        long time = Utils.parseDate(dateString).getTime();

        BirdSighting sighting = new BirdSighting(bird, location, time);

        dao.insert(sighting);
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

        this.imagePath = photoFile.getPath();
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
