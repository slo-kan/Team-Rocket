package com.teamrocket.app.ui.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.teamrocket.app.R;
import com.teamrocket.app.util.Utils;

import static android.graphics.Bitmap.Config.ARGB_8888;

public class ImageMarker implements Target {

    private Context context;
    private Marker marker;

    ImageMarker(Context context, Marker marker) {
        this.context = context;
        this.marker = marker;
    }

    @Override
    public void onBitmapLoaded(Bitmap image, Picasso.LoadedFrom from) {
        int borderColour = ContextCompat.getColor(context, R.color.gray_9);

        float bitmapPadding = Utils.toPx(16, context);

        int markerWidth = (int) (image.getWidth() + bitmapPadding);
        int markerHeight = (int) (image.getHeight() + bitmapPadding);

        Bitmap markerBitmap = Bitmap.createBitmap(markerWidth, markerHeight, ARGB_8888);
        Canvas canvas = new Canvas(markerBitmap);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
        paint.setColor(borderColour);

        float borderLeft = bitmapPadding / 2f;
        float borderTop = bitmapPadding / 2f;
        float borderRight = markerWidth - bitmapPadding / 2f;
        float borderBottom = markerHeight - bitmapPadding / 2f;

        float trianglePadding = bitmapPadding / 3;
        float triangleMargin = bitmapPadding / 5;

        Path triangle = new Path();
        triangle.moveTo((borderLeft + borderRight) / 2 + trianglePadding, borderBottom + triangleMargin);
        triangle.lineTo(canvas.getWidth() / 2f, canvas.getHeight());
        triangle.lineTo((borderLeft + borderRight) / 2 - trianglePadding, borderBottom + triangleMargin);
        triangle.close();

        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(triangle, paint);

        canvas.drawBitmap(image, bitmapPadding / 2, bitmapPadding / 2, null);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(borderLeft, borderTop, borderRight, borderBottom, paint);

        this.marker.setIcon(BitmapDescriptorFactory.fromBitmap(markerBitmap));
        this.marker.setAnchor(0.5f, 1);
    }

    @Override
    public void onBitmapFailed(Exception e, Drawable errorDrawable) {

    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }
}
