package com.example.pdfscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;

public class ViewActivity extends AppCompatActivity {

    private Uri uri;
    private String mime;
    private ImageView image_View;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view);


        uri = getIntent().getParcelableExtra("URI");
        mime = getIntent().getStringExtra("mimeType");
        image_View = findViewById(R.id.image_view);

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            image_View.setImageBitmap(bitmap);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}