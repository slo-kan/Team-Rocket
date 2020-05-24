package com.teamrocket.app.ui.add;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.picasso.Picasso;
import com.teamrocket.app.BTApplication;
import com.teamrocket.app.R;
import com.teamrocket.app.data.db.BirdSightingDao;
import com.teamrocket.app.data.db.CategoryDao;
import com.teamrocket.app.model.Bird;
import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.model.Category;
import com.teamrocket.app.util.TextChangedListener;
import com.teamrocket.app.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.os.Environment.DIRECTORY_PICTURES;
import static android.widget.Toast.LENGTH_SHORT;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

public class AddSightingActivity extends AppCompatActivity {

    private static final int RC_PHOTO = 122;
    private static final int RC_LOCATION = 333;
    private static final String URL_WIKIPEDIA = "https://wikipedia.org/wiki/%s";

    private BirdSightingDao dao;
    private CategoryDao categoryDao;

    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;

    private View locationPickerView;
    private MapView mapView;

    private GoogleMap map = null;
    private Location lastLocation = null;

    private String imagePath;
    private String selectedCategory;

    private EditText editName;
    private EditText editFamily;
    private EditText editLocation;
    private EditText editDateTime;

    private ImageButton btnMoreInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        setSupportActionBar(findViewById(R.id.toolbarAddSighting));
        MapsInitializer.initialize(AddSightingActivity.this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        dao = ((BTApplication) getApplication()).getBirdSightingDao();
        dao.addListener(sighting -> finish());

        categoryDao = ((BTApplication) getApplication()).getCategoryDao();
        categoryDao.populateDefaults(getApplicationContext());

        locationProvider = LocationServices.getFusedLocationProviderClient(this);

        editName = findViewById(R.id.editNameAddSighting);
        editFamily = findViewById(R.id.editFamilyAddSighting);
        editLocation = findViewById(R.id.editLocationAddSighting);
        editDateTime = findViewById(R.id.editDateTimeAddSighting);
        btnMoreInfo = findViewById(R.id.btnMoreInfoAddSighting);

        if (!Utils.isLocationPermissionGranted(this)) {
            Utils.requestLocationPermission(this, RC_LOCATION);
        }

        editFamily.setOnClickListener(v -> showSelectCategoryDialog());

        ImageButton btnAddCategory = findViewById(R.id.btnAddCategoryAddSighting);
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        //Create the view which is shown when the location picker is opened.
        //We create this here and load the map to avoid unnecessary object creation later on.
        locationPickerView = View.inflate(this, R.layout.add_location_picker, null);
        mapView = locationPickerView.findViewById(R.id.mapLocationPicker);
        mapView.onCreate(null);
        mapView.onResume();
        mapView.getMapAsync(gMap -> map = gMap);

        editLocation.setOnClickListener(v -> showLocationPickerDialog());
        editDateTime.setOnClickListener(v -> showDateTimePickerDialog());

        ImageButton btnAddImage = findViewById(R.id.btnAddImageAddSighting);
        btnAddImage.setOnClickListener(v -> launchImageCaptureIntent());

        Button btnSave = findViewById(R.id.btnSaveAddSighting);
        btnSave.setOnClickListener(v -> addBird());

        editDateTime.setText(Utils.formatDate(System.currentTimeMillis()));

        btnMoreInfo.setImageAlpha(0x3F);
        btnMoreInfo.setEnabled(false);
        btnMoreInfo.setOnClickListener(v -> {
            String url = String.format(URL_WIKIPEDIA, editName.getText().toString());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(url));
            startActivity(browserIntent);
        });

        editName.addTextChangedListener(new TextChangedListener(count -> {
            Log.d("AddSighting", "onCreate: Count is " + count);
            btnMoreInfo.setImageAlpha(count > 3 ? 0xFF : 0x3F);
            btnMoreInfo.setEnabled(count > 3);
        }));

        Button randomiseButton = findViewById(R.id.btnRandomise);
        randomiseButton.setVisibility(View.GONE);
        randomiseButton.setOnClickListener(v -> {
            String[] locationParts = editLocation.getText().toString().split(", ");
            if (locationParts.length != 2) return;

            double lat = Double.parseDouble(locationParts[0]);
            double lon = Double.parseDouble(locationParts[1]);

            LatLng random = Utils.getRandomLocation(new LatLng(lat, lon), 1000);
            String locationMask = "%.05f, %.05f";
            editLocation.setText(String.format(Locale.getDefault(), locationMask,
                    random.latitude, random.longitude));
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
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
        //This will be called in onResume so we should only update the location if the
        //location field is empty.
        if (!editLocation.getText().toString().isEmpty()) return;

        LocationRequest request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(120 * 1000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    lastLocation = locationResult.getLastLocation();
                    String locationMask = "%.05f, %.05f";
                    editLocation.setText(String.format(Locale.getDefault(), locationMask,
                            lastLocation.getLatitude(), lastLocation.getLongitude()));
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

    private void showLocationPickerDialog() {
        //A previous dialog may have already shown the map in which case that will remain as the
        //parent. We need to manually remove the view's parent to show it in another dialog.
        ViewGroup parent = (ViewGroup) locationPickerView.getParent();
        if (parent != null) {
            parent.removeView(locationPickerView);
        }

        //Find where to zoom in the map.
        //If there's a location in the edit field, then zoom to it.
        //Otherwise if location permission is granted and GPS is ON, zoom to the current location.
        //Otherwise don't zoom.
        LatLng zoomToLoc = null;
        String textLocation = editLocation.getText().toString();

        if (!textLocation.isEmpty()) {
            String[] splits = textLocation.split(",");
            double lat = Double.parseDouble(splits[0].trim());
            double lon = Double.parseDouble(splits[1].trim());
            zoomToLoc = new LatLng(lat, lon);
        } else if (Utils.isLocationPermissionGranted(this) && lastLocation != null) {
            zoomToLoc = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        }

        new AlertDialog.Builder(AddSightingActivity.this)
                .setTitle(R.string.add_sighting_select_location)
                .setView(locationPickerView)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (map == null) return;
                    //Projection is used to convert location on the screen to location in map.
                    //In this case, we need to find the coordinates which correspond to the centre
                    //of the map.
                    Projection projection = map.getProjection();
                    int x = mapView.getWidth() / 2;
                    int y = mapView.getHeight() / 2;
                    LatLng centrePosition = projection.fromScreenLocation(new Point(x, y));
                    String locationMask = "%.05f, %.05f";
                    editLocation.setText(String.format(Locale.getDefault(), locationMask,
                            centrePosition.latitude, centrePosition.longitude));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        if (map != null && zoomToLoc != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(zoomToLoc, 15));
        }
    }

    private void showDateTimePickerDialog() {
        String dateString = editDateTime.getText().toString();
        Date date = Utils.parseDate(dateString);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        new DatePickerDialog(this, (dView, year, month, day) -> {
            new TimePickerDialog(this, (tView, hour, minute) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day, hour, minute);
                String dateTime = Utils.formatDate(cal.getTime().getTime());
                editDateTime.setText(dateTime);
            }, calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), true).show();
        }, calendar.get(YEAR), calendar.get(MONTH), calendar.get(DAY_OF_MONTH)).show();
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

    private void showSelectCategoryDialog() {
        String[] categories = categoryDao.getAll()
                .stream()
                .map(Category::getName)
                .toArray(size -> new String[categoryDao.getNumCategories()]);

        new AlertDialog.Builder(AddSightingActivity.this)
                .setTitle(R.string.add_sighting_title_select_category)
                .setSingleChoiceItems(categories, -1, (d, w) -> {
                    this.selectedCategory = categories[w];
                    editFamily.setText(this.selectedCategory);
                })
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (d, w) -> editFamily.setText(""))
                .show();
    }

    private void showAddCategoryDialog() {
        View dialogView = View.inflate(this, R.layout.add_dialog_add_category, null);
        EditText editCategory = dialogView.findViewById(R.id.editCategoryAddCategory);
        editCategory.requestFocus();

        new AlertDialog.Builder(AddSightingActivity.this)
                .setTitle(R.string.add_sighting_title_add_category)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String categoryName = editCategory.getText().toString();
                    boolean isValid = !categoryName.isEmpty()
                            && categoryDao.getNumCategories(categoryName) == 0;

                    if (!isValid) {
                        String message = categoryName.isEmpty()
                                ? getString(R.string.add_sighting_cat_name_empty)
                                : getString(R.string.add_sighting_cat_name_exists, categoryName);

                        Toast.makeText(this, message, LENGTH_SHORT).show();
                        return;
                    }

                    Category category = new Category(categoryName);
                    categoryDao.insert(category);
                    editFamily.setText(categoryName);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

    }
}
