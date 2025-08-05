package com.example.aircraftmarshalling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

// ML Kit Pose Detection
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

// ML Kit Vision Common
import com.google.mlkit.vision.common.InputImage;

// CameraX Image Analysis
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;


public class SimulationPage extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private PreviewView previewView;
    private PoseDetector poseDetector;
    private TextView poseStatusText;

    private String email, name, phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        email = intent.getStringExtra("email");
        phone = intent.getStringExtra("phone");

        setContentView(R.layout.activity_simulation_page);

        // 🔧 View references
        previewView = findViewById(R.id.previewView);
        FrameLayout runwayContainer = findViewById(R.id.runwayContainer);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        Button startSimButton = findViewById(R.id.startSim_button); // This is from your XML Button
        poseStatusText = findViewById(R.id.poseStatusText);

        // 🛬 Hide runway at start
        runwayContainer.setVisibility(View.GONE);

        AccuratePoseDetectorOptions options =
                new AccuratePoseDetectorOptions.Builder()
                        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                        .build();

        poseDetector = PoseDetection.getClient(options);

// 📷 Start camera right away (permission check)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }

// ▶️ Start Button Logic
        startSimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSimButton.setVisibility(View.GONE);        // hide the button
                runwayContainer.setVisibility(View.VISIBLE);    // show runway container

            }
        });

        // ✅ Bottom Navigation setup
        bottomNavigationView.setSelectedItemId(R.id.nav_simulation); // highlight current tab


        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_simulation) {
                return true; // Already on this page
            } else if (itemId == R.id.nav_module) {
                Intent moduleIntent = new Intent(SimulationPage.this, ModulePage.class);
                moduleIntent.putExtra("name", getIntent().getStringExtra("name"));
                moduleIntent.putExtra("email", getIntent().getStringExtra("email"));
                moduleIntent.putExtra("phone", getIntent().getStringExtra("phone"));
                startActivity(moduleIntent);
            } else if (itemId == R.id.nav_assessment) {
                Intent intent1 = new Intent(SimulationPage.this, AssessmentPage.class);
                intent1.putExtra("name", getIntent().getStringExtra("name")); // or pass stored variable
                intent1.putExtra("email", getIntent().getStringExtra("email"));
                intent1.putExtra("phone", getIntent().getStringExtra("phone"));
                startActivity(intent1);
            } else {
                return false;
            }

            overridePendingTransition(0, 0); // optional: no animation between screens

            return true;
        });
    }


    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 🧠 Create Image Analysis use case
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    processImageProxy(image);
                });

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("SimulationPage", "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImageProxy(ImageProxy imageProxy) {
        @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
        android.media.Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        detectAircraftPose(pose);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PoseDetection", "Detection failed", e);
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close(); // must close the image
                    });
        }
    }

//    private void detectAircraftPose(Pose pose) {
//        PoseLandmark leftHand = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
//        PoseLandmark rightHand = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
//        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
//        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
//
//        if (leftHand != null && rightHand != null &&
//                leftShoulder != null && rightShoulder != null) {
//
//            float leftY = leftHand.getPosition().y;
//            float rightY = rightHand.getPosition().y;
//
//            runOnUiThread(() -> {
//                poseStatusText.setVisibility(View.VISIBLE); // Show the text
//
//                if (leftY < leftShoulder.getPosition().y && rightY < rightShoulder.getPosition().y) {
//                    poseStatusText.setText("🛫 All Clear Signal Detected");
//                } else {
//                    poseStatusText.setText("✋ Unknown Pose");
//                }
//            });
//        }
//    }

    private void detectAircraftPose(Pose pose) {
        PoseLandmark leftHand = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightHand = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);

        if (leftHand != null && rightHand != null &&
                leftShoulder != null && rightShoulder != null) {

            float leftY = leftHand.getPosition().y;
            float rightY = rightHand.getPosition().y;
            float leftShoulderY = leftShoulder.getPosition().y;
            float rightShoulderY = rightShoulder.getPosition().y;

            runOnUiThread(() -> {
                poseStatusText.setVisibility(View.VISIBLE); // Show the text

                if (leftY < leftShoulderY && rightY >= rightShoulderY) {
                    poseStatusText.setText("🖐 Left Hand Raised");
                } else if (rightY < rightShoulderY && leftY >= leftShoulderY) {
                    poseStatusText.setText("🖐 Right Hand Raised");
                } else {
                    poseStatusText.setText("✋ Unknown Pose");
                }
            });
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
