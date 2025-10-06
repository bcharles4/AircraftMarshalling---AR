package com.example.aircraftmarshalling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aircraftmarshalling.PoseOverlayView;
import com.example.aircraftmarshalling.simulation.PoseDetectionManager;
import com.example.aircraftmarshalling.simulation.FilamentManager;
import com.example.aircraftmarshalling.simulation.ChockAnimator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SimulationPage2 extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PoseDetectionManager poseDetectionManager;
    private FilamentManager filamentManager;
    private SurfaceView filamentView;
    private PoseOverlayView poseOverlayView;
    private TextView poseStatusText;
    private Button startSimButton, skellyButton, moveButton;
    private ImageButton flipButton;

    private androidx.camera.view.PreviewView previewView;
    private PoseDetector poseDetector;
    private boolean isUsingFrontCamera = false;

    // Chock/engine state
    private boolean isChocked = true;
    private boolean leftEngineStarted = false;
    private boolean rightEngineStarted = false;
    private boolean turnedLeft = false;
    private boolean turnedRight = false;
    private boolean isCentered = true;

    @androidx.camera.core.ExperimentalGetImage
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation_page); // Make sure this layout exists in res/layout

        poseStatusText = findViewById(R.id.poseStatusText);
        poseOverlayView = findViewById(R.id.poseOverlayView);
        filamentView = findViewById(R.id.filamentView);
        startSimButton = findViewById(R.id.startSim_button);
        skellyButton = findViewById(R.id.SkellyButton);
        moveButton = findViewById(R.id.MoveButton);
        flipButton = findViewById(R.id.flipButton);
        previewView = findViewById(R.id.previewView);

        filamentManager = new com.example.aircraftmarshalling.simulation.FilamentManager(this, filamentView);

        poseDetectionManager = new com.example.aircraftmarshalling.simulation.PoseDetectionManager(
            poseStatusText,
            poseOverlayView,
            // onStartEngineL
            () -> {
                leftEngineStarted = true;
                if (leftEngineStarted && rightEngineStarted) {
                    ChockAnimator.hideChocks(filamentManager.modelViewer, getFrontChockEntities(), getBackChockEntities(), getChockBaseLocal());
                    isChocked = false;
                }
            },
            // onStartEngineR
            () -> {
                rightEngineStarted = true;
                if (leftEngineStarted && rightEngineStarted) {
                    ChockAnimator.hideChocks(filamentManager.modelViewer, getFrontChockEntities(), getBackChockEntities(), getChockBaseLocal());
                    isChocked = false;
                }
            },
            // onTurnLeft
            () -> {
                if (leftEngineStarted && rightEngineStarted && (isCentered || turnedRight) && !turnedLeft) {
                    // Rotate airplane left
                    filamentManager.rotateAirplane(-5f, 4000); // rotate left by -5 degrees over 4s
                    turnedLeft = true;
                    turnedRight = false;
                    isCentered = false;
                }
            },
            // onTurnRight
            () -> {
                if (leftEngineStarted && rightEngineStarted && (isCentered || turnedLeft) && !turnedRight) {
                    // Rotate airplane right
                    filamentManager.rotateAirplane(5f, 4000); // rotate right by 5 degrees over 4s
                    turnedRight = true;
                    turnedLeft = false;
                    isCentered = false;
                }
            },
            // onChockInstalled
            () -> {
                ChockAnimator.resetChocks(filamentManager.modelViewer, getFrontChockEntities(), getBackChockEntities(), getChockBaseLocal());
                isChocked = true;
            },
            // onSlowDown
            () -> {
                // Implement runway slow down logic if needed
            },
            // onShutOffEngine
            () -> {
                leftEngineStarted = false;
                rightEngineStarted = false;
            },
            // onNegative
            () -> {
                // Implement negative gesture logic if needed
            },
            // onHoldPosition
            () -> {
                // Implement hold position logic if needed
            }
        );

        startSimButton.setOnClickListener(v -> {
            startSimButton.setVisibility(android.view.View.GONE);
            poseStatusText.setVisibility(android.view.View.VISIBLE);
            flipButton.setVisibility(android.view.View.VISIBLE);
            if (poseOverlayView != null) poseOverlayView.setVisibility(android.view.View.GONE);
            filamentView.setVisibility(android.view.View.VISIBLE);
            skellyButton.setVisibility(android.view.View.VISIBLE);
        });

        moveButton.setOnClickListener(v -> {
            if (isChocked) {
                ChockAnimator.hideChocks(filamentManager.modelViewer, getFrontChockEntities(), getBackChockEntities(), getChockBaseLocal());
                isChocked = false;
            } else {
                ChockAnimator.resetChocks(filamentManager.modelViewer, getFrontChockEntities(), getBackChockEntities(), getChockBaseLocal());
                isChocked = true;
            }
        });

        skellyButton.setOnClickListener(v -> {
            if (poseOverlayView != null) {
                if (poseOverlayView.getVisibility() != android.view.View.VISIBLE) {
                    poseOverlayView.setVisibility(android.view.View.VISIBLE);
                    filamentView.setVisibility(android.view.View.GONE);
                } else {
                    poseOverlayView.setVisibility(android.view.View.GONE);
                    filamentView.setVisibility(android.view.View.VISIBLE);
                }
            }
        });

        flipButton.setOnClickListener(v -> {
            isUsingFrontCamera = !isUsingFrontCamera;
            startCamera();
        });

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
    }

    @androidx.camera.core.ExperimentalGetImage
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                CameraSelector selector = isUsingFrontCamera
                        ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA;

                Preview preview = new Preview.Builder().build();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::processImageProxy);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("SimulationPage2", "Camera start failed", e);
            } catch (IllegalArgumentException iae) {
                Log.e("SimulationPage2", "No camera matched selector", iae);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @ExperimentalGetImage
    private void processImageProxy(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            poseDetector.process(image)
                    .addOnSuccessListener(pose -> {
                        poseDetectionManager.detectAircraftPose(pose, image.getWidth(), image.getHeight());
                    })
                    .addOnFailureListener(e -> Log.e("PoseDetection", "Detection failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    // Helper methods to access chock entities and transforms from FilamentManager
    private List<Integer> getFrontChockEntities() {
        return filamentManager.getFrontChockEntities();
    }
    private List<Integer> getBackChockEntities() {
        return filamentManager.getBackChockEntities();
    }
    private Map<Integer, float[]> getChockBaseLocal() {
        return filamentManager.getChockBaseLocal();
    }

    @androidx.camera.core.ExperimentalGetImage
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(getApplicationContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
