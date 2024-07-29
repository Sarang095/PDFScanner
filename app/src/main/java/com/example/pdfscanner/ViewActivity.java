package com.example.pdfscanner;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ViewActivity extends AppCompatActivity {

    private Uri uri;
    private String mime;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view);

        uri = getIntent().getParcelableExtra("URI");
        mime = getIntent().getStringExtra("mimeType");
        imageView = findViewById(R.id.image_view);
        Button download = findViewById(R.id.download);
        TextView pdfView = findViewById(R.id.pdf_view);

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String extractedText = null;
                try {
                    extractedText = extractText(uri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Uri pdfUri = convertWordToPdf(extractedText);
                    downloadPdf(pdfUri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private String extractText(Uri uri) throws IOException {
        ContentResolver contentResolver = getContentResolver();
        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            XWPFDocument document = new XWPFDocument(inputStream);
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    private Uri convertWordToPdf(String text) throws IOException {
        File pdfFile = new File(getFilesDir(), "NewPDF.pdf");

        try (PdfWriter writer = new PdfWriter(pdfFile)) {
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            String[] lines = text.split("\n");

            for (String line : lines) {
                document.add(new Paragraph(line).setFont(font).setFontSize(12));
            }

            document.close();
        }

        return savePdfToMediaStore(pdfFile);
    }

    private Uri savePdfToMediaStore(File pdfFile) throws IOException {
        ContentResolver contentResolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NewPDF.pdf");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

        Uri pdfUri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

        if (pdfUri != null) {
            try (OutputStream out = contentResolver.openOutputStream(pdfUri);
                 FileInputStream in = new FileInputStream(pdfFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new IOException("Error writing to output stream", e);
            }
        } else {
            throw new IOException("Failed to create new MediaStore entry");
        }

        return pdfUri;
    }

    private void downloadPdf(Uri pdfUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Download PDF using"));
    }
}
