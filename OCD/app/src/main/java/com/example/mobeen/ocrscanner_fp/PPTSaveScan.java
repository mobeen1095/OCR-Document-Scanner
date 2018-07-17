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
import com.independentsoft.office.Unit;
import com.independentsoft.office.UnitType;
import com.independentsoft.office.drawing.Extents;
import com.independentsoft.office.drawing.Offset;
import com.independentsoft.office.drawing.PresetGeometry;
import com.independentsoft.office.drawing.ShapeLocking;
import com.independentsoft.office.drawing.ShapeType;
import com.independentsoft.office.drawing.TextAlignmentType;
import com.independentsoft.office.drawing.TextParagraph;
import com.independentsoft.office.drawing.TextRun;
import com.independentsoft.office.drawing.Transform2D;
import com.independentsoft.office.drawing.TransformGroup2D;
import com.independentsoft.office.presentation.CommonSlideData;
import com.independentsoft.office.presentation.NotesSlideSize;
import com.independentsoft.office.presentation.Presentation;
import com.independentsoft.office.presentation.Slide;
import com.independentsoft.office.presentation.SlideLayout;
import com.independentsoft.office.presentation.SlideMaster;
import com.independentsoft.office.presentation.SlideMasterTextStyles;
import com.independentsoft.office.presentation.SlideMasterTitleTextStyle;
import com.independentsoft.office.presentation.SlideSize;
import com.independentsoft.office.presentation.SlideSizeType;
import com.independentsoft.office.presentation.drawing.GroupShape;
import com.independentsoft.office.presentation.drawing.Placeholder;
import com.independentsoft.office.presentation.drawing.PlaceholderType;
import com.independentsoft.office.presentation.drawing.Shape;
import com.independentsoft.office.presentation.drawing.ShapeTextBody;

import java.io.File;
import java.io.IOException;


/**
 * Created by mobeen on 8/3/17.
 */

public class PPTSaveScan extends AppCompatActivity {
    SurfaceView cameraView;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    final int RequestStorageWritePermissionID = 1002;
    Dialog myDialog;
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

                            ActivityCompat.requestPermissions(PPTSaveScan.this,
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

                        PPTSaveScan.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                                        ActivityCompat.requestPermissions(PPTSaveScan.this,
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                RequestStorageWritePermissionID);
                                        return;
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                                AlertDialog.Builder builder = new AlertDialog.Builder(PPTSaveScan.this);
                                builder.setTitle("Enter the File Name for PPT File :");
                                builder.setCancelable(false);

                                final EditText input = new EditText(PPTSaveScan.this);
                                input.setInputType(InputType.TYPE_CLASS_TEXT);
                                builder.setView(input);

                                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String m_Text = input.getText().toString();
                                        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OCR_FP/PPTX_Slides";

                                        File dir = new File(fullPath);
                                        if (!dir.exists())
                                            dir.mkdirs();

                                        File file = new File(fullPath, m_Text+".pptx");

                                        Presentation presentation = new Presentation();
                                        GroupShape shapeTree = new GroupShape();
                                        shapeTree.setID("1");
                                        shapeTree.setName("ShapeTree");
                                        shapeTree.getShapeProperties().setTransformGroup2D(new TransformGroup2D());

                                        TextRun run = new TextRun(myText);

                                        TextParagraph paragraph = new TextParagraph();
                                        paragraph.setAlignment(TextAlignmentType.LEFT);
                                        paragraph.getContent().add(run);

                                        ShapeTextBody textBody = new ShapeTextBody();
                                        textBody.getParagraphs().add(paragraph);

                                        Shape shape = new Shape();
                                        shape.setID("2");
                                        shape.setName("Title 1");
                                        shape.setLocking(new ShapeLocking());
                                        shape.getLocking().setDisallowGrouping(true);
                                        shape.setPlaceholder(new Placeholder(PlaceholderType.TITLE));
                                        shape.setTextBody(textBody);

                                        Unit offsetX = new Unit(2, UnitType.INCH);
                                        Unit offsetY = new Unit(1, UnitType.INCH);

                                        Unit width = new Unit(6, UnitType.INCH);
                                        Unit height = new Unit(2, UnitType.INCH);

                                        shape.getShapeProperties().setTransform2D(new Transform2D());
                                        shape.getShapeProperties().getTransform2D().setOffset(new Offset(offsetX, offsetY));
                                        shape.getShapeProperties().getTransform2D().setExtents(new Extents(width, height));
                                        shape.getShapeProperties().setPresetGeometry(new PresetGeometry(ShapeType.RECTANGLE));

                                        shapeTree.getElements().add(shape);

                                        CommonSlideData commonSlideData = new CommonSlideData();
                                        commonSlideData.setShapeTree(shapeTree);

                                        SlideLayout layout = new SlideLayout();
                                        layout.setCommonSlideData(getLayoutCommonSlideData());

                                        Slide slide = new Slide();
                                        slide.setCommonSlideData(commonSlideData);
                                        slide.setLayout(layout);

                                        SlideMaster master = new SlideMaster();
                                        master.setCommonSlideData(getLayoutCommonSlideData());
                                        master.getLayouts().add(layout);

                                        master.setTextStyles(new SlideMasterTextStyles());
                                        master.getTextStyles().setTitleStyle(new SlideMasterTitleTextStyle());
                                        master.getTextStyles().getTitleStyle().getListLevel1TextStyle().getDefaultTextRunProperties().setFontSize(20);

                                        presentation.getSlides().add(slide);
                                        presentation.getSlideMasters().add(master);

                                        presentation.setSlideSize(new SlideSize(9144000, 6858000, SlideSizeType.SCREEN_16X9));
                                        presentation.setNotesSlideSize(new NotesSlideSize(6858000, 9144000));

                                        try {
                                            presentation.save(file.toString(), true);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        myDialog = null;

                                        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(PPTSaveScan.this);
                                        Bundle params = new Bundle();
                                        params.putString("Name", m_Text+".pptx");
                                        mFirebaseAnalytics.logEvent("PPTFileNameSaved", params);

                                        FirebaseStorage storage = FirebaseStorage.getInstance();
                                        Uri fileUp = Uri.fromFile(new File(fullPath, m_Text+".pptx"));
                                        StorageReference storageRef = storage.getReference();
                                        StorageReference riversRef = storageRef.child("PPTX_Docs/"+m_Text+".pptx");

                                        riversRef.putFile(fileUp)
                                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                        Toast.makeText(PPTSaveScan.this, "File Uploaded.", Toast.LENGTH_LONG).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception exception) {
                                                        Toast.makeText(PPTSaveScan.this,exception.getMessage(),Toast.LENGTH_LONG).show();
                                                    }
                                                });

                                        Toast.makeText(PPTSaveScan.this, "Created PPTX and Saved to Storage.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        myDialog = null;

                                        iAd = new InterstitialAd(PPTSaveScan.this);
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

    static CommonSlideData getLayoutCommonSlideData() {
        GroupShape shapeTree = new GroupShape();
        shapeTree.setID("1");
        shapeTree.setName("layout");

        shapeTree.getShapeProperties().setTransformGroup2D(new TransformGroup2D());

        CommonSlideData commonSlideData = new CommonSlideData();
        commonSlideData.setShapeTree(shapeTree);

        return commonSlideData;
    }
}
