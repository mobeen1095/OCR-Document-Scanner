package com.example.mobeen.ocrscanner_fp;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

public class MainActivity extends AppCompatActivity {

    Button exit, contact, ppt, pdf, img;
    private AdView mAdView;
    private InterstitialAd iAd;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        exit = (Button)findViewById(R.id.button5);
        contact = (Button)findViewById(R.id.button);
        ppt = (Button)findViewById(R.id.button2);
        pdf = (Button)findViewById(R.id.button3);
        img = (Button)findViewById(R.id.button4);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    public void SaveCNT(View v) {
        startActivity(new Intent(MainActivity.this, ContactSaveScan.class));
    }

    public void SavePPT(View v) {
        startActivity(new Intent(MainActivity.this, PPTSaveScan.class));
    }

    public void SavePDF(View v) {
        startActivity(new Intent(MainActivity.this, PDFSaveScan.class));
    }

    public void SaveIMG(View v) {startActivity(new Intent(MainActivity.this, ImageSaveScan.class));}

    public void downloadActivity(View v) {startActivity(new Intent(MainActivity.this, DownloadOnline.class));}

    public void OnExit(View v) {
        new AlertDialog.Builder(this)
                .setTitle("Exit Confirmation")
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();

        iAd = new InterstitialAd(this);
        iAd.setAdUnitId("ca-app-pub-3064362887745866/2849451202");
        iAd.loadAd(new AdRequest.Builder().build());

        if(iAd.isLoaded())
            iAd.show();
    }

    public void onBackPressed() {
        View v = null;
        OnExit(v);
    }
}
