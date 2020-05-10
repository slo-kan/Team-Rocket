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

public class AddSightingActivity extends AppCompatActivity {

    private static final int RC_PHOTO = 122;

    private void takePhoto() {
        Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (photoIntent.resolveActivity(this.getPackageManager()) == null) {
            return;
        }

        File photoFile = getPhotoFile();
        if (photoFile == null) {
            return;
        }

        Uri photoUri = FileProvider.getUriForFile(this, "com.teamrocket.app.fileprovider", photoFile);
        photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(photoIntent, RC_PHOTO);

    }

    private File getPhotoFile() {
        String fileName = "IMG_" + System.currentTimeMillis();
        String extension = ".jpg";

        File tempFile = null;
        File storageDir = this.getExternalFilesDir(DIRECTORY_PICTURES);
        try {
            tempFile = File.createTempFile(fileName, extension, storageDir);
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

        Toast.makeText(this, "Photo taken successesfully", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        findViewById(R.id.btnAddImageAddSighting).setOnClickListener(v -> takePhoto());
    }
}
