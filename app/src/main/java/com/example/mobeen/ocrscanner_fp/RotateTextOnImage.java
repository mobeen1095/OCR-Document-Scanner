package com.example.mobeen.ocrscanner_fp;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.itextpdf.text.Document;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Created by mobeen on 8/12/17.
 */

public class RotateTextOnImage extends AppCompatActivity {

    private static int RESULT_LOAD_IMAGE = 1;
    Button save, rotate;
    ImageView image;
    String recvText;
    Bitmap bitmap;
    Canvas canvas;
    Dialog myDialog;
    int rotated = 0;
    private AdView mAdView;
    private InterstitialAd iAd;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_rotate_on_img);
        Bundle bundle = getIntent().getExtras();
        recvText = bundle.getString("TextToRotate");

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        save = (Button)findViewById(R.id.saveImg);
        rotate = (Button)findViewById(R.id.rotateTxt);
        image = (ImageView)findViewById(R.id.imageView2);

        iAd = new InterstitialAd(RotateTextOnImage.this);
        iAd.setAdUnitId("ca-app-pub-3064362887745866/2849451202");
        iAd.loadAd(new AdRequest.Builder().build());

        if(iAd.isLoaded())
            iAd.show();

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, RESULT_LOAD_IMAGE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            cursor.close();

            try {
                bitmap = getBitmapFromUri(selectedImage);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            image.setImageBitmap(drawTextToBitmap(this, bitmap, recvText));
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public Bitmap drawTextToBitmap(Context gContext,
                                   Bitmap bitmap,
                                   String gText) {
        Resources resources = gContext.getResources();
        float scale = resources.getDisplayMetrics().density;

        android.graphics.Bitmap.Config bitmapConfig =
                bitmap.getConfig();

        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        bitmap = bitmap.copy(bitmapConfig, true);

        canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.RED);
        paint.setTextSize((int) (15 * scale));

        Rect bounds = new Rect();

        int noOfLines = 0;
        for (String line: gText.split("\n")) {
            noOfLines++;
        }

        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (canvas.getWidth() / 2);
        int y = (int) ((canvas.getHeight() / 2) - (paint.descent() + paint.ascent()) / 2);

        canvas.rotate(rotated, x, y);
        rotated-=15;

        Paint mPaint = new Paint();
        mPaint.setColor(Color.RED);

        for (String line: gText.split("\n")) {
            canvas.drawText(line, x, y, paint);
            y += paint.descent() - paint.ascent();
        }
        return bitmap;
    }

    public void rotateTheImg(View v){
        image.setImageBitmap(drawTextToBitmap(this, bitmap, recvText));
    }

    public void saveTheImg(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(RotateTextOnImage.this);
        builder.setTitle("Enter the File Name for Image File :");
        builder.setCancelable(false);

        final EditText input = new EditText(RotateTextOnImage.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = input.getText().toString();

                image.setDrawingCacheEnabled(true);
                image.buildDrawingCache();

                Bitmap bitmap2 = Bitmap.createBitmap(image.getDrawingCache());
                SaveIamge(bitmap2, m_Text);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                myDialog = null;
            }
        });
        if(myDialog == null) {
            myDialog = builder.create();
            myDialog.show();
        }
    }

    private void SaveIamge(Bitmap finalBitmap, String fileName) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/OCR_FP/IMG_Files");

        if (!myDir.exists())
            myDir.mkdirs();

        String fname = fileName+".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            Toast.makeText(RotateTextOnImage.this, "Created IMG and Saved to Storage.", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        }

        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(RotateTextOnImage.this);
        Bundle params = new Bundle();
        params.putString("Name", fname);
        mFirebaseAnalytics.logEvent("IMGFileNameSaved", params);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        Uri fileUp = Uri.fromFile(new File(myDir, fname));
        StorageReference storageRef = storage.getReference();
        StorageReference riversRef = storageRef.child("IMG_Files/"+fname);

        riversRef.putFile(fileUp)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(RotateTextOnImage.this, "File Uploaded.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(RotateTextOnImage.this,exception.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
    }
}