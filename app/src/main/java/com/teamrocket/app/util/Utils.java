package com.teamrocket.app.util;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.LOCATION_SERVICE;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class Utils {

    private static SimpleDateFormat simpleDateFormat;

    static {
        simpleDateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager conManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = conManager.getActiveNetworkInfo();

        return info != null && info.isConnected();
    }

    public static LatLng getRandomLocation(LatLng location, int radiusMtrs) {
        Random random = new Random();
        double u = random.nextDouble();
        double v = random.nextDouble();

        double radiusDeg = radiusMtrs / 113300.0;
        double w = radiusDeg * Math.sqrt(u);
        double t = 2 * Math.PI * v;
        double x = w * Math.cos(t);
        double y = w * Math.sin(t);

        x /= Math.cos(location.latitude);

        return new LatLng(y + location.latitude, x + location.longitude);
    }

    /**
     * Returns the bounds for the distance from a give location within a given distance.
     *
     * @param location The location from which the bounds are to be measured.
     * @param distKm   The distance until which in all directions the bounds must be measured.
     * @return a double array of size 2 containing the latitude and longitude deviations.
     */
    public static double[] getCoordinateDeviations(LatLng location, int distKm) {
        // 1Lat = 111,111 mtrs
        // 1000mtr = 1000/111,111 deg = 0.009000deg Lat

        double kmLat = 1 / 111.111;
        double kmLon = 1 / (111.111 * Math.cos(location.latitude * Math.PI / 180));

        return new double[]{kmLat * distKm, kmLon * distKm};
    }

    public static int toDp(float px, Context context) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    public static int toPx(float dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static boolean isLocationPermissionGranted(Context context) {
        return PermissionChecker.checkSelfPermission(context, ACCESS_FINE_LOCATION)
                == PERMISSION_GRANTED;
    }

    public static boolean isLocationPermissionGranted(int[] grantResults) {
        return grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{ACCESS_FINE_LOCATION},
                requestCode);
    }

    public static void requestLocationPermission(Fragment fragment, int requestCode) {
        fragment.requestPermissions(new String[]{ACCESS_FINE_LOCATION}, requestCode);
    }

    public static boolean isGpsEnabled(Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        return manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static boolean isPermissionGranted(Context context, String permission) {
        return PermissionChecker.checkSelfPermission(context, permission) == PERMISSION_GRANTED;
    }

    public static void requestPermission(Activity activity, String permission, int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
    }

    public static void showInfoDialog(Context context, int titleId, int messageId) {
        new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static String formatDate(long timestamp) {
        Date date = new Date(timestamp);
        return simpleDateFormat.format(date);
    }

    public static Date parseDate(String date) {
        try {
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return new Date();
    }
}
