package com.example.aircraftmarshalling.core;

import android.content.Context;
import android.graphics.PointF;
import android.widget.TextView;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import java.util.HashMap;
import java.util.Map;

public class PoseDetectionManager {
    private final Context context;
    private final TextView poseStatusText;
    private final PoseOverlayView poseOverlayView;
    private final FilamentManager filamentManager;
    private final PoseDetector poseDetector;

    // --- State variables ---
    private boolean isDetectingAction = false;
    private boolean isCooldown = false;
    private long phaseStartTime = 0;
    private String lastDetectionResult = "";

    // --- Tracking phases for all gestures ---
    private boolean startEnginePose1Done = false;
    private boolean startEnginePose2Done = false;
    private long startEngineStartTime = 0;
    private boolean startEnginePose1Done2 = false;
    private boolean startEnginePose2Done2 = false;
    private long startEngineStartTime2 = 0;
    private boolean normalStopPose1Done = false;
    private boolean normalStopPose2Done = false;
    private boolean emergencyStopPose1Done = false;
    private boolean emergencyStopPose2Done = false;
    private boolean emergencyStopPose3Done = false;
    private boolean emergencyStopPose4Done = false;
    private boolean turnRightPose1Done = false;
    private boolean turnRightPose2Done = false;
    private boolean turnRightPose3Done = false;
    private boolean turnRightPose4Done = false;
    private long turnRightStartTime = 0;
    private boolean slowDownPose1Done = false;
    private boolean slowDownPose2Done = false;
    private long slowDownStartTime = 0;
    private boolean shutOffEnginePose1Done = false;
    private boolean shutOffEnginePose2Done = false;
    private long shutOffEngineStartTime = 0;
    private boolean passControlPose1Done = false;
    private boolean passControlPose2Done = false;
    private boolean passControlPose3Done = false;
    private boolean passControlPose4Done = false;
    private boolean passControlPose5Done = false;
    private boolean passControlPose6Done = false;
    private long passControlStartTime = 0;
    private boolean passControlPose1Done2 = false;
    private boolean passControlPose2Done2 = false;
    private boolean passControlPose3Done2 = false;
    private boolean passControlPose4Done2 = false;
    private boolean passControlPose5Done2 = false;
    private boolean passControlPose6Done2 = false;
    private long passControlStartTime2 = 0;
    private boolean engineOnFirePose1Done = false;
    private boolean engineOnFirePose2Done = false;
    private boolean engineOnFirePose3Done = false;
    private boolean engineOnFirePose4Done = false;
    private boolean engineOnFirePose1Done2 = false;
    private boolean engineOnFirePose2Done2 = false;
    private boolean engineOnFirePose3Done2 = false;
    private boolean engineOnFirePose4Done2 = false;
    private boolean brakesOnFirePose1Done = false;
    private boolean brakesOnFirePose2Done = false;
    private boolean brakesOnFirePose3Done = false;
    private boolean brakesOnFirePose4Done = false;
    private boolean brakesOnFirePose1Done2 = false;
    private boolean brakesOnFirePose2Done2 = false;
    private boolean brakesOnFirePose3Done2 = false;
    private boolean brakesOnFirePose4Done2 = false;
    private boolean turnLeftPose1Done = false;
    private boolean turnLeftPose2Done = false;
    private boolean turnLeftPose3Done = false;
    private boolean turnLeftPose4Done = false;
    private long turnLeftStartTime = 0;

    public PoseDetectionManager(Context context, TextView poseStatusText, PoseOverlayView poseOverlayView, FilamentManager filamentManager) {
        this.context = context;
        this.poseStatusText = poseStatusText;
        this.poseOverlayView = poseOverlayView;
        this.filamentManager = filamentManager;
        AccuratePoseDetectorOptions options =
                new AccuratePoseDetectorOptions.Builder()
                        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                        .build();
        poseDetector = PoseDetection.getClient(options);
    }

    public void processImageProxy(ImageProxy imageProxy) {
        @androidx.annotation.OptIn(markerClass = ExperimentalGetImage.class)
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> detectAircraftPose(pose, image.getWidth(), image.getHeight()))
                    .addOnFailureListener(e -> android.util.Log.e("PoseDetection", "Detection failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

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

        if (isCooldown) {
            long cooldownElapsed = (currentTime - phaseStartTime) / 1000;
            long remainingCooldown = 7 - cooldownElapsed;
            if (remainingCooldown > 0) {
                runOnUiThread(() -> poseStatusText.setText(lastDetectionResult + " (" + remainingCooldown + ")"));
            } else {
                isCooldown = false;
                isDetectingAction = false;
            }
            return;
        }

        if (!isDetectingAction) {
            isDetectingAction = true;
            phaseStartTime = currentTime;
            resetGestureTracking();
        }

        long elapsed = (currentTime - phaseStartTime) / 1000;
        long remainingDetection = 7 - elapsed;

        if (remainingDetection > 0) {
            runOnUiThread(() -> poseStatusText.setText(
                    "Detecting Action... (" + remainingDetection + ")"
            ));
        }

        // Run all detectors
        boolean normalStop = detectNormalStop(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean passControlL = detectPassControlL(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean passControlR = detectPassControlR(rightWrist, rightElbow, rightShoulder, leftWrist, leftElbow, leftShoulder);
        boolean startEngineL = detectStartEngineL(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean startEngineR = detectStartEngineR(rightWrist, rightElbow, rightShoulder, leftWrist, leftElbow, leftShoulder);
        boolean turnRight = detectTurnRight(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean turnLeft = detectTurnLeft(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean engineFireL = detectEngineOnFireL(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean engineFireR = detectEngineOnFireR(rightWrist, rightElbow, rightShoulder, leftWrist, leftElbow, leftShoulder);
        boolean brakeFireL = detectBrakesOnFireL(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder);
        boolean brakeFireR = detectBrakesOnFireR(rightWrist, rightElbow, rightShoulder, leftWrist, leftElbow, leftShoulder);
        boolean slowDown = detectSlowDown(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean shutOffEngine = detectShutOffEngine(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean negativeSignal = detectNegative(leftShoulder, leftElbow, leftWrist, rightElbow, rightWrist);
        boolean chockInstalled = detectChockInstalled(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean holdPosition = detectHoldPosition(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);

        // End of 7-second detection
        if (elapsed >= 7) {
            if (normalStop) {
                lastDetectionResult = "Normal Stop";
                filamentManager.callMoveRunway(2);
            } else if (passControlL) {
                lastDetectionResult = "Pass Control to Left";
                filamentManager.callMoveRunway(4);
            } else if (passControlR) {
                lastDetectionResult = "Pass Control to Right";
                filamentManager.callMoveRunway(4);
            } else if (startEngineL) {
                lastDetectionResult = "Start Left Engine";
                filamentManager.callMoveRunway(4);
            } else if (startEngineR) {
                lastDetectionResult = "Start Right Engine";
                filamentManager.callMoveRunway(4);
            } else if (turnRight) {
                lastDetectionResult = "Turn Right";
                // filamentManager.turnRight(4000);
            } else if (turnLeft) {
                lastDetectionResult = "Turn Left";
                // filamentManager.turnLeft(3000);
            } else if (engineFireL) {
                lastDetectionResult = "Left Engine on Fire";
                filamentManager.callMoveRunway(4);
            } else if (engineFireR) {
                lastDetectionResult = "Right Engine on Fire";
                filamentManager.callMoveRunway(4);
            } else if (brakeFireL) {
                lastDetectionResult = "Left Brakes on Fire";
                filamentManager.callMoveRunway(4);
            } else if (brakeFireR) {
                lastDetectionResult = "Right Brakes on Fire";
                filamentManager.callMoveRunway(4);
            } else if (slowDown) {
                lastDetectionResult = "Slow Down";
                filamentManager.callMoveRunway(2);
            } else if (shutOffEngine) {
                lastDetectionResult = "Shut Off Engine";
            } else if (negativeSignal) {
                lastDetectionResult = "Negative";
                filamentManager.callMoveRunway(4);
            } else if (chockInstalled) {
                lastDetectionResult = "Chock Installed";
            } else if (holdPosition) {
                lastDetectionResult = "Hold Position";
                filamentManager.callMoveRunway(4);
            } else {
                filamentManager.callMoveRunway(4);
                lastDetectionResult = "Unknown";
            }

            runOnUiThread(() -> poseStatusText.setText(lastDetectionResult + " (5)"));
            isCooldown = true;
            phaseStartTime = currentTime;
        }
    }

    // --- All gesture detection methods ---
    // ...existing code from SimulationPage.java for:
    // detectStartEngineL, detectStartEngineR, detectNegative, detectNormalStop, detectEmergencyStop,
    // detectChockInstalled, detectHoldPosition, detectTurnRight, detectSlowDown, detectShutOffEngine,
    // detectPassControlL, detectPassControlR, detectEngineOnFireL, detectEngineOnFireR,
    // detectBrakesOnFireL, detectBrakesOnFireR, detectTurnLeft

    // For brevity, use comments to indicate unchanged code regions.
    // Copy all gesture detection methods from your original SimulationPage.java here.

    // Example:
    // private boolean detectStartEngineL(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
    //                                   PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
    //     ...existing code...
    // }

    // ...repeat for all detectXXX methods...

    private void resetGestureTracking() {
        // ...existing code from SimulationPage.java resetGestureTracking()...
        startEnginePose1Done = false;
        startEnginePose2Done = false;
        startEngineStartTime = 0;
        startEnginePose1Done2 = false;
        startEnginePose2Done2 = false;
        startEngineStartTime2 = 0;
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
        engineOnFirePose1Done2 = false;
        engineOnFirePose2Done2 = false;
        engineOnFirePose3Done2 = false;
        engineOnFirePose4Done2 = false;
        brakesOnFirePose1Done = false;
        brakesOnFirePose2Done = false;
        brakesOnFirePose3Done = false;
        brakesOnFirePose4Done = false;
        brakesOnFirePose1Done2 = false;
        brakesOnFirePose2Done2 = false;
        brakesOnFirePose3Done2 = false;
        brakesOnFirePose4Done2 = false;
        turnLeftPose1Done = false;
        turnLeftPose2Done = false;
        turnLeftPose3Done = false;
        turnLeftPose4Done = false;
        turnLeftStartTime = 0;
        passControlPose1Done = false;
        passControlPose2Done = false;
        passControlPose3Done = false;
        passControlPose4Done = false;
        passControlPose5Done = false;
        passControlPose6Done = false;
        passControlStartTime = 0;
        passControlPose1Done2 = false;
        passControlPose2Done2 = false;
        passControlPose3Done2 = false;
        passControlPose4Done2 = false;
        passControlPose5Done2 = false;
        passControlPose6Done2 = false;
        passControlStartTime2 = 0;
    }

    private void runOnUiThread(Runnable r) {
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(r);
    }

    // --- Overlay updater (optional, if you want to show skeleton overlay) ---
    // private void updateSkeletonOverlay(int imageWidth, int imageHeight,
    //                                    PoseLandmark lw, PoseLandmark rw,
    //                                    PoseLandmark le, PoseLandmark re,
    //                                    PoseLandmark ls, PoseLandmark rs) {
    //     ...existing code...
    // }
}