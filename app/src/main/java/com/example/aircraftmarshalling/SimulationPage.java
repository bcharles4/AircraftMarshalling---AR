package com.example.aircraftmarshalling;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SimulationPage extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private PoseOverlayView poseOverlayView;
    private TextView poseStatusText;
    private PoseDetector poseDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation_page);

        previewView = findViewById(R.id.previewView);
        poseOverlayView = findViewById(R.id.poseOverlayView);
        poseStatusText = findViewById(R.id.poseStatusText);
        Button startSimButton = findViewById(R.id.startSim_button);
        FrameLayout runwayContainer = findViewById(R.id.runwayContainer);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        runwayContainer.setVisibility(View.GONE);

        AccuratePoseDetectorOptions options =
                new AccuratePoseDetectorOptions.Builder()
                        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                        .build();

        poseDetector = PoseDetection.getClient(options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }

        startSimButton.setOnClickListener(v -> {
            startSimButton.setVisibility(View.GONE);
            runwayContainer.setVisibility(View.VISIBLE);
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_simulation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_simulation) return true;
            if (itemId == R.id.nav_module)
                startActivity(new Intent(this, ModulePage.class));
            else if (itemId == R.id.nav_assessment)
                startActivity(new Intent(this, AssessmentPage.class));
            overridePendingTransition(0, 0);
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

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::processImageProxy);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("SimulationPage", "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImageProxy(ImageProxy imageProxy) {
        @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            poseDetector.process(image)
                    .addOnSuccessListener(pose -> detectAircraftPose(pose, image.getWidth(), image.getHeight()))
                    .addOnFailureListener(e -> Log.e("PoseDetection", "Detection failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private void detectAircraftPose(Pose pose, int imageWidth, int imageHeight) {
        // Get landmarks
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);

        if (leftWrist != null && rightWrist != null &&
                leftShoulder != null && rightShoulder != null &&
                leftElbow != null && rightElbow != null) {

            // Mirror-aware: since you're using the front camera (mirrored preview)
            // what appears as right on screen is actually left in MLKit's coordinate
            // system. So we'll interpret your "right arm" as MLKit's LEFT arm.

            String detectedPose = "Unknown";

            float wristsDistance = distance(
                    leftWrist.getPosition().x, leftWrist.getPosition().y,
                    rightWrist.getPosition().x, rightWrist.getPosition().y);

            float shouldersDistance = distance(
                    leftShoulder.getPosition().x, leftShoulder.getPosition().y,
                    rightShoulder.getPosition().x, rightShoulder.getPosition().y);

            float leftArmXDiff = leftWrist.getPosition().x - leftShoulder.getPosition().x;
            float leftArmYDiff = leftWrist.getPosition().y - leftShoulder.getPosition().y;

            float rightArmXDiff = rightWrist.getPosition().x - rightShoulder.getPosition().x;
            float rightArmYDiff = rightWrist.getPosition().y - rightShoulder.getPosition().y;

            // ==== Pose 1: "Negative" (Your right arm stretched to side, other arm down) ====
            if (
                // MLKit's LEFT arm (your right) is stretched sideways
                    leftArmXDiff > shouldersDistance * 0.8 &&
                            Math.abs(leftArmYDiff) < shouldersDistance * 0.4 &&
                            // MLKit's RIGHT arm (your left) is down
                            rightWrist.getPosition().y > rightShoulder.getPosition().y + shouldersDistance * 0.5
            ) {
                detectedPose = "Negative";
            }

            // ==== Pose 2: "Normal Stop" - Arms raised, wrists close ====
            else if (
                    leftWrist.getPosition().y < leftShoulder.getPosition().y &&
                            rightWrist.getPosition().y < rightShoulder.getPosition().y &&
                            wristsDistance < shouldersDistance * 0.6
            ) {
                detectedPose = "Normal Stop";
            }

            // ==== Pose 3: "Hold Position" - Both wrists below respective shoulders ====
            else if (
                    leftWrist.getPosition().y > leftShoulder.getPosition().y &&
                            rightWrist.getPosition().y > rightShoulder.getPosition().y
            ) {
                detectedPose = "Hold Position";
            }

            // Update UI
            String finalDetectedPose = detectedPose;
            runOnUiThread(() -> {
                poseStatusText.setVisibility(View.VISIBLE);
                poseStatusText.setText("Detected Pose: " + finalDetectedPose);
//                Toast.makeText(this, "🧍 Pose: " + finalDetectedPose, Toast.LENGTH_SHORT).show();
            });

            // Optional: Draw points
//            int viewWidth = previewView.getWidth();
//            int viewHeight = previewView.getHeight();
//            Map<String, PointF> posePoints = new HashMap<>();
//            posePoints.put("LEFT_WRIST", mapPoint(leftWrist, imageWidth, imageHeight, viewWidth, viewHeight));
//            posePoints.put("RIGHT_WRIST", mapPoint(rightWrist, imageWidth, imageHeight, viewWidth, viewHeight));
//            posePoints.put("LEFT_ELBOW", mapPoint(leftElbow, imageWidth, imageHeight, viewWidth, viewHeight));
//            posePoints.put("RIGHT_ELBOW", mapPoint(rightElbow, imageWidth, imageHeight, viewWidth, viewHeight));
//            posePoints.put("LEFT_SHOULDER", mapPoint(leftShoulder, imageWidth, imageHeight, viewWidth, viewHeight));
//            posePoints.put("RIGHT_SHOULDER", mapPoint(rightShoulder, imageWidth, imageHeight, viewWidth, viewHeight));
//            runOnUiThread(() -> poseOverlayView.updatePosePoints(posePoints));
        }
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private PointF mapPoint(PoseLandmark landmark, int imageWidth, int imageHeight, int viewWidth, int viewHeight) {
        float x = landmark.getPosition().x * ((float) viewWidth / imageWidth);
        float y = landmark.getPosition().y * ((float) viewHeight / imageHeight);
        return new PointF(x, y);
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
