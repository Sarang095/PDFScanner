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
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;


import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;

import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ViewActivity extends AppCompatActivity {

    private Uri uri;
    private String mime;
    private int code;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view);

        uri = getIntent().getParcelableExtra("URI");
        mime = getIntent().getStringExtra("mimeType");
        code = getIntent().getIntExtra("code", 0);
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

                switch (code) {
                    case 3:
                        try {
                            extractedText = extractTextWord(uri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Uri pdfUri = convertWordToPdf(extractedText);
                            downloadPdf(pdfUri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;

                    case 5:
                        try {
                            extractedText = extractTextPdf(uri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Uri wordUri = pdftoWord(extractedText);
                            downloadWord(wordUri);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                }
            }
        });
    }

    private String extractTextWord(Uri uri) throws IOException {
        ContentResolver contentResolver = getContentResolver(); // declaring teh content resolver instance to perform the "get" action
        try (InputStream inputStream = contentResolver.openInputStream(uri)) {  // taking the word document from the uri with the help of the inputstream and the contentResolver
            XWPFDocument document = new XWPFDocument(inputStream);
            XWPFWordExtractor extractor = new XWPFWordExtractor(document); //using the XWPFWordExtractor for the extracting the text from the word doc
            return extractor.getText(); //converting the extracted text into string.
        }
    }

    private String extractTextPdf(Uri uri) throws IOException {
        ContentResolver contentResolver = getContentResolver();
        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            PDFBoxResourceLoader.init(getApplicationContext()); // Initialize PDFBoxResourceLoader
            PDDocument document = PDDocument.load(inputStream); //
            StringBuilder text = new StringBuilder();
            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                PDFTextStripper stripper = new PDFTextStripper(); //Used the PDFTextStripper to extract the text form the pdf
                text.append(stripper.getText(document));
            }
            document.close();
            return text.toString();
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

    private Uri pdftoWord(String text) throws IOException {
        File wordFile = new File(getFilesDir(), "NewDocument.docx"); //first we created the word file in mobile's internal storage

        try (XWPFDocument document = new XWPFDocument()) {  //created a word document to perform the actions
            String[] lines = text.split("\n");  // then we declared a string array and added the splitted text in multiple lines

            for (String line : lines) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line);
            }

            try (OutputStream outputStream = new FileOutputStream(wordFile)) {  //now
                document.write(outputStream); //used the output stream to write those lines to a wordFile.
            }
        }

        return saveWordToMediaStore(wordFile);
    }

    private Uri saveWordToMediaStore(File wordFile) throws IOException {
        ContentResolver contentResolver = getContentResolver();
        ContentValues contentValues = new ContentValues(); //used the content values to label those file in the internal filesystem
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NewDocument.docx");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS);

        Uri wordUri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

        if (wordUri != null) {
            try (OutputStream out = contentResolver.openOutputStream(wordUri);  //using outputStream to write the text to the word file
                 FileInputStream in = new FileInputStream(wordFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {  //running th while loop until the complete bytes from the pdf is taken
                    out.write(buffer, 0, bytesRead);  //writing those byte data in the file
                }
            } catch (IOException e) {
                throw new IOException("Error writing to output stream", e);
            }
        } else {
            throw new IOException("Failed to create new MediaStore entry");
        }

        return wordUri;
    }

    private void downloadWord(Uri wordUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(wordUri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Download Word using"));
    }
}
