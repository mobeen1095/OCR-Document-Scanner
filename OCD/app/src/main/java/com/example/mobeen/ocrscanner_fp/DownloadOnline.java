package com.example.mobeen.ocrscanner_fp;
import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;


/**
 * Created by mobeen on 8/14/17.
 */

public class DownloadOnline extends AppCompatActivity {

    TextView tv;
    EditText et;
    public Button b;

    final int RequestStorageWritePermissionID = 1002;
    private AdView mAdView;
    private InterstitialAd iAd;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestStorageWritePermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
            }
        }
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_online);

        tv = (TextView)findViewById(R.id.textView2);
        et = (EditText)findViewById(R.id.editText);
        b = (Button)findViewById(R.id.button6);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        iAd = new InterstitialAd(DownloadOnline.this);
        iAd.setAdUnitId("ca-app-pub-3064362887745866/2849451202");
        iAd.loadAd(new AdRequest.Builder().build());

        if(iAd.isLoaded())
            iAd.show();

        b.setOnClickListener(new View.OnClickListener(){
         public void onClick(View v){
             try {
                 if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                     ActivityCompat.requestPermissions(DownloadOnline.this,
                             new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                             RequestStorageWritePermissionID);
                     return;
                 }
             }catch (Exception e){
                 e.printStackTrace();
             }
;
             final String fileName = et.getText().toString();
             if(!fileName.equals("")){
                 et.setText("");
                 String[] parts = fileName.split(Pattern.quote("."));

                 String DirFileName = fileName;
                 if(parts[1].equals("pdf"))
                     DirFileName = "PDF_Docs";
                 if(parts[1].equals("pptx"))
                     DirFileName = "PPTX_Docs";
                 if(parts[1].equals("jpg"))
                     DirFileName = "IMG_Files";

                 FirebaseStorage storage = FirebaseStorage.getInstance();
                 StorageReference storageRef = storage.getReferenceFromUrl("gs://ocrscanner-fp.appspot.com/");
                 StorageReference  islandRef = storageRef.child(DirFileName+"/"+fileName);

                 File rootPath = new File(Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS);
                 if(!rootPath.exists()) {
                     rootPath.mkdirs();
                 }

                 File localFile = null;

                 try {
                     localFile = File.createTempFile(DirFileName+"-","-"+parts[0]+"."+parts[1], rootPath);
                 } catch (IOException e) {
                     e.printStackTrace();
                 }

                 final File finalLocalFile = localFile;
                 islandRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                     @Override
                     public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                         Toast.makeText(DownloadOnline.this, "Download Complete.", Toast.LENGTH_LONG).show();
                         FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(DownloadOnline.this);
                         Bundle params = new Bundle();
                         params.putString("Name", fileName);
                         mFirebaseAnalytics.logEvent("DownloadedFileName", params);
                     }
                 }).addOnFailureListener(new OnFailureListener() {
                     @Override
                     public void onFailure(@NonNull Exception exception) {
                         Toast.makeText(DownloadOnline.this, "File with such name does not exist, Try Again.", Toast.LENGTH_LONG).show();
                         if(finalLocalFile.exists()){
                             finalLocalFile.delete();
                         }
                     }
                 });
             }
             else
                Toast.makeText(DownloadOnline.this, "Please enter file name.", Toast.LENGTH_LONG).show();
            }
        });
    }
}