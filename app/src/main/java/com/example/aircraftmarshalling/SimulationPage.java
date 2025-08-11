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
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

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
        long remainingDetection = 4 - elapsed;

        if (remainingDetection > 0) {
            runOnUiThread(() -> poseStatusText.setText("Detecting Action (" + remainingDetection + ")"));
        }

        // Run all detectors
        boolean startEngine = detectStartEngine(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean negativeSignal = detectNegative(leftShoulder, leftElbow, leftWrist, rightElbow, rightWrist);
        boolean normalStop = detectNormalStop(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder); // ✅ New detector
        boolean emergencyStop = detectEmergencyStop(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder); // ✅ New detector
        boolean holdPosition = detectHoldPosition(leftShoulder, leftElbow,leftWrist, rightShoulder,rightElbow,rightWrist);
        boolean turnRight = detectTurnRight(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean chalkInstalled = detectChalkInstalled(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean slowDown = detectSlowDown(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean shutOffEngine = detectShutOffEngine(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean passControl = detectPassControl(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean engineFire = detectEngineOnFire(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean brakeFire = detectBrakesOnFire(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);

        // End of 5-second detection
        if (elapsed >= 4) {
            if (normalStop) { // ✅ Added
                lastDetectionResult = "Normal Stop";
            }
            else if (emergencyStop) { // ✅ Added
                lastDetectionResult = "Emergency Stop";
            }
            else if (startEngine) {
                lastDetectionResult = "Start Engine";
            }
            else if (turnRight) {
                lastDetectionResult = "Turn Right";
            }
            else if (engineFire) {
                lastDetectionResult = "Engine on Fire";
            }
            else if (brakeFire) {
                lastDetectionResult = "Brakes on Fire";
            }
            else if (slowDown) {
                lastDetectionResult = "Slow Down";
            }
            else if (shutOffEngine) {
                lastDetectionResult = "Shut Off Engine";
            }
            else if (passControl) {
                lastDetectionResult = "Pass Control";
            }
            else if (negativeSignal) {
                lastDetectionResult = "Negative";
            }
            else if (chalkInstalled) {
                lastDetectionResult = "Chalk Installed";
            }
            else if (holdPosition) {
                lastDetectionResult = "Hold Position";
            } else {
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
                        lw.getPosition().y < le.getPosition().y &&
                        le.getPosition().y < ls.getPosition().y &&// wrist above elbow
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

    private boolean detectNegative(PoseLandmark ls, PoseLandmark le, PoseLandmark lw,
                                   PoseLandmark re, PoseLandmark rw) {

        // Pose 1: Left arm down, right arm out to the side (wrist right of elbow)
        boolean leftArmDown = (
                lw.getPosition().y > le.getPosition().y && // wrist below elbow
                        le.getPosition().y > ls.getPosition().y    // elbow below shoulder
        );

        boolean rightArmSide = (
                rw.getPosition().y < le.getPosition().y && // right wrist above left elbow
                        rw.getPosition().x < re.getPosition().x    // wrist right of elbow
        );

        // For static pose, both conditions must be true at the same time
        return leftArmDown && rightArmSide;
    }

    // Tracking phases
    private boolean normalStopPose1Done = false;
    private boolean normalStopPose2Done = false;

    private boolean detectNormalStop(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                     PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {

        // Pose 1: Both arms down
        boolean leftArmDown = (
                le.getPosition().y > ls.getPosition().y && // elbow below shoulder
                        lw.getPosition().y > le.getPosition().y    // wrist below elbow
        );
        boolean rightArmDown = (
                re.getPosition().y > rs.getPosition().y && // elbow below shoulder
                        rw.getPosition().y > re.getPosition().y    // wrist below elbow
        );

        if (!normalStopPose1Done &&
                leftArmDown &&
                rightArmDown) {

            normalStopPose1Done = true;
        }

        // Pose 2: Both arms up over the head
        boolean leftArmUp = (
                lw.getPosition().y < le.getPosition().y && // wrist above elbow
                        le.getPosition().y < ls.getPosition().y    // elbow above shoulder
        );
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y && // wrist above elbow
                        re.getPosition().y < rs.getPosition().y    // elbow above shoulder
        );

        if (normalStopPose1Done &&
                !normalStopPose2Done &&
                leftArmUp &&
                rightArmUp) {

            normalStopPose2Done = true;
        }

        return normalStopPose1Done && normalStopPose2Done;
    }

    // Tracking phases for Emergency Stop
    private boolean emergencyStopPose1Done = false;
    private boolean emergencyStopPose2Done = false;
    private boolean emergencyStopPose3Done = false;
    private boolean emergencyStopPose4Done = false;

    private boolean detectEmergencyStop(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                        PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {

        // Arms must be stretched upwards above the head (Y-axis check)
        boolean leftArmUp = (
                lw.getPosition().y < le.getPosition().y && // wrist above elbow
                        le.getPosition().y < ls.getPosition().y    // elbow above shoulder
        );
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y && // wrist above elbow
                        re.getPosition().y < rs.getPosition().y    // elbow above shoulder
        );

        // Frame 1: Both wrists positioned LEFT of their respective elbows (X-axis check)
        // This is the first wave position
        if (!emergencyStopPose1Done &&
                leftArmUp &&
                rightArmUp &&
                lw.getPosition().x < le.getPosition().x &&  // left wrist to the left of left elbow
                rw.getPosition().x > re.getPosition().x) {  // right wrist to the left of right elbow

            emergencyStopPose1Done = true;
        }

        // Frame 2: Both wrists positioned RIGHT of their respective elbows
        // Indicates a swing from left to right
        if (emergencyStopPose1Done &&
                !emergencyStopPose2Done &&
                leftArmUp &&
                rightArmUp &&
                lw.getPosition().x > le.getPosition().x &&  // left wrist to the right of left elbow
                rw.getPosition().x < re.getPosition().x) {  // right wrist to the right of right elbow

            emergencyStopPose2Done = true;
        }

        // Frame 3: Both wrists LEFT again (swing back)
        if (emergencyStopPose2Done &&
                !emergencyStopPose3Done &&
                leftArmUp &&
                rightArmUp &&
                lw.getPosition().x < le.getPosition().x &&
                rw.getPosition().x > re.getPosition().x) {

            emergencyStopPose3Done = true;
        }

        // Frame 4: Both wrists RIGHT again (second forward swing)
        if (emergencyStopPose3Done &&
                !emergencyStopPose4Done &&
                leftArmUp &&
                rightArmUp &&
                lw.getPosition().x > le.getPosition().x &&
                rw.getPosition().x < re.getPosition().x) {

            emergencyStopPose4Done = true;
        }

        // Only return true when all 4 waving phases are completed in order
        return emergencyStopPose1Done &&
                emergencyStopPose2Done &&
                emergencyStopPose3Done &&
                emergencyStopPose4Done;
    }

    private boolean detectHoldPosition(
            PoseLandmark ls, PoseLandmark le, PoseLandmark lw, // left shoulder, elbow, wrist
            PoseLandmark rs, PoseLandmark re, PoseLandmark rw  // right shoulder, elbow, wrist
    ) {
        // --- LEFT ARM CHECK (\ shape) ---
        boolean leftArmDown = (
                lw.getPosition().y > le.getPosition().y && // wrist lower than elbow (downward)
                        le.getPosition().y > ls.getPosition().y    // elbow lower than shoulder (downward)
        );

        boolean leftArmDiagonal = (
                lw.getPosition().x > le.getPosition().x && // wrist is further LEFT than elbow
                        le.getPosition().x > ls.getPosition().x    // elbow is further LEFT than shoulder
        );

        // --- RIGHT ARM CHECK (/ shape) ---
        boolean rightArmDown = (
                rw.getPosition().y > re.getPosition().y && // wrist lower than elbow (downward)
                        re.getPosition().y > rs.getPosition().y    // elbow lower than shoulder (downward)
        );

        boolean rightArmDiagonal = (
                rw.getPosition().x < re.getPosition().x && // wrist is further RIGHT than elbow
                        re.getPosition().x < rs.getPosition().x    // elbow is further RIGHT than shoulder
        );

        // Both arms must be in the down-diagonal positions at the same time
        return leftArmDown && leftArmDiagonal && rightArmDown && rightArmDiagonal;
    }

    // Tracking phases for Turn Right with continuous waving
    private boolean turnRightPose1Done = false;
    private boolean turnRightPose2Done = false;
    private boolean turnRightPose3Done = false;
    private boolean turnRightPose4Done = false;
    private long turnRightStartTime = 0;

    private boolean detectTurnRight(
            PoseLandmark lw, PoseLandmark le, PoseLandmark ls, // left wrist, elbow, shoulder
            PoseLandmark rw, PoseLandmark re, PoseLandmark rs  // right wrist, elbow, shoulder
    ) {
        long now = System.currentTimeMillis();

        boolean leftArmRightAngle = (
                ls.getPosition().x < le.getPosition().x && // shoulder X < elbow X
                        le.getPosition().x < lw.getPosition().x    // elbow X < wrist X
        );

        // --- RIGHT ARM: raised up ---
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y    // wrist above elbow
        );

        // --- Frame 1: wrist LEFT of elbow ---
        if (!turnRightPose1Done &&
                leftArmRightAngle &&
                rightArmUp &&
                rw.getPosition().x < re.getPosition().x) {

            turnRightPose1Done = true;
            turnRightStartTime = now;
        }

        // --- Frame 2: wrist RIGHT of elbow ---
        if (turnRightPose1Done &&
                !turnRightPose2Done &&
                (now - turnRightStartTime <= 3000) &&
                leftArmRightAngle &&
                rightArmUp &&
                rw.getPosition().x > re.getPosition().x) {

            turnRightPose2Done = true;
        }

        // --- Frame 3: wrist LEFT of elbow ---
        if (turnRightPose2Done &&
                !turnRightPose3Done &&
                (now - turnRightStartTime <= 3000) &&
                leftArmRightAngle &&
                rightArmUp &&
                rw.getPosition().x < re.getPosition().x) {

            turnRightPose3Done = true;
        }

        // --- Frame 4: wrist RIGHT of elbow ---
        if (turnRightPose3Done &&
                !turnRightPose4Done &&
                (now - turnRightStartTime <= 3000) &&
                leftArmRightAngle &&
                rightArmUp &&
                rw.getPosition().x > re.getPosition().x) {

            turnRightPose4Done = true;
        }

        return turnRightPose1Done && turnRightPose2Done && turnRightPose3Done && turnRightPose4Done;
    }


    private boolean detectChalkInstalled(
            PoseLandmark ls, PoseLandmark le, PoseLandmark lw, // left shoulder, elbow, wrist
            PoseLandmark rs, PoseLandmark re, PoseLandmark rw  // right shoulder, elbow, wrist
    ) {
        // --- Left Arm Up ---
        boolean leftArmUp = (
                lw.getPosition().y < le.getPosition().y && // wrist above elbow
                        le.getPosition().y < ls.getPosition().y    // elbow above shoulder
        );

        // --- Right Arm Up ---
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y && // wrist above elbow
                        re.getPosition().y < rs.getPosition().y    // elbow above shoulder
        );

        // --- Additional X-axis rules ---
        boolean xPositionCheck = (
                rw.getPosition().x < re.getPosition().x && // right wrist left of right elbow
                        lw.getPosition().x > le.getPosition().x    // left wrist right of left elbow
        );

        // --- Static pose detection ---
        return leftArmUp && rightArmUp && xPositionCheck;
    }

    // Tracking phases for Slow Down
    private boolean slowDownPose1Done = false;
    private boolean slowDownPose2Done = false;
    private long slowDownStartTime = 0;

    private boolean detectSlowDown(
            PoseLandmark ls, PoseLandmark le, PoseLandmark lw, // left shoulder, elbow, wrist
            PoseLandmark rs, PoseLandmark re, PoseLandmark rw  // right shoulder, elbow, wrist
    ) {
        long now = System.currentTimeMillis();

        // --- LEFT ARM CHECK (\ shape) ---
        boolean leftArmDown = (
                lw.getPosition().y > le.getPosition().y && // wrist lower than elbow
                        le.getPosition().y > ls.getPosition().y    // elbow lower than shoulder
        );
        boolean leftArmDiagonal = (
                lw.getPosition().x > le.getPosition().x && // wrist further LEFT than elbow
                        le.getPosition().x > ls.getPosition().x    // elbow further LEFT than shoulder
        );

        // --- RIGHT ARM CHECK (/ shape) ---
        boolean rightArmDown = (
                rw.getPosition().y > re.getPosition().y && // wrist lower than elbow
                        re.getPosition().y > rs.getPosition().y    // elbow lower than shoulder
        );
        boolean rightArmDiagonal = (
                rw.getPosition().x < re.getPosition().x && // wrist further RIGHT than elbow
                        re.getPosition().x < rs.getPosition().x    // elbow further RIGHT than shoulder
        );

        // --- Phase 1: arms diagonal down + wrists BELOW elbows ---
        if (!slowDownPose1Done &&
                leftArmDown &&
                rightArmDown &&
                leftArmDiagonal &&
                rightArmDiagonal) {

            slowDownPose1Done = true;
            slowDownStartTime = now;
        }

        // --- Phase 2: arms still diagonal down + wrists ABOVE elbows ---
        boolean wristsAboveElbows = (
                lw.getPosition().y < le.getPosition().y && // left wrist above left elbow
                        rw.getPosition().y < re.getPosition().y    // right wrist above right elbow
        );

        if (slowDownPose1Done &&
                !slowDownPose2Done &&
                (now - slowDownStartTime <= 3000) &&
                leftArmDiagonal &&
                rightArmDiagonal &&
                wristsAboveElbows &&
                // elbows still below shoulders
                (le.getPosition().y > ls.getPosition().y && re.getPosition().y > rs.getPosition().y)) {

            slowDownPose2Done = true;
        }

        return slowDownPose1Done && slowDownPose2Done;
    }

    // Tracking phases for Shut Off Engine
    private boolean shutOffEnginePose1Done = false;
    private boolean shutOffEnginePose2Done = false;
    private long shutOffEngineStartTime = 0;

    private boolean detectShutOffEngine(
            PoseLandmark ls, PoseLandmark le, PoseLandmark lw, // left shoulder, elbow, wrist
            PoseLandmark rs, PoseLandmark re, PoseLandmark rw  // right shoulder, elbow, wrist
    ) {
        long now = System.currentTimeMillis();

        // --- LEFT ARM DOWN ---
        boolean leftArmDown = (
                lw.getPosition().y > le.getPosition().y && // wrist below elbow
                        le.getPosition().y > ls.getPosition().y    // elbow below shoulder
        );

        // --- RIGHT ARM UP ---
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y && // wrist above elbow
                        re.getPosition().y > rs.getPosition().y    // elbow below shoulder
        );

        // --- Phase 1: right wrist to the RIGHT of right elbow ---
        if (!shutOffEnginePose1Done &&
                leftArmDown &&
                rightArmUp &&
                rw.getPosition().x > re.getPosition().x) { // wrist further right than elbow

            shutOffEnginePose1Done = true;
            shutOffEngineStartTime = now;
        }

        // --- Phase 2: right wrist to the LEFT of right elbow ---
        if (shutOffEnginePose1Done &&
                !shutOffEnginePose2Done &&
                (now - shutOffEngineStartTime <= 3000) &&
                leftArmDown &&
                rightArmUp &&
                rw.getPosition().x < re.getPosition().x) { // wrist further left than elbow

            shutOffEnginePose2Done = true;
        }

        return shutOffEnginePose1Done && shutOffEnginePose2Done;
    }

    private boolean detectPassControl(
            PoseLandmark ls, PoseLandmark le, PoseLandmark lw, // left shoulder, elbow, wrist
            PoseLandmark rs, PoseLandmark re, PoseLandmark rw  // right shoulder, elbow, wrist
    ) {
        boolean leftArmRightAngle = (
                ls.getPosition().x < le.getPosition().x && // shoulder X < elbow X
                        le.getPosition().x < lw.getPosition().x    // elbow X < wrist X
        );

        // --- RIGHT ARM: raised up & elbow below shoulder ---
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y && // wrist above elbow
                        re.getPosition().y > rs.getPosition().y &&    // elbow below shoulder
                        rw.getPosition().x < re.getPosition().x
        );

        // For static pose detection, both must be true simultaneously
        return leftArmRightAngle && rightArmUp;
    }

    // Tracking phases for Engine on Fire
    private boolean engineOnFirePose1Done = false;
    private boolean engineOnFirePose2Done = false;
    private boolean engineOnFirePose3Done = false;
    private boolean engineOnFirePose4Done = false;

    private boolean detectEngineOnFire(
            PoseLandmark lw, PoseLandmark le, PoseLandmark ls, // left wrist, elbow, shoulder
            PoseLandmark rw, PoseLandmark re, PoseLandmark rs  // right wrist, elbow, shoulder
    ) {

        // --- LEFT ARM: static angled position ---
        boolean leftArmStatic = (
                le.getPosition().x > ls.getPosition().x && // elbow is left of shoulder
                        lw.getPosition().y < le.getPosition().y && // wrist above elbow
                        le.getPosition().y < ls.getPosition().y && // elbow above shoulder
                        lw.getPosition().x > le.getPosition().x    // wrist right of elbow
        );

        // --- RIGHT ARM: up position ---
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y && // wrist above elbow
                        re.getPosition().y > rs.getPosition().y && // elbow below shoulder
                        rw.getPosition().x < re.getPosition().x    // wrist left of elbow (X-axis)
        );

        // --- RIGHT ARM: down position ---
        boolean rightArmDown = (
                rw.getPosition().y > re.getPosition().y && // wrist below elbow
                        re.getPosition().y > rs.getPosition().y && // elbow below shoulder
                        rw.getPosition().x < re.getPosition().x    // wrist left of elbow (X-axis)
        );

        // Frame 1: Left arm static + Right arm up
        if (!engineOnFirePose1Done &&
                leftArmStatic &&
                rightArmUp) {
            engineOnFirePose1Done = true;
        }

        // Frame 2: Left arm static + Right arm down
        if (engineOnFirePose1Done &&
                !engineOnFirePose2Done &&
                leftArmStatic &&
                rightArmDown) {
            engineOnFirePose2Done = true;
        }

        // Frame 3: Left arm static + Right arm up again
        if (engineOnFirePose2Done &&
                !engineOnFirePose3Done &&
                leftArmStatic &&
                rightArmUp) {
            engineOnFirePose3Done = true;
        }

        // Frame 4: Left arm static + Right arm down again
        if (engineOnFirePose3Done &&
                !engineOnFirePose4Done &&
                leftArmStatic &&
                rightArmDown) {
            engineOnFirePose4Done = true;
        }

        // Return true only when all 4 fanning phases are complete
        return engineOnFirePose1Done &&
                engineOnFirePose2Done &&
                engineOnFirePose3Done &&
                engineOnFirePose4Done;
    }

    // Tracking phases for Brakes on Fire
    private boolean brakesOnFirePose1Done = false;
    private boolean brakesOnFirePose2Done = false;
    private boolean brakesOnFirePose3Done = false;
    private boolean brakesOnFirePose4Done = false;

    private boolean detectBrakesOnFire(
            PoseLandmark lw, PoseLandmark le, PoseLandmark ls, // left wrist, elbow, shoulder
            PoseLandmark rw, PoseLandmark re, PoseLandmark rs  // right wrist, elbow, shoulder
    ) {

        // --- LEFT ARM: down & diagonal ---
        boolean leftArmDown = (
                lw.getPosition().y > le.getPosition().y && // wrist lower than elbow
                        le.getPosition().y > ls.getPosition().y    // elbow lower than shoulder
        );

        boolean leftArmDiagonal = (
                lw.getPosition().x > le.getPosition().x && // wrist further Right than elbow
                        le.getPosition().x > ls.getPosition().x    // elbow further Right than shoulder
        );

        // --- RIGHT ARM: up position ---
        boolean rightArmUp = (
                rw.getPosition().y < re.getPosition().y && // wrist above elbow
                        re.getPosition().y > rs.getPosition().y && // elbow below shoulder
                        rw.getPosition().x < re.getPosition().x    // wrist left of elbow (X-axis)
        );

        // --- RIGHT ARM: down position ---
        boolean rightArmDown = (
                rw.getPosition().y > re.getPosition().y && // wrist below elbow
                        re.getPosition().y > rs.getPosition().y && // elbow below shoulder
                        rw.getPosition().x < re.getPosition().x    // wrist left of elbow (X-axis)
        );

        // Frame 1: Left arm down+diagonal + Right arm up
        if (!brakesOnFirePose1Done &&
                leftArmDown && leftArmDiagonal &&
                rightArmUp) {
            brakesOnFirePose1Done = true;
        }

        // Frame 2: Left arm down+diagonal + Right arm down
        if (brakesOnFirePose1Done &&
                !brakesOnFirePose2Done &&
                leftArmDown && leftArmDiagonal &&
                rightArmDown) {
            brakesOnFirePose2Done = true;
        }

        // Frame 3: Left arm down+diagonal + Right arm up again
        if (brakesOnFirePose2Done &&
                !brakesOnFirePose3Done &&
                leftArmDown && leftArmDiagonal &&
                rightArmUp) {
            brakesOnFirePose3Done = true;
        }

        // Frame 4: Left arm down+diagonal + Right arm down again
        if (brakesOnFirePose3Done &&
                !brakesOnFirePose4Done &&
                leftArmDown && leftArmDiagonal &&
                rightArmDown) {
            brakesOnFirePose4Done = true;
        }

        // Return true only when all 4 phases are complete
        return brakesOnFirePose1Done &&
                brakesOnFirePose2Done &&
                brakesOnFirePose3Done &&
                brakesOnFirePose4Done;
    }





    private void resetGestureTracking() {
        startEnginePose1Done = false;
        startEnginePose2Done = false;
        startEngineStartTime = 0;
        normalStopPose1Done = false;
        normalStopPose2Done = false;
        emergencyStopPose1Done = false;
        emergencyStopPose2Done = false;
        emergencyStopPose3Done = false;
        emergencyStopPose4Done = false;
        turnRightPose1Done = false;
        turnRightPose2Done = false;
        turnRightPose3Done = false;
        turnRightPose4Done = false;
        turnRightStartTime = 0;
        slowDownPose1Done = false;
        slowDownPose2Done = false;
        slowDownStartTime = 0;
        shutOffEnginePose1Done = false;
        shutOffEnginePose2Done = false;
        shutOffEngineStartTime = 0;
        engineOnFirePose1Done = false;
        engineOnFirePose2Done = false;
        engineOnFirePose3Done = false;
        engineOnFirePose4Done = false;
        brakesOnFirePose1Done = false;
        brakesOnFirePose2Done = false;
        brakesOnFirePose3Done = false;
        brakesOnFirePose4Done = false;
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