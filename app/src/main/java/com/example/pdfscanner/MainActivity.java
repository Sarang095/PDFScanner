package com.example.pdfscanner;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1;
    private static final int UPLOAD_IMG_CODE = 2;
    private static final int UPLOAD_WORD_CODE = 3;
    private static final int IMAGE_CAPTURE_CODE = 4;
    private static final int UPLOAD_PDF_CODE =5;

    private int currentRequestCode;
    private String currentMimeType;
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button ImgToPdf = findViewById(R.id.image_to_pdf);
        Button WordToPdf = findViewById(R.id.word_to_pdf);
        Button capture = findViewById(R.id.capture);
        Button PDFtoWord =  findViewById(R.id.upload_pdf);
        Button test_ui = findViewById(R.id.test_ui);


        test_ui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                startActivity(intent);
            }
        });


        ImgToPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permissions(UPLOAD_IMG_CODE, "image/*");
            }
        });

        WordToPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permissions(UPLOAD_WORD_CODE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
        });

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permissions(IMAGE_CAPTURE_CODE, "image/jpeg");
            }
        });

        PDFtoWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permissions(UPLOAD_PDF_CODE, "application/pdf");
            }
        });

    }

    public void permissions(int code, String mime) {
        currentRequestCode = code;
        currentMimeType = mime;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                }, PERMISSION_CODE);
            } else {
                chooseFile(currentRequestCode, currentMimeType);
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_CODE);
            } else {
                chooseFile(currentRequestCode, currentMimeType);
            }
        } else {
            chooseFile(currentRequestCode, currentMimeType);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                chooseFile(currentRequestCode, currentMimeType);
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void chooseFile(int code, String mime) {
        switch (code) {
            case UPLOAD_WORD_CODE:
                openFilePicker(code, mime);
                break;
            case UPLOAD_IMG_CODE:
                openFilePicker(code, mime);
                break;
            case IMAGE_CAPTURE_CODE:
                openCamera();
                break;
            case UPLOAD_PDF_CODE:
                openFilePicker(code, mime);
                break;
        }
    }

    public void openFilePicker(int code, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(mime);
        startActivityForResult(intent, code);
    }

    public void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        //ContentResolver allows you to query, insert, update, and delete data from content providers.
        //When you want to insert new data into a content provider (e.g., adding a new image), you use ContentResolver to perform the insertion.
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == IMAGE_CAPTURE_CODE || requestCode == UPLOAD_IMG_CODE || requestCode == UPLOAD_WORD_CODE || requestCode == UPLOAD_PDF_CODE) && resultCode == RESULT_OK) {
            Uri uri = null;
            if (requestCode == IMAGE_CAPTURE_CODE) {
                uri = imageUri;
            } else if (data != null) {
                uri = data.getData();
            }
            if (uri != null) {
                switch (requestCode) {
                    case UPLOAD_WORD_CODE:
                        Intent wordDoc = new Intent(this, ViewActivity.class);
                        wordDoc.putExtra("URI", uri);
                        wordDoc.putExtra("code", UPLOAD_WORD_CODE);
                        wordDoc.putExtra("mimeType", currentMimeType);
                        startActivity(wordDoc);
                        break;
                    case UPLOAD_IMG_CODE:
                        Intent img = new Intent(this, ViewActivity.class);
                        img.putExtra("URI", uri);
                        img.putExtra("code", UPLOAD_IMG_CODE);
                        img.putExtra("mimeType", currentMimeType);
                        startActivity(img);
                        break;
                    case IMAGE_CAPTURE_CODE:
                        Intent camera = new Intent(this, ViewActivity.class);
                        camera.putExtra("URI", uri);
                        camera.putExtra("code", IMAGE_CAPTURE_CODE);
                        startActivity(camera);
                        break;
                    case UPLOAD_PDF_CODE:
                        Intent pdf = new Intent(this, ViewActivity.class);
                        pdf.putExtra("URI", uri);
                        pdf.putExtra("code", UPLOAD_PDF_CODE);
                        startActivity(pdf);
                        break;
                }
            }
        }
    }
}
