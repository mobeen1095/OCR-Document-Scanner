package com.example.mobeen.ocrscanner_fp;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;

/**
 * Created by mobeen on 8/3/17.
 */

public class ContactSaveScan extends AppCompatActivity {

    SurfaceView cameraView;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    Dialog myDialog = null;
    private AdView mAdView;
    private InterstitialAd iAd;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.business_activity);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        cameraView = (SurfaceView) findViewById(R.id.surface_view);

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

                            ActivityCompat.requestPermissions(ContactSaveScan.this,
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

                        String cardData = stringBuilder.toString();
                        String contactNum = "num";

                        String[] subStr = cardData.split("\n");
                        for(int j = 0; j < subStr.length; j++){
                            if (subStr[j].contains("cel") || subStr[j].contains("cell") || subStr[j]
                                    .contains("Cel") || subStr[j].contains("Cell") || subStr[j].
                                    contains("Tel") || subStr[j].contains("tel")){
                                contactNum = subStr[j].split(":")[1];
                                break;
                            }
                        }

                       if(!contactNum.equals("num")) {
                           final String finalContactNum = contactNum;
                           ContactSaveScan.this.runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   AlertDialog.Builder builder = new AlertDialog.Builder(ContactSaveScan.this);
                                   builder.setTitle("Enter the Name :");

                                   final EditText input = new EditText(ContactSaveScan.this);
                                   input.setInputType(InputType.TYPE_CLASS_TEXT);
                                   builder.setView(input);
                                   final String finalContact = finalContactNum;
                                   builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialog, int which) {
                                           myDialog = null;
                                           String m_Text = input.getText().toString();
                                           Intent intent = new Intent(Intent.ACTION_INSERT);
                                           intent.setType(ContactsContract.Contacts.CONTENT_TYPE);

                                           intent.putExtra(ContactsContract.Intents.Insert.NAME, m_Text);
                                           intent.putExtra(ContactsContract.Intents.Insert.PHONE, finalContact);
                                           startActivityForResult(intent, 1);

                                           FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(ContactSaveScan.this);
                                           Bundle params = new Bundle();
                                           params.putString("Name", m_Text);
                                           mFirebaseAnalytics.logEvent("ContactNameSaved", params);

                                       }
                                   });
                                   builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialog, int which) {
                                           dialog.cancel();
                                           myDialog = null;

                                           iAd = new InterstitialAd(ContactSaveScan.this);
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
                }
            });
        }
    }
}