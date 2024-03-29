package com.teamrocket.app.ui.add;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

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
import com.teamrocket.app.data.network.IWikiApi;
import com.teamrocket.app.model.Bird;
import com.teamrocket.app.model.BirdSighting;
import com.teamrocket.app.model.Category;
import com.teamrocket.app.model.WikiResponse;
import com.teamrocket.app.util.TextChangedListener;
import com.teamrocket.app.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.widget.Toast.LENGTH_SHORT;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

public class AddSightingActivity extends AppCompatActivity {

    private static final int RC_CAPTURE_PHOTO = 122;
    private static final int RC_PICK_PHOTO = 123;
    private static final int RC_LOCATION = 333;
    private static final int RC_STORAGE = 334;

    private static final String URL_WIKIPEDIA = "https://wikipedia.org/wiki/%s";

    private BirdSightingDao dao;
    private BirdSightingDao.Listener listener;
    private CategoryDao categoryDao;

    private IWikiApi wikiApi;

    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;

    private View locationPickerView;
    private MapView mapView;

    private GoogleMap map = null;
    private Location lastLocation = null;

    private String imagePath;
    private String selectedCategory;

    private ImageView imageThumbnail;
    private EditText editName;
    private EditText editFamily;
    private EditText editLocation;
    private EditText editDateTime;
    private EditText editNotes;

    private TextView btnMoreInfo;

    private boolean shouldIncludeLocation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setLanguage();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        setSupportActionBar(findViewById(R.id.toolbarAddSighting));
        MapsInitializer.initialize(AddSightingActivity.this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        dao = ((BTApplication) getApplication()).getBirdSightingDao();
        listener = (state, sighting) -> {
            if (state == BirdSightingDao.Listener.ADDED) finish();
        };

        dao.addListener(listener);

        wikiApi = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(IWikiApi.BASE_URL)
                .build()
                .create(IWikiApi.class);

        categoryDao = ((BTApplication) getApplication()).getCategoryDao();
        categoryDao.populateDefaults(getBaseContext());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        shouldIncludeLocation = !prefs.getBoolean("hideSightingLocations", false);

        locationProvider = LocationServices.getFusedLocationProviderClient(this);

        imageThumbnail = findViewById(R.id.imageAddSighting);
        editName = findViewById(R.id.editNameAddSighting);
        editFamily = findViewById(R.id.editFamilyAddSighting);
        editLocation = findViewById(R.id.editLocationAddSighting);
        editDateTime = findViewById(R.id.editDateTimeAddSighting);
        editNotes = findViewById(R.id.editNotesAddSighting);
        btnMoreInfo = findViewById(R.id.btnMoreInfoAddSighting);

        if (shouldIncludeLocation && !Utils.isLocationPermissionGranted(this)) {
            Utils.requestLocationPermission(this, RC_LOCATION);
        }

        editLocation.setVisibility(shouldIncludeLocation ? View.VISIBLE : View.GONE);
        findViewById(R.id.textLocationAddSighting).setVisibility(shouldIncludeLocation ? View.VISIBLE : View.GONE);

        editFamily.setOnClickListener(v -> showSelectCategoryDialog());

        TextView btnAddCategory = findViewById(R.id.btnAddCategoryAddSighting);
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

        ImageButton btnPickImage = findViewById(R.id.btnPickImageAddSighting);
        btnPickImage.setOnClickListener(v -> {
            //TODO: Check if storage permission is even required for SAF
            if (!Utils.isPermissionGranted(this, READ_EXTERNAL_STORAGE)) {
                Utils.requestPermission(this, READ_EXTERNAL_STORAGE, RC_STORAGE);
                return;
            }

            launchImagePickIntent();
        });

        Button btnSave = findViewById(R.id.btnSaveAddSighting);
        btnSave.setOnClickListener(v -> addBird());

        editDateTime.setText(Utils.formatDate(System.currentTimeMillis()));

        btnMoreInfo.setEnabled(false);
        btnMoreInfo.setOnClickListener(v -> onMoreInfoClicked());

        editName.addTextChangedListener(new TextChangedListener(count -> {
            btnMoreInfo.setEnabled(count > 0);
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
    protected void onDestroy() {
        super.onDestroy();
        dao.removeListener(listener);
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

        if (resultCode != RESULT_OK) {
            this.imagePath = null;
            return;
        }

        //If image is captured from the camera, the image path is already set
        if (requestCode == RC_PICK_PHOTO && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                this.imagePath = imageUri.toString();
            }
        }

        String path = imagePath.startsWith("content://") ? imagePath : "file://" + imagePath;
        Picasso.get().load(path).fit().centerCrop().into(imageThumbnail);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (grantResults[0] != PERMISSION_GRANTED) {
            return;
        }

        if (requestCode == RC_LOCATION) startLocationUpdates();
        else if (requestCode == RC_STORAGE) launchImagePickIntent();
    }

    @SuppressLint("MissingPermission")
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

    private void onMoreInfoClicked() {
        if (!Utils.isOnline(AddSightingActivity.this)) {
            Toast.makeText(getBaseContext(), R.string.add_sighting_msg_offline, LENGTH_SHORT).show();
            return;
        }

        Utils.hideKeyboard(AddSightingActivity.this);

        View infoView = View.inflate(this, R.layout.dialog_info, null);
        infoView.findViewById(R.id.progress_dialog_info).setVisibility(View.VISIBLE);
        infoView.findViewById(R.id.content_dialog_info).setVisibility(View.GONE);

        infoView.findViewById(R.id.btn_wikipedia_dialog_info).setOnClickListener(v -> {
            String url = String.format(URL_WIKIPEDIA, editName.getText().toString());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(url));
            startActivity(browserIntent);
        });

        AlertDialog infoDialog = new AlertDialog.Builder(AddSightingActivity.this)
                .setTitle(editName.getText().toString())
                .setView(infoView)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        infoDialog.show();

        wikiApi.getBirdInformation(editName.getText().toString()).enqueue(new Callback<WikiResponse>() {
            @Override
            public void onResponse(Call<WikiResponse> call, Response<WikiResponse> response) {
                WikiResponse info = response.body();
                if (info == null) {
                    Toast.makeText(getBaseContext(), R.string.add_sighting_error_wiki, LENGTH_SHORT).show();
                    if (infoDialog.isShowing()) infoDialog.dismiss();
                    return;
                }

                if (infoDialog.isShowing()) {
                    ((TextView) infoView.findViewById(R.id.text_dialog_info)).setText(info.getDescription());
                    infoView.findViewById(R.id.progress_dialog_info).setVisibility(View.GONE);
                    infoView.findViewById(R.id.content_dialog_info).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<WikiResponse> call, Throwable t) {
                Toast.makeText(getBaseContext(), R.string.add_sighting_error_wiki, LENGTH_SHORT).show();
                if (infoDialog.isShowing()) infoDialog.dismiss();
            }
        });
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

        DatePickerDialog dateDialog = new DatePickerDialog(this, (dView, year, month, day) -> {
            new TimePickerDialog(this, (tView, hour, minute) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day, hour, minute);
                String dateTime = Utils.formatDate(cal.getTime().getTime());
                editDateTime.setText(dateTime);
            }, calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), true).show();
        }, calendar.get(YEAR), calendar.get(MONTH), calendar.get(DAY_OF_MONTH));

        dateDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dateDialog.show();
    }

    private boolean isFormValid() {
        boolean isValid = imagePath != null
                && !editDateTime.getText().toString().isEmpty()
                && !editName.getText().toString().isEmpty()
                && !editFamily.getText().toString().isEmpty();

        if (shouldIncludeLocation) {
            isValid = isValid && !editLocation.getText().toString().isEmpty();
        }

        return isValid;
    }

    private void addBird() {
        if (!isFormValid()) {
            Toast.makeText(this, R.string.add_sighting_msg_fill_details, LENGTH_SHORT).show();
            return;
        }

        List<String> localisedNames = Arrays.asList(getBaseContext().getResources().getStringArray(R.array.categories));

        Configuration conf = getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(new Locale("en"));
        Context localizedContext = createConfigurationContext(conf);
        Resources res = localizedContext.getResources();

        List<String> names = Arrays.asList(res.getStringArray(R.array.categories));

        String localisedCategory = editFamily.getText().toString();
        String category = localisedNames.contains(localisedCategory)
                ? names.get(localisedNames.indexOf(localisedCategory))
                : localisedCategory;

        Bird bird = new Bird();
        bird.setName(editName.getText().toString());
        bird.setFamily(category);
        bird.setImagePath(imagePath);
        bird.setSize(Bird.SIZE_SMALL);
        bird.setColor("blue");

        String locString = shouldIncludeLocation ? editLocation.getText().toString() : "-1000, -1000";
        String[] locationParts = locString.split(", ");
        double lat = Double.parseDouble(locationParts[0]);
        double lon = Double.parseDouble(locationParts[1]);
        BirdSighting.Location location = new BirdSighting.Location(lat, lon);

        String dateString = editDateTime.getText().toString();
        String notes = editNotes.getText().toString();
        long time = Utils.parseDate(dateString).getTime();

        BirdSighting sighting = new BirdSighting(bird, location, time, notes);

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
        startActivityForResult(photoIntent, RC_CAPTURE_PHOTO);

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

    private void launchImagePickIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        if (intent.resolveActivity(getPackageManager()) == null) {
            //There are no applications that can handle this intent
            return;
        }

        startActivityForResult(intent, RC_PICK_PHOTO);
    }

    private void showSelectCategoryDialog() {
        String[] categories = categoryDao.getAll(getBaseContext()).toArray(new String[categoryDao.getNumCategories()]);

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

    private void setLanguage() {
        String language = PreferenceManager.getDefaultSharedPreferences(this).getString("lang", "");
        Locale locale = language.isEmpty() ? Locale.getDefault() : new Locale(language);

        Resources res = getResources();
        Configuration config = new Configuration(res.getConfiguration());
        Locale.setDefault(locale);
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

}
