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

    // --- State variables ---
    private boolean isDetectingAction = false;
    private boolean isCooldown = false;
    private long phaseStartTime = 0;
    private String lastDetectionResult = "";

    // For tracking step-based gestures like waving
    private boolean waveStep1Done = false, waveStep2Done = false;
    private boolean leftWaveStep1Done = false, leftWaveStep2Done = false;

    // --- Main Detection ---
    private void detectAircraftPose(Pose pose, int imageWidth, int imageHeight) {
        // Get landmarks
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);

        if (rightWrist == null || rightElbow == null || leftWrist == null || leftElbow == null ||
                leftShoulder == null || rightShoulder == null) return;

        long currentTime = System.currentTimeMillis();

        // --- Cooldown phase ---
        if (isCooldown) {
            long cooldownElapsed = (currentTime - phaseStartTime) / 1000;
            long remainingCooldown = 5 - cooldownElapsed;

            if (remainingCooldown > 0) {
                runOnUiThread(() -> poseStatusText.setText(lastDetectionResult + " (" + remainingCooldown + ")"));
            } else {
                isCooldown = false;
                isDetectingAction = false;
            }
            updateSkeletonOverlay(imageWidth, imageHeight, leftWrist, rightWrist, leftElbow, rightElbow, leftShoulder, rightShoulder);
            return;
        }

        // --- Detection phase ---
        if (!isDetectingAction) {
            isDetectingAction = true;
            phaseStartTime = currentTime;
            resetGestureTracking();
        }

        long elapsed = (currentTime - phaseStartTime) / 1000;
        long remainingDetection = 5 - elapsed;

        if (remainingDetection > 0) {
            runOnUiThread(() -> poseStatusText.setText("Detecting Action (" + remainingDetection + ")"));
        }

        // Run all detectors
        boolean rightWave = detectWaveRight(rightWrist, rightElbow);
        boolean leftWave = detectWaveLeft(leftWrist, leftElbow);
        boolean leftArmRaised = detectLeftArmRaised(leftWrist, leftElbow, leftShoulder);
        boolean startEngine = detectStartEngine(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);

        // End of 5-second detection
        if (elapsed >= 5) {
            if (rightWave && leftWave) {
                lastDetectionResult = "Both Wave";
            }
            else if (startEngine) {
                lastDetectionResult = "Start Engine";
            }
            else if (rightWave) {
                lastDetectionResult = "Wave";
            }
            else if (leftArmRaised) {
                lastDetectionResult = "Left Arm Raised";
            }
             else {
                lastDetectionResult = "Unknown";
            }

            runOnUiThread(() -> poseStatusText.setText(lastDetectionResult + " (5)"));

            // Start cooldown
            isCooldown = true;
            phaseStartTime = currentTime;
        }

        // Always update overlay
        updateSkeletonOverlay(imageWidth, imageHeight, leftWrist, rightWrist, leftElbow, rightElbow, leftShoulder, rightShoulder);
    }

    // --- Gesture detection functions ---
    private boolean detectWaveRight(PoseLandmark wrist, PoseLandmark elbow) {
        if (!waveStep1Done &&
                wrist.getPosition().x < elbow.getPosition().x &&
                wrist.getPosition().y < elbow.getPosition().y) {
            waveStep1Done = true;
        }
        if (waveStep1Done && !waveStep2Done &&
                wrist.getPosition().x > elbow.getPosition().x &&
                wrist.getPosition().y < elbow.getPosition().y) {
            waveStep2Done = true;
        }
        return waveStep1Done && waveStep2Done;
    }

    private boolean detectWaveLeft(PoseLandmark wrist, PoseLandmark elbow) {
        if (!leftWaveStep1Done &&
                wrist.getPosition().x > elbow.getPosition().x &&
                wrist.getPosition().y < elbow.getPosition().y) {
            leftWaveStep1Done = true;
        }
        if (leftWaveStep1Done && !leftWaveStep2Done &&
                wrist.getPosition().x < elbow.getPosition().x &&
                wrist.getPosition().y < elbow.getPosition().y) {
            leftWaveStep2Done = true;
        }
        return leftWaveStep1Done && leftWaveStep2Done;
    }

    private boolean detectLeftArmRaised(PoseLandmark lw, PoseLandmark le, PoseLandmark ls) {
        if (lw == null || le == null || ls == null) return false;

        // Left arm raised to the right ("/" pose):
        return (
                le.getPosition().x > ls.getPosition().x && // elbow is left of shoulder
                        lw.getPosition().y < le.getPosition().y && // wrist above elbow
                        lw.getPosition().x > le.getPosition().x    // wrist right of elbow
        );
    }



    // Tracking phases
    private boolean startEnginePose1Done = false;
    private boolean startEnginePose2Done = false;
    private long startEngineStartTime = 0;

    private boolean detectStartEngine(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                      PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {

        long now = System.currentTimeMillis();

        // Pose 1: left arm static "/" + right arm up, wrist left of elbow
        boolean leftArmStatic = (
                le.getPosition().x > ls.getPosition().x && // elbow is left of shoulder
                        lw.getPosition().y < le.getPosition().y && // wrist above elbow
                        lw.getPosition().x > le.getPosition().x    // wrist right of elbow
        );
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y
        );

        if (!startEnginePose1Done &&
                leftArmStatic &&
                rightArmUp &&
                rw.getPosition().x < re.getPosition().x) {

            startEnginePose1Done = true;
            startEngineStartTime = now;
        }

        // Pose 2: left arm still static + right arm still up, wrist right of elbow
        if (startEnginePose1Done &&
                !startEnginePose2Done &&
                (now - startEngineStartTime <= 3000) && // within 3 seconds
                leftArmStatic &&
                rightArmUp &&
                rw.getPosition().x > re.getPosition().x) {

            startEnginePose2Done = true;
        }

        return startEnginePose1Done && startEnginePose2Done;
    }

    private void resetGestureTracking() {
        waveStep1Done = waveStep2Done = false;
        leftWaveStep1Done = leftWaveStep2Done = false;
        startEnginePose1Done = false;
        startEnginePose2Done = false;
        startEngineStartTime = 0;
    }




    // --- Overlay updater ---
    private void updateSkeletonOverlay(int imageWidth, int imageHeight,
                                       PoseLandmark lw, PoseLandmark rw,
                                       PoseLandmark le, PoseLandmark re,
                                       PoseLandmark ls, PoseLandmark rs) {
        int viewWidth = previewView.getWidth();
        int viewHeight = previewView.getHeight();
        Map<String, PointF> posePoints = new HashMap<>();
        if (lw != null) posePoints.put("LEFT_WRIST", mapPoint(lw, imageWidth, imageHeight, viewWidth, viewHeight));
        if (rw != null) posePoints.put("RIGHT_WRIST", mapPoint(rw, imageWidth, imageHeight, viewWidth, viewHeight));
        if (le != null) posePoints.put("LEFT_ELBOW", mapPoint(le, imageWidth, imageHeight, viewWidth, viewHeight));
        if (re != null) posePoints.put("RIGHT_ELBOW", mapPoint(re, imageWidth, imageHeight, viewWidth, viewHeight));
        if (ls != null) posePoints.put("LEFT_SHOULDER", mapPoint(ls, imageWidth, imageHeight, viewWidth, viewHeight));
        if (rs != null) posePoints.put("RIGHT_SHOULDER", mapPoint(rs, imageWidth, imageHeight, viewWidth, viewHeight));
        runOnUiThread(() -> poseOverlayView.updatePosePoints(posePoints));
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