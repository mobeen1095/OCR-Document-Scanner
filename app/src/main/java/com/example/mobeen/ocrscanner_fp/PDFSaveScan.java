package com.example.mobeen.ocrscanner_fp;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created by mobeen on 8/3/17.
 */

public class PDFSaveScan extends AppCompatActivity {

    SurfaceView cameraView;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    final int RequestStorageWritePermissionID = 1002;
    Dialog myDialog;
    private AdView mAdView;
    private InterstitialAd iAd;
    private StorageReference mStorageRef;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }

            case RequestStorageWritePermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }

            try {
                cameraSource.start(cameraView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }

            break;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pdf_activity);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        cameraView = (SurfaceView) findViewById(R.id.surface_view);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(PDFSaveScan.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {}

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if(items.size() != 0)
                    {
                        StringBuilder stringBuilder = new StringBuilder();
                        for(int i =0;i<items.size();++i)
                        {
                            TextBlock item = items.valueAt(i);
                            stringBuilder.append(item.getValue());
                            stringBuilder.append("\n");
                        }

                        final String myText = stringBuilder.toString();

                        PDFSaveScan.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                                        ActivityCompat.requestPermissions(PDFSaveScan.this,
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                RequestStorageWritePermissionID);
                                        return;
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                                AlertDialog.Builder builder = new AlertDialog.Builder(PDFSaveScan.this);
                                builder.setTitle("Enter the File Name for PDF File :");
                                builder.setCancelable(false);

                                final EditText input = new EditText(PDFSaveScan.this);
                                input.setInputType(InputType.TYPE_CLASS_TEXT);
                                builder.setView(input);

                                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String m_Text = input.getText().toString();

                                        Document document = new Document();
                                        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OCR_FP/PDF_Docs";

                                        File dir = new File(fullPath);
                                        if (!dir.exists())
                                            dir.mkdirs();

                                        File file = new File(fullPath, m_Text+".pdf");
                                        try {
                                            PdfWriter.getInstance(document, new FileOutputStream(file));
                                        } catch (DocumentException e) {
                                            e.printStackTrace();
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                        document.open();
                                        try {
                                            document.add(new Paragraph(myText));
                                        } catch (DocumentException e) {
                                            e.printStackTrace();
                                        }
                                        document.close();
                                        myDialog = null;

                                        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(PDFSaveScan.this);
                                        Bundle params = new Bundle();
                                        params.putString("Name", m_Text+".pdf");
                                        mFirebaseAnalytics.logEvent("PDFFileNameSaved", params);

                                        FirebaseStorage storage = FirebaseStorage.getInstance();
                                        Uri fileUp = Uri.fromFile(new File(fullPath, m_Text+".pdf"));
                                        StorageReference storageRef = storage.getReference();
                                        StorageReference riversRef = storageRef.child("PDF_Docs/"+m_Text+".pdf");

                                        riversRef.putFile(fileUp)
                                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                        Toast.makeText(PDFSaveScan.this, "File Uploaded.", Toast.LENGTH_LONG).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception exception) {
                                                       Toast.makeText(PDFSaveScan.this,exception.getMessage(),Toast.LENGTH_LONG).show();
                                                    }
                                                });

                                        Toast.makeText(PDFSaveScan.this, "Created PDF and Saved to Storage.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        myDialog = null;

                                        iAd = new InterstitialAd(PDFSaveScan.this);
                                        iAd.setAdUnitId("ca-app-pub-3064362887745866/2849451202");
                                        iAd.loadAd(new AdRequest.Builder().build());

                                        if(iAd.isLoaded())
                                            iAd.show();
                                    }
                                });
                                if(myDialog == null) {
                                    myDialog = builder.create();
                                    myDialog.show();
                                }
                            }
                        });
                    }
                }
            });
        }
    }
}

