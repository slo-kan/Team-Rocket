package com.teamrocket.app.ui.add;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.teamrocket.app.R;

import java.io.File;
import java.io.IOException;

import static android.os.Environment.DIRECTORY_PICTURES;
import static android.widget.Toast.LENGTH_SHORT;

public class AddSightingActivity extends AppCompatActivity {

    private static final int RC_PHOTO = 122;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        findViewById(R.id.btnAddImageAddSighting).setOnClickListener(v -> launchImageCaptureIntent());
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_PHOTO || resultCode != RESULT_OK) {
            return;
        }
    }
}
