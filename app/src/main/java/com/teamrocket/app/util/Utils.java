package com.teamrocket.app.util;

import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

public class Utils {

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
}
