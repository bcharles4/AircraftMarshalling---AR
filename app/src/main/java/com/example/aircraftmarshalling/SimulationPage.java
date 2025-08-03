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
import java.util.ArrayDeque;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import org.tensorflow.lite.Interpreter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;




public class SimulationPage extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private PoseOverlayView poseOverlayView;
    private TextView poseStatusText;
    private PoseDetector poseDetector;

    // === Gesture‑tracking buffers ===
    private static final int WINDOW_SIZE = 70;          // ≈1 s at 30 fps
    private static final float MIN_AMPLITUDE_PX = 100;  // tune per device

    private final ArrayDeque<Float> leftYWristHist  = new ArrayDeque<>();
    private final ArrayDeque<Float> rightYWristHist = new ArrayDeque<>();
    private long lastGestureTime = 0;

    private static final int RECORD_DURATION_FRAMES = 120; // ≈ 2 seconds at 30 fps
    private boolean isRecording = false;

    private List<List<Float>> savedGesture;
    private List<List<Float>> recordedSequence = new ArrayList<>();

    private static final long GESTURE_COOLDOWN_MS = 3000; // 3 sec
    private boolean gestureDetected = false;


    private Interpreter tflite;
    private String[] labelNames = {
            "chalk_installed",
            "emergency_stop",
            "turn_left",
            "normal_stop",
            "negative",
            "start_engine",
            "None",
            "go_straight",
            "turn_right",
            "slow_down"
    };

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
        Button recordButton = findViewById(R.id.recordButton);
        recordButton.setOnClickListener(v -> {
            isRecording = true;
            recordedSequence.clear();
            Toast.makeText(this, "Recording started…", Toast.LENGTH_SHORT).show();
        });

        savedGesture = loadGestureFromFile("go_forward");

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

        try {
            tflite = new Interpreter(loadModelFile("pose_action_model.tflite"));
            Log.d("TFLite", "Model loaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load model", Toast.LENGTH_LONG).show();
        }


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
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);

        if (leftWrist != null && rightWrist != null && leftShoulder != null && rightShoulder != null) {

            List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
            if (!landmarks.isEmpty()) {
                List<Float> normalized = normalizePoseFrame(landmarks);

                // Always keep recordedSequence as a sliding window
                if (isRecording) {
                    recordedSequence.add(normalized);
                    if (recordedSequence.size() > RECORD_DURATION_FRAMES) {
                        recordedSequence.remove(0);
                    }
                    if (recordedSequence.size() == RECORD_DURATION_FRAMES) {
                        isRecording = false;
                        Toast.makeText(this, "Recording complete!", Toast.LENGTH_SHORT).show();
                        saveGestureToFile("go_forward");
                        savedGesture = loadGestureFromFile("go_forward");
                        // Don't clear recordedSequence here!
                    }
                } else {
                    // Sliding window for live detection
                    if (recordedSequence.size() == RECORD_DURATION_FRAMES) {
                        recordedSequence.remove(0);
                    }
                    recordedSequence.add(normalized);
                }
            }

            // Real-time gesture detection using TFLite model
            if (!isRecording && recordedSequence.size() == RECORD_DURATION_FRAMES) {
                // Convert recordedSequence to float input tensor
                float[][][] input = new float[1][RECORD_DURATION_FRAMES][recordedSequence.get(0).size()];
                for (int i = 0; i < RECORD_DURATION_FRAMES; i++) {
                    for (int j = 0; j < recordedSequence.get(i).size(); j++) {
                        input[0][i][j] = recordedSequence.get(i).get(j);
                    }
                }

                float[][] output = new float[4][labelNames.length];

                // Run inference
                tflite.run(input, output);

                // Use first row (or average across all 4 if needed)
                float[] probs = output[0];

                // Find the best class
                int bestIndex = 0;
                float bestScore = probs[0];
                for (int i = 1; i < labelNames.length; i++) {
                    if (probs[i] > bestScore) {
                        bestScore = probs[i];
                        bestIndex = i;
                    }
                }

                String detectedAction = labelNames[bestIndex];

                runOnUiThread(() -> {
                    poseStatusText.setText("Detected: " + detectedAction);
                    Toast.makeText(this, "✈️ Action: " + detectedAction, Toast.LENGTH_SHORT).show();
                });

                lastGestureTime = System.currentTimeMillis();
            }

            // Cooldown reset after detection
            if (gestureDetected && System.currentTimeMillis() - lastGestureTime > GESTURE_COOLDOWN_MS) {
                runOnUiThread(() -> poseStatusText.setText("Pose: Unknown"));
                gestureDetected = false;
            }

            // Optional: drawing joints on screen
            int viewWidth = previewView.getWidth();
            int viewHeight = previewView.getHeight();

            Map<String, PointF> posePoints = new HashMap<>();
            posePoints.put("LEFT_WRIST", mapPoint(leftWrist, imageWidth, imageHeight, viewWidth, viewHeight));
            posePoints.put("RIGHT_WRIST", mapPoint(rightWrist, imageWidth, imageHeight, viewWidth, viewHeight));
            posePoints.put("LEFT_SHOULDER", mapPoint(leftShoulder, imageWidth, imageHeight, viewWidth, viewHeight));
            posePoints.put("RIGHT_SHOULDER", mapPoint(rightShoulder, imageWidth, imageHeight, viewWidth, viewHeight));

            runOnUiThread(() -> {
                poseOverlayView.updatePosePoints(posePoints);
                poseStatusText.setVisibility(View.VISIBLE);
            });

            Log.d("POSE_POINTS", "LEFT_WRIST: " + posePoints.get("LEFT_WRIST").toString());
            Log.d("POSE_POINTS", "RIGHT_WRIST: " + posePoints.get("RIGHT_WRIST").toString());
            Log.d("POSE_POINTS", "LEFT_SHOULDER: " + posePoints.get("LEFT_SHOULDER").toString());
            Log.d("POSE_POINTS", "RIGHT_SHOULDER: " + posePoints.get("RIGHT_SHOULDER").toString());

        }
    }


    private static final float SIMILARITY_THRESHOLD = 0.1f; // Tune this

    private float calculateSimilarity(List<List<Float>> a, List<List<Float>> b) {
        if (a.size() != b.size()) return Float.MAX_VALUE;

        float totalDistance = 0f;

        for (int i = 0; i < a.size(); i++) {
            List<Float> frameA = a.get(i);
            List<Float> frameB = b.get(i);

            float frameDist = 0f;
            for (int j = 0; j < frameA.size(); j++) {
                float diff = frameA.get(j) - frameB.get(j);
                frameDist += diff * diff;
            }
            totalDistance += Math.sqrt(frameDist);
        }

        return totalDistance / a.size();  // Average frame-wise distance
    }


    private List<Float> normalizePoseFrame(List<PoseLandmark> landmarks) {
        List<Float> result = new ArrayList<>();

        PoseLandmark leftShoulder = landmarks.get(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = landmarks.get(PoseLandmark.RIGHT_SHOULDER);

        if (leftShoulder == null || rightShoulder == null) return result;

        float refX = (leftShoulder.getPosition().x + rightShoulder.getPosition().x) / 2f;
        float refY = (leftShoulder.getPosition().y + rightShoulder.getPosition().y) / 2f;

        float scale = (float) Math.hypot(
                rightShoulder.getPosition().x - leftShoulder.getPosition().x,
                rightShoulder.getPosition().y - leftShoulder.getPosition().y
        );
        if (scale < 1e-5f) scale = 1f;

        for (PoseLandmark lm : landmarks) {
            float normX = (lm.getPosition().x - refX) / scale;
            float normY = (lm.getPosition().y - refY) / scale;
            result.add(normX);
            result.add(normY);
        }

        return result;
    }


    // Keep a fixed‑length buffer
    private void addToBuffer(ArrayDeque<Float> buf, float value) {
        if (buf.size() == WINDOW_SIZE) buf.removeFirst();
        buf.addLast(value);
    }

    // Very naive: did the wrist travel up‑and‑down (or down‑and‑up) far enough?
    private boolean isGoForward(ArrayDeque<Float> hist) {
        float max = Collections.max(hist);
        float min = Collections.min(hist);
        if (max - min < MIN_AMPLITUDE_PX) return false;      // not big enough swing

        // simple “saw” pattern: first half mostly going up, second half mostly down
        int half = WINDOW_SIZE / 2;
        List<Float> first = new ArrayList<>(hist).subList(0, half);
        List<Float> second = new ArrayList<>(hist).subList(half, WINDOW_SIZE);

        boolean upThenDown =
            first.get(0) - first.get(half-1) < 0 &&   // wrist moved UP
            second.get(0) - second.get(half-1) > 0;   // wrist moved DOWN

        boolean downThenUp = !upThenDown &&          // or opposite direction
            first.get(0) - first.get(half-1) > 0 &&
            second.get(0) - second.get(half-1) < 0;

        return upThenDown || downThenUp;
    }

    private void saveGestureToFile(String label) {
        Gson gson = new Gson();
        String json = gson.toJson(recordedSequence);
        String filename = "gesture_" + label + ".json";

        try (FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes());
            Toast.makeText(this, "Gesture saved: " + filename, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save gesture", Toast.LENGTH_SHORT).show();
        }
    }

    private List<List<Float>> loadGestureFromFile(String label) {
        List<List<Float>> gesture = new ArrayList<>();
        String filename = "gesture_" + label + ".json";

        try (FileInputStream fis = openFileInput(filename)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            Gson gson = new Gson();
            Type type = new TypeToken<List<List<Float>>>() {}.getType();
            gesture = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load gesture", Toast.LENGTH_SHORT).show();
        }

        return gesture;
    }

    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        try (FileInputStream fis = new FileInputStream(getAssets().openFd(modelName).getFileDescriptor());
             FileChannel fileChannel = fis.getChannel()) {
            long startOffset = getAssets().openFd(modelName).getStartOffset();
            long declaredLength = getAssets().openFd(modelName).getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
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
