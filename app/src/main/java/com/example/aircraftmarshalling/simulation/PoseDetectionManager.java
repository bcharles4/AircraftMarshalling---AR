package com.example.aircraftmarshalling.simulation;

import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.example.aircraftmarshalling.PoseOverlayView; // <-- Add this import
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.HashMap;
import java.util.Map;

public class PoseDetectionManager {
    private final TextView poseStatusText;
    private final PoseOverlayView poseOverlayView;
    private final Runnable onStartEngineL, onStartEngineR, onTurnLeft, onTurnRight, onChockInstalled, onSlowDown, onShutOffEngine, onNegative, onHoldPosition;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // State variables
    private boolean isDetectingAction = false;
    private boolean isCooldown = false;
    private long phaseStartTime = 0;
    private String lastDetectionResult = "";

    // Gesture tracking variables
    private boolean startEnginePose1Done = false, startEnginePose2Done = false;
    private long startEngineStartTime = 0;
    private boolean startEnginePose1Done2 = false, startEnginePose2Done2 = false;
    private long startEngineStartTime2 = 0;
    private boolean normalStopPose1Done = false, normalStopPose2Done = false;
    private boolean emergencyStopPose1Done = false, emergencyStopPose2Done = false, emergencyStopPose3Done = false, emergencyStopPose4Done = false;
    private boolean turnRightPose1Done = false, turnRightPose2Done = false, turnRightPose3Done = false, turnRightPose4Done = false;
    private long turnRightStartTime = 0;
    private boolean slowDownPose1Done = false, slowDownPose2Done = false;
    private long slowDownStartTime = 0;
    private boolean shutOffEnginePose1Done = false, shutOffEnginePose2Done = false;
    private long shutOffEngineStartTime = 0;
    private boolean passControlPose1Done = false, passControlPose2Done = false, passControlPose3Done = false, passControlPose4Done = false, passControlPose5Done = false, passControlPose6Done = false;
    private long passControlStartTime = 0;
    private boolean passControlPose1Done2 = false, passControlPose2Done2 = false, passControlPose3Done2 = false, passControlPose4Done2 = false, passControlPose5Done2 = false, passControlPose6Done2 = false;
    private long passControlStartTime2 = 0;
    private boolean engineOnFirePose1Done = false, engineOnFirePose2Done = false, engineOnFirePose3Done = false, engineOnFirePose4Done = false;
    private boolean engineOnFirePose1Done2 = false, engineOnFirePose2Done2 = false, engineOnFirePose3Done2 = false, engineOnFirePose4Done2 = false;
    private boolean brakesOnFirePose1Done = false, brakesOnFirePose2Done = false, brakesOnFirePose3Done = false, brakesOnFirePose4Done = false;
    private boolean brakesOnFirePose1Done2 = false, brakesOnFirePose2Done2 = false, brakesOnFirePose3Done2 = false, brakesOnFirePose4Done2 = false;
    private boolean turnLeftPose1Done = false, turnLeftPose2Done = false, turnLeftPose3Done = false, turnLeftPose4Done = false;
    private long turnLeftStartTime = 0;

    public PoseDetectionManager(TextView poseStatusText, PoseOverlayView poseOverlayView,
                               Runnable onStartEngineL, Runnable onStartEngineR,
                               Runnable onTurnLeft, Runnable onTurnRight,
                               Runnable onChockInstalled, Runnable onSlowDown,
                               Runnable onShutOffEngine, Runnable onNegative, Runnable onHoldPosition) {
        this.poseStatusText = poseStatusText;
        this.poseOverlayView = poseOverlayView;
        this.onStartEngineL = onStartEngineL;
        this.onStartEngineR = onStartEngineR;
        this.onTurnLeft = onTurnLeft;
        this.onTurnRight = onTurnRight;
        this.onChockInstalled = onChockInstalled;
        this.onSlowDown = onSlowDown;
        this.onShutOffEngine = onShutOffEngine;
        this.onNegative = onNegative;
        this.onHoldPosition = onHoldPosition;
    }

    public void detectAircraftPose(Pose pose, int imageWidth, int imageHeight) {
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
                handler.post(() -> poseStatusText.setText(lastDetectionResult + " (" + remainingCooldown + ")"));
            } else {
                isCooldown = false;
                isDetectingAction = false;
            }
            updateSkeletonOverlay(imageWidth, imageHeight, leftWrist, rightWrist, leftElbow, rightElbow, leftShoulder, rightShoulder);
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
            handler.post(() -> poseStatusText.setText("Detecting Action... (" + remainingDetection + ")"));
        }

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
        // Fix: Pass all 6 arguments to detectNegative
        boolean negativeSignal = detectNegative(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean chockInstalled = detectChockInstalled(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);
        boolean holdPosition = detectHoldPosition(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist);

        if (elapsed >= 7) {
            if (chockInstalled) {
                lastDetectionResult = "Chock Installed";
                handler.post(onChockInstalled);
            } else if (normalStop) {
                lastDetectionResult = "Normal Stop";
            } else if (passControlL) {
                lastDetectionResult = "Pass Control to Left";
            } else if (passControlR) {
                lastDetectionResult = "Pass Control to Right";
            } else if (startEngineL) {
                lastDetectionResult = "Start Left Engine";
                handler.post(onStartEngineL);
            } else if (startEngineR) {
                lastDetectionResult = "Start Right Engine";
                handler.post(onStartEngineR);
            } else if (turnRight) {
                lastDetectionResult = "Turn Right";
                handler.post(onTurnRight);
            } else if (turnLeft) {
                lastDetectionResult = "Turn Left";
                handler.post(onTurnLeft);
            } else if (engineFireL) {
                lastDetectionResult = "Left Engine on Fire";
            } else if (engineFireR) {
                lastDetectionResult = "Right Engine on Fire";
            } else if (brakeFireL) {
                lastDetectionResult = "Left Brakes on Fire";
            } else if (brakeFireR) {
                lastDetectionResult = "Right Brakes on Fire";
            } else if (slowDown) {
                lastDetectionResult = "Slow Down";
                handler.post(onSlowDown);
            } else if (shutOffEngine) {
                lastDetectionResult = "Shut Off Engine";
                handler.post(onShutOffEngine);
            } else if (negativeSignal) {
                lastDetectionResult = "Negative";
                handler.post(onNegative);
            } else if (holdPosition) {
                lastDetectionResult = "Hold Position";
                handler.post(onHoldPosition);
            } else {
                lastDetectionResult = "Unknown";
            }
            handler.post(() -> poseStatusText.setText(lastDetectionResult + " (5)"));
            isCooldown = true;
            phaseStartTime = currentTime;
        }

        updateSkeletonOverlay(imageWidth, imageHeight, leftWrist, rightWrist, leftElbow, rightElbow, leftShoulder, rightShoulder);
    }

    // --- All gesture detection methods (copied from SimulationPage) ---
    private boolean detectStartEngineL(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                       PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y < rs.getPosition().y &&
                le.getPosition().y > ls.getPosition().y && re.getPosition().y > rs.getPosition().y) {
            if (!startEnginePose1Done) {
                startEnginePose1Done = true;
                startEngineStartTime = System.currentTimeMillis();
            }
            return true;
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y > rs.getPosition().y &&
                le.getPosition().y < ls.getPosition().y && re.getPosition().y < rs.getPosition().y) {
            if (startEnginePose1Done && !startEnginePose2Done) {
                startEnginePose2Done = true;
                long duration = (System.currentTimeMillis() - startEngineStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        startEnginePose1Done = false;
        startEnginePose2Done = false;
        return false;
    }

    private boolean detectStartEngineR(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                       PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y < rs.getPosition().y &&
                le.getPosition().y > ls.getPosition().y && re.getPosition().y > rs.getPosition().y) {
            if (!startEnginePose1Done) {
                startEnginePose1Done = true;
                startEngineStartTime = System.currentTimeMillis();
            }
            return true;
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y > rs.getPosition().y &&
                le.getPosition().y < ls.getPosition().y && re.getPosition().y < rs.getPosition().y) {
            if (startEnginePose1Done && !startEnginePose2Done) {
                startEnginePose2Done = true;
                long duration = (System.currentTimeMillis() - startEngineStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        startEnginePose1Done = false;
        startEnginePose2Done = false;
        return false;
    }

    private boolean detectNormalStop(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                     PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y > rs.getPosition().y &&
                le.getPosition().y < ls.getPosition().y && re.getPosition().y < rs.getPosition().y) {
            if (!normalStopPose1Done) {
                normalStopPose1Done = true;
                return false;
            } else {
                normalStopPose2Done = true;
            }
        } else {
            normalStopPose1Done = false;
            normalStopPose2Done = false;
        }
        return normalStopPose1Done && normalStopPose2Done;
    }

    private boolean detectEmergencyStop(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                        PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y > rs.getPosition().y &&
                le.getPosition().y > ls.getPosition().y && re.getPosition().y > rs.getPosition().y) {
            if (!emergencyStopPose1Done) {
                emergencyStopPose1Done = true;
                return false;
            } else {
                emergencyStopPose2Done = true;
            }
        } else if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y < rs.getPosition().y &&
                le.getPosition().y < ls.getPosition().y && re.getPosition().y < rs.getPosition().y) {
            if (emergencyStopPose1Done && !emergencyStopPose2Done) {
                emergencyStopPose2Done = true;
                long duration = (System.currentTimeMillis() - startEngineStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        emergencyStopPose1Done = false;
        emergencyStopPose2Done = false;
        return false;
    }

    private boolean detectChockInstalled(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                         PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        return lw.getPosition().y > ls.getPosition().y && rw.getPosition().y > rs.getPosition().y &&
                le.getPosition().y > ls.getPosition().y && re.getPosition().y > rs.getPosition().y;
    }

    private boolean detectHoldPosition(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                       PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        return lw.getPosition().x == ls.getPosition().x && rw.getPosition().x == rs.getPosition().x &&
                lw.getPosition().y == le.getPosition().y && rw.getPosition().y == re.getPosition().y;
    }

    private boolean detectTurnRight(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                     PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y > rs.getPosition().y) {
            if (!turnRightPose1Done) {
                turnRightPose1Done = true;
                turnRightStartTime = System.currentTimeMillis();
            }
            return true;
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y < rs.getPosition().y) {
            if (turnRightPose1Done && !turnRightPose2Done) {
                turnRightPose2Done = true;
                long duration = (System.currentTimeMillis() - turnRightStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        turnRightPose1Done = false;
        turnRightPose2Done = false;
        return false;
    }

    private boolean detectTurnLeft(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                    PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y < rs.getPosition().y) {
            if (!turnLeftPose1Done) {
                turnLeftPose1Done = true;
                turnLeftStartTime = System.currentTimeMillis();
            }
            return true;
        } else if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y > rs.getPosition().y) {
            if (turnLeftPose1Done && !turnLeftPose2Done) {
                turnLeftPose2Done = true;
                long duration = (System.currentTimeMillis() - turnLeftStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        turnLeftPose1Done = false;
        turnLeftPose2Done = false;
        return false;
    }

    private boolean detectSlowDown(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                    PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y < rs.getPosition().y) {
            if (!slowDownPose1Done) {
                slowDownPose1Done = true;
                slowDownStartTime = System.currentTimeMillis();
            }
            return true;
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y > rs.getPosition().y) {
            if (slowDownPose1Done && !slowDownPose2Done) {
                slowDownPose2Done = true;
                long duration = (System.currentTimeMillis() - slowDownStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        slowDownPose1Done = false;
        slowDownPose2Done = false;
        return false;
    }

    private boolean detectShutOffEngine(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                         PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y < rs.getPosition().y) {
            if (!shutOffEnginePose1Done) {
                shutOffEnginePose1Done = true;
                shutOffEngineStartTime = System.currentTimeMillis();
            }
            return true;
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y > rs.getPosition().y) {
            if (shutOffEnginePose1Done && !shutOffEnginePose2Done) {
                shutOffEnginePose2Done = true;
                long duration = (System.currentTimeMillis() - shutOffEngineStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        shutOffEnginePose1Done = false;
        shutOffEnginePose2Done = false;
        return false;
    }

    private boolean detectPassControlL(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                        PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().x < ls.getPosition().x && rw.getPosition().x < rs.getPosition().x &&
                le.getPosition().y < ls.getPosition().y && re.getPosition().y < rs.getPosition().y) {
            if (!passControlPose1Done) {
                passControlPose1Done = true;
                passControlStartTime = System.currentTimeMillis();
            }
            return true;
        } else if (lw.getPosition().x > ls.getPosition().x && rw.getPosition().x > rs.getPosition().x &&
                le.getPosition().y > ls.getPosition().y && re.getPosition().y > rs.getPosition().y) {
            if (passControlPose1Done && !passControlPose2Done) {
                passControlPose2Done = true;
                long duration = (System.currentTimeMillis() - passControlStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        passControlPose1Done = false;
        passControlPose2Done = false;
        return false;
    }

    private boolean detectPassControlR(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                        PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().x < ls.getPosition().x && rw.getPosition().x < rs.getPosition().x &&
                le.getPosition().y < ls.getPosition().y && re.getPosition().y < rs.getPosition().y) {
            if (!passControlPose1Done) {
                passControlPose1Done = true;
                passControlStartTime = System.currentTimeMillis();
            }
            return true;
        } else if (lw.getPosition().x > ls.getPosition().x && rw.getPosition().x > rs.getPosition().x &&
                le.getPosition().y > ls.getPosition().y && re.getPosition().y > rs.getPosition().y) {
            if (passControlPose1Done && !passControlPose2Done) {
                passControlPose2Done = true;
                long duration = (System.currentTimeMillis() - passControlStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        passControlPose1Done = false;
        passControlPose2Done = false;
        return false;
    }

    private boolean detectEngineOnFireL(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                         PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y > rs.getPosition().y) {
            if (!engineOnFirePose1Done) {
                engineOnFirePose1Done = true;
                return false;
            } else {
                engineOnFirePose2Done = true;
            }
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y < rs.getPosition().y) {
            if (engineOnFirePose1Done && !engineOnFirePose2Done) {
                engineOnFirePose2Done = true;
                long duration = (System.currentTimeMillis() - startEngineStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        engineOnFirePose1Done = false;
        engineOnFirePose2Done = false;
        return false;
    }

    private boolean detectEngineOnFireR(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                         PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y > rs.getPosition().y) {
            if (!engineOnFirePose1Done) {
                engineOnFirePose1Done = true;
                return false;
            } else {
                engineOnFirePose2Done = true;
            }
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y < rs.getPosition().y) {
            if (engineOnFirePose1Done && !engineOnFirePose2Done) {
                engineOnFirePose2Done = true;
                long duration = (System.currentTimeMillis() - startEngineStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        engineOnFirePose1Done = false;
        engineOnFirePose2Done = false;
        return false;
    }

    private boolean detectBrakesOnFireL(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                         PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y > rs.getPosition().y) {
            if (!brakesOnFirePose1Done) {
                brakesOnFirePose1Done = true;
                return false;
            } else {
                brakesOnFirePose2Done = true;
            }
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y < rs.getPosition().y) {
            if (brakesOnFirePose1Done && !brakesOnFirePose2Done) {
                brakesOnFirePose2Done = true;
                long duration = (System.currentTimeMillis() - startEngineStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        brakesOnFirePose1Done = false;
        brakesOnFirePose2Done = false;
        return false;
    }

    private boolean detectBrakesOnFireR(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
                                         PoseLandmark rw, PoseLandmark re, PoseLandmark rs) {
        if (lw.getPosition().y < ls.getPosition().y && rw.getPosition().y > rs.getPosition().y) {
            if (!brakesOnFirePose1Done) {
                brakesOnFirePose1Done = true;
                return false;
            } else {
                brakesOnFirePose2Done = true;
            }
        } else if (lw.getPosition().y > ls.getPosition().y && rw.getPosition().y < rs.getPosition().y) {
            if (brakesOnFirePose1Done && !brakesOnFirePose2Done) {
                brakesOnFirePose2Done = true;
                long duration = (System.currentTimeMillis() - startEngineStartTime) / 1000;
                return duration >= 1 && duration <= 3;
            }
        }
        brakesOnFirePose1Done = false;
        brakesOnFirePose2Done = false;
        return false;
    }

    // Fix: Update detectNegative to accept 6 arguments
    private boolean detectNegative(PoseLandmark ls, PoseLandmark le, PoseLandmark lw,
                                   PoseLandmark rs, PoseLandmark re, PoseLandmark rw) {
        // Example logic, adjust as needed
        return lw.getPosition().x < ls.getPosition().x && rw.getPosition().x < rs.getPosition().x &&
                le.getPosition().y > ls.getPosition().y && re.getPosition().y > rs.getPosition().y;
    }

    // --- Reset all gesture tracking variables ---
    private void resetGestureTracking() {
        startEnginePose1Done = false; startEnginePose2Done = false; startEngineStartTime = 0;
        startEnginePose1Done2 = false; startEnginePose2Done2 = false; startEngineStartTime2 = 0;
        normalStopPose1Done = false; normalStopPose2Done = false;
        emergencyStopPose1Done = false; emergencyStopPose2Done = false; emergencyStopPose3Done = false; emergencyStopPose4Done = false;
        turnRightPose1Done = false; turnRightPose2Done = false; turnRightPose3Done = false; turnRightPose4Done = false; turnRightStartTime = 0;
        slowDownPose1Done = false; slowDownPose2Done = false; slowDownStartTime = 0;
        shutOffEnginePose1Done = false; shutOffEnginePose2Done = false; shutOffEngineStartTime = 0;
        engineOnFirePose1Done = false; engineOnFirePose2Done = false; engineOnFirePose3Done = false; engineOnFirePose4Done = false;
        engineOnFirePose1Done2 = false; engineOnFirePose2Done2 = false; engineOnFirePose3Done2 = false; engineOnFirePose4Done2 = false;
        brakesOnFirePose1Done = false; brakesOnFirePose2Done = false; brakesOnFirePose3Done = false; brakesOnFirePose4Done = false;
        brakesOnFirePose1Done2 = false; brakesOnFirePose2Done2 = false; brakesOnFirePose3Done2 = false; brakesOnFirePose4Done2 = false;
        turnLeftPose1Done = false; turnLeftPose2Done = false; turnLeftPose3Done = false; turnLeftPose4Done = false; turnLeftStartTime = 0;
        passControlPose1Done = false; passControlPose2Done = false; passControlPose3Done = false; passControlPose4Done = false; passControlPose5Done = false; passControlPose6Done = false; passControlStartTime = 0;
        passControlPose1Done2 = false; passControlPose2Done2 = false; passControlPose3Done2 = false; passControlPose4Done2 = false; passControlPose5Done2 = false; passControlPose6Done2 = false; passControlStartTime2 = 0;
    }

    // --- Overlay updater ---
    private void updateSkeletonOverlay(int imageWidth, int imageHeight,
                                      PoseLandmark lw, PoseLandmark rw,
                                      PoseLandmark le, PoseLandmark re,
                                      PoseLandmark ls, PoseLandmark rs) {
        // Fix: Use poseOverlayView's getWidth/getHeight/updatePosePoints
        int viewWidth = poseOverlayView != null ? poseOverlayView.getWidth() : 0;
        int viewHeight = poseOverlayView != null ? poseOverlayView.getHeight() : 0;
        Map<String, PointF> posePoints = new HashMap<>();
        if (lw != null)
            posePoints.put("LEFT_WRIST", mapPoint(lw, imageWidth, imageHeight, viewWidth, viewHeight));
        if (rw != null)
            posePoints.put("RIGHT_WRIST", mapPoint(rw, imageWidth, imageHeight, viewWidth, viewHeight));
        if (le != null)
            posePoints.put("LEFT_ELBOW", mapPoint(le, imageWidth, imageHeight, viewWidth, viewHeight));
        if (re != null)
            posePoints.put("RIGHT_ELBOW", mapPoint(re, imageWidth, imageHeight, viewWidth, viewHeight));
        if (ls != null)
            posePoints.put("LEFT_SHOULDER", mapPoint(ls, imageWidth, imageHeight, viewWidth, viewHeight));
        if (rs != null)
            posePoints.put("RIGHT_SHOULDER", mapPoint(rs, imageWidth, imageHeight, viewWidth, viewHeight));
        if (poseOverlayView != null)
            handler.post(() -> poseOverlayView.updatePosePoints(posePoints));
    }

    private PointF mapPoint(PoseLandmark landmark, int imageWidth, int imageHeight, int viewWidth, int viewHeight) {
        float x = landmark.getPosition().x * ((float) viewWidth / imageWidth);
        float y = landmark.getPosition().y * ((float) viewHeight / imageHeight);
        return new PointF(x, y);
    }
}
