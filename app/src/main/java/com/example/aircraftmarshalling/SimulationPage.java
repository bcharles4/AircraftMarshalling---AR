package com.example.aircraftmarshalling;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
//import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.filament.Material;
import com.google.android.filament.MaterialInstance;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import com.google.android.filament.Engine;
import com.google.android.filament.View;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.utils.ModelViewer;
import com.google.android.filament.utils.Utils;
import com.google.android.filament.Skybox;
import com.google.android.filament.utils.Float3;
import com.google.android.filament.TransformManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Engine;
import com.google.android.filament.Scene;
import com.google.android.filament.LightManager;
import com.google.android.filament.EntityManager;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.UbershaderProvider;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import android.animation.ObjectAnimator;
import android.widget.ImageView;

import android.view.Choreographer;
import android.view.SurfaceView;
import android.view.TextureView;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.MotionEvent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import android.view.TextureView;

// Android animation
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.animation.AccelerateDecelerateInterpolator;

// Filament
import com.google.android.filament.TransformManager;

// Math
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimulationPage extends AppCompatActivity {

    static {
        Utils.INSTANCE.init();
    }

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private String name, email, phone;
//    private TextureView previewView; // Change from PreviewView to TextureView
    private PoseOverlayView poseOverlayView;
    private androidx.camera.view.PreviewView previewView;
    private TextView poseStatusText;
    private PoseDetector poseDetector;
    private boolean isUsingFrontCamera = false; // default = back camera

    ImageView movableImage;

    private SurfaceView filamentView; // Change from TextureView to SurfaceView
    private Choreographer choreographer;
    private ModelViewer modelViewer;

    // Add this field:
    private MaterialProvider materialProvider;
    private FilamentAsset runwayAsset;

    // New field to keep the original ByteBuffer for the runway GLB
    private ByteBuffer runwayGlbBuffer;

    // --- Editable runway translation values (set here directly) ---
    // Change these values to move the runway in X, Y, Z
    private static float runwayTranslateX = 0f;
    private static float runwayTranslateY = -0.025f;
    private static float runwayTranslateZ = 0f;

    // --- Infinite slanted runway fields ---
    private static final float RUNWAY_LENGTH = 2.0f; // adjust to match your model's length in Filament units
    private static final float RUNWAY_SPEED = 0.1f; // movement per frame (adjust for speed)
    private static final float RUNWAY_ANGLE_X = -10f; // must match angleX in loadSecondGlb
    private static final float RUNWAY_ANGLE_Y = 90f;  // must match angleY in loadSecondGlb
    private static final float RUNWAY_ANGLE_Z = 0f;
    private static final float RUNWAY_SCALE = 0.001f;
    boolean infiniteRunwayStarted = false;

    int runwayMoved = 0;
    private float airplaneYaw = 0f; // Y-axis rotation in degrees

    // List to keep track of all runway clones (including the original)
    private final List<FilamentAsset> runwayClones = new ArrayList<>();

    private final java.util.List<Integer> fanEntities = new java.util.ArrayList<>();

    // Base LOCAL transforms captured once after load (column-major 4x4)
    private final java.util.Map<Integer, float[]> fanBaseLocal = new java.util.HashMap<>();

    boolean lefEngineStarted = false;
    boolean rightEngineStarted = false;
    private float fanAngle = 0f;
    private float fanSpeed = 0f;  // deg per frame (or scale by dt if you want)

    boolean skellyMode = false;
    private boolean turnedLeft = false;
    private boolean turnedRight = false;
    private boolean isCentered = true;

    private Integer fanEntityLeft = null;
    private Integer fanEntityRight = null;

    private static final float RUNWAY_MOVE_Y = 0.0047f;    // Y movement per frame (tweak as needed)
    private static final float RUNWAY_MOVE_Z = -0.0270f;  // Z movement per frame (tweak as needed)
    private static final float FIRST_CLONE_OFFSET_Y = -0.260f; // tweak as needed
    private static final float FIRST_CLONE_OFFSET_Z = 1.475f;  // tweak as needed
    private static final float CLONE_OFFSET_Y = -0.260f; // tweak as needed
    private static final float CLONE_OFFSET_Z = 1.475f;  // tweak as needed
    private static final float SPAWN_TRIGGER_DISTANCE = 0.5f; // When to spawn next (Z < this)
    private static final float REMOVE_TRIGGER_DISTANCE = -2.0f; // When to remove (Z < this)

    private boolean firstCloneSpawned = false;
    private boolean runwayMoving = false;
    private long runwayMoveStartTime = 0;
    private long runwayMoveDurationMs = 0;

    private FilamentAsset chocksAsset = null;
    boolean isChocked = true;
    private final List<Integer> frontChockEntities = new ArrayList<>();
    private final List<Integer> backChockEntities = new ArrayList<>();
    private final Map<Integer, float[]> chockBaseLocal = new HashMap<>();

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            updateFanRotation();
            updateRunwayAnimation();
            modelViewer.render(frameTimeNanos);
            choreographer.postFrameCallback(this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_simulation_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.camera_page), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        previewView = findViewById(R.id.previewView); // Now a TextureView
        poseOverlayView = findViewById(R.id.poseOverlayView);
        poseStatusText = findViewById(R.id.poseStatusText);
        Button startSimButton = findViewById(R.id.startSim_button);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        Button skellyButton = findViewById(R.id.SkellyButton);

        ImageButton flipButton = findViewById(R.id.flipButton);
        flipButton.setOnClickListener(v -> {
            isUsingFrontCamera = !isUsingFrontCamera; // toggle
            startCamera(); // restart with new selector
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

        filamentView = findViewById(R.id.filamentView); // SurfaceView

        // Make SurfaceView transparent and set Z order
        filamentView.setZOrderOnTop(true);
//        filamentView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        choreographer = Choreographer.getInstance();

        Engine engine = Engine.create();
        UiHelper uiHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
        uiHelper.setOpaque(false);
        modelViewer = new ModelViewer(filamentView, engine, uiHelper, /* manipulator = */ null);

        // No need to create a second MaterialProvider or AssetLoader, use modelViewer's

        makeTransparentBackground();
        loadGlb("3DAirplane");
        loadSecondGlb("LightRunway");
        // createRunwayClone(0, -0.285f, 1.475f);
        addDefaultLights();
        loadGlbThird("Chocks");

        startSimButton.setOnClickListener(v -> {
            startSimButton.setVisibility(android.view.View.GONE);
            poseStatusText.setVisibility(android.view.View.VISIBLE);
            flipButton.setVisibility(android.view.View.VISIBLE);
            poseOverlayView.setVisibility(android.view.View.GONE);
            filamentView.setVisibility(android.view.View.VISIBLE);
            skellyButton.setVisibility(android.view.View.VISIBLE);
        });

        Intent intent2 = getIntent();
        name = intent2.getStringExtra("name");
        email = intent2.getStringExtra("email");
        phone = intent2.getStringExtra("phone");

        bottomNavigationView.setSelectedItemId(R.id.nav_simulation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_simulation) {
                return true;
            } else if (itemId == R.id.nav_module) {

                Intent intent1 = new Intent(SimulationPage.this, AssessmentPage.class);
                intent1.putExtra("name", getIntent().getStringExtra("name"));
                intent1.putExtra("email", getIntent().getStringExtra("email"));
                intent1.putExtra("phone", getIntent().getStringExtra("phone"));
                startActivity(intent1);
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_assessment) {
                Intent intent1 = new Intent(SimulationPage.this, AssessmentPage.class);
                intent1.putExtra("name", getIntent().getStringExtra("name"));
                intent1.putExtra("email", getIntent().getStringExtra("email"));
                intent1.putExtra("phone", getIntent().getStringExtra("phone"));
                startActivity(intent1);
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });

        Button moveButton = findViewById(R.id.MoveButton);
        moveButton.setOnClickListener(v -> {
//            lefEngineStarted = true;
//            rightEngineStarted = true;
//            callMoveRunway(3);
            if (isChocked)
            {
                hideChocks();
                isChocked = false;
            }
            else {
                resetChocks();
                isChocked = true;
            }
        });


        skellyButton.setOnClickListener(v -> {
            if (!skellyMode)
            {
                poseOverlayView.setVisibility(android.view.View.VISIBLE);
                skellyMode = true;
                filamentView.setVisibility(android.view.View.GONE);
            }
            else
            {
                poseOverlayView.setVisibility(android.view.View.GONE);
                skellyMode = false;
                filamentView.setVisibility(android.view.View.VISIBLE);
            }
        });
    }

    // private void callMoveRunway(int times) {
    //     ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    //     final int[] count = {0};
    //     Runnable task = () -> {
    //         moveRunway();
    //         count[0]++;
    //         if (count[0] >= times) {
    //             scheduler.shutdown();
    //         }
    //     };

    //     // Run every 1 ms (minimum realistic delay on Android)
    //     scheduler.scheduleWithFixedDelay(task, 0, 800, TimeUnit.MILLISECONDS);
    // }

    public void callMoveRunway(int seconds) {
        if (!lefEngineStarted || !rightEngineStarted) return;
        if (runwayMoving) return; // Prevent overlapping moves
        hideChocks();
        runwayMoving = true;
        runwayMoveStartTime = System.currentTimeMillis();
        runwayMoveDurationMs = seconds * 1000L;
        firstCloneSpawned = false; // Reset so first clone logic works again
    }

    // Move all runway clones (including original) by -0.01 on Y and 0.1 on Z
    private void moveRunway() {
        if (!lefEngineStarted && !rightEngineStarted) {
            return;
        }

        // Move the original runwayAsset
        if (runwayAsset != null) {
            TransformManager tm = modelViewer.getEngine().getTransformManager();
            int instance = tm.getInstance(runwayAsset.getRoot());
            float[] matrix = new float[16];
            tm.getTransform(instance, matrix);
            matrix[13] += 0.005f;
            matrix[14] -= 0.0288f;
            tm.setTransform(instance, matrix);
        }
        // Move all clones
        for (FilamentAsset clone : runwayClones) {
            TransformManager tm = modelViewer.getEngine().getTransformManager();
            int instance = tm.getInstance(clone.getRoot());
            float[] matrix = new float[16];
            tm.getTransform(instance, matrix);
            matrix[13] += 0.005f;  // Y
            matrix[14] -= 0.0288f;  // Z
            tm.setTransform(instance, matrix);
        }

        runwayMoved += 1;

        if (runwayMoved % 51 == 0) {
            createRunwayClone(0, -0.289f, 1.475f);
        }
    }


    private void updateRunwayAnimation() {
        if (!runwayMoving || !lefEngineStarted || !rightEngineStarted) return;

        long now = System.currentTimeMillis();
        long elapsed = now - runwayMoveStartTime;
        if (elapsed > runwayMoveDurationMs) {
            runwayMoving = false;
            return;
        }

        // Move original runway
        if (runwayAsset != null) {
            moveRunwayAsset(runwayAsset, RUNWAY_MOVE_Y, RUNWAY_MOVE_Z);
        }
        // Move all clones
        for (FilamentAsset clone : runwayClones) {
            moveRunwayAsset(clone, RUNWAY_MOVE_Y, RUNWAY_MOVE_Z);
        }

        // --- SPAWN LOGIC ---
        TransformManager tm = modelViewer.getEngine().getTransformManager();

        // 1. Spawn the first clone based on the original runway asset
        if (!firstCloneSpawned && runwayAsset != null) {
            int inst = tm.getInstance(runwayAsset.getRoot());
            float[] matrix = new float[16];
            tm.getTransform(inst, matrix);
            float z = matrix[14];
            if (z < SPAWN_TRIGGER_DISTANCE) {
                float y = matrix[13] + FIRST_CLONE_OFFSET_Y;
                float zNew = matrix[14] + FIRST_CLONE_OFFSET_Z;
                createRunwayClone(0, y, zNew);
                firstCloneSpawned = true;
            }
        }

        // 2. Spawn subsequent clones based on the last clone
        if (firstCloneSpawned && !runwayClones.isEmpty()) {
            FilamentAsset last = runwayClones.get(runwayClones.size() - 1);
            int inst = tm.getInstance(last.getRoot());
            float[] matrix = new float[16];
            tm.getTransform(inst, matrix);
            float z = matrix[14];
            if (z < SPAWN_TRIGGER_DISTANCE) {
                float y = matrix[13] + CLONE_OFFSET_Y;
                float zNew = matrix[14] + CLONE_OFFSET_Z;
                createRunwayClone(0, y, zNew);
            }
        }

        // Remove runways that are too far away
        // 1. Remove original runway asset if needed
        // if (runwayAsset != null) {
        //     int instOrig = tm.getInstance(runwayAsset.getRoot());
        //     float[] matrixOrig = new float[16];
        //     tm.getTransform(instOrig, matrixOrig);
        //     if (matrixOrig[14] < REMOVE_TRIGGER_DISTANCE) {
        //         modelViewer.getScene().removeEntities(runwayAsset.getEntities());
        //         runwayAsset = null;
        //     }
        // }

        // // 2. Remove clones as before
        // Iterator<FilamentAsset> it = runwayClones.iterator();
        // while (it.hasNext()) {
        //     FilamentAsset clone = it.next();
        //     int inst = tm.getInstance(clone.getRoot());
        //     float[] matrix = new float[16];
        //     tm.getTransform(inst, matrix);
        //     if (matrix[14] < REMOVE_TRIGGER_DISTANCE) {
        //         modelViewer.getScene().removeEntities(clone.getEntities());
        //         it.remove();
        //     }
        // }
    }

    // --- Helper to move a runway asset by deltaY, deltaZ ---
    private void moveRunwayAsset(FilamentAsset asset, float deltaY, float deltaZ) {
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        int inst = tm.getInstance(asset.getRoot());
        float[] matrix = new float[16];
        tm.getTransform(inst, matrix);
        matrix[13] += deltaY; // Y axis
        matrix[14] += deltaZ; // Z axis
        tm.setTransform(inst, matrix);
    }

    // Callable function to create a new runway clone at a given y and z (and x) position
    // The clone will have the same angles and rotation as the original
    public FilamentAsset createRunwayClone(float x, float y, float z) {
        if (runwayGlbBuffer == null) return null;
        AssetLoader assetLoader = new AssetLoader(
                modelViewer.getEngine(),
                new UbershaderProvider(modelViewer.getEngine()),
                EntityManager.get()
        );
        FilamentAsset cloneAsset = assetLoader.createAsset(runwayGlbBuffer.duplicate());
        ResourceLoader resourceLoader = new ResourceLoader(modelViewer.getEngine());
        resourceLoader.loadResources(cloneAsset);

        int root = cloneAsset.getRoot();
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        int instance = tm.getInstance(root);

        // Use the same scale and angles as the original
        float scale = RUNWAY_SCALE;
        float angleX = RUNWAY_ANGLE_X;
        float angleY = RUNWAY_ANGLE_Y;
        float angleZ = RUNWAY_ANGLE_Z;

        float[] matrix = createTransform(scale, angleX, angleY, angleZ);
        matrix[12] = x;
        matrix[13] = y;
        matrix[14] = z;
        tm.setTransform(instance, matrix);

        modelViewer.getScene().addEntities(cloneAsset.getEntities());
        runwayClones.add(cloneAsset);
        return cloneAsset;
    }



    private void updateFanRotation() {
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        
        // Spool-up
        if (fanSpeed < 20f) fanSpeed += 0.2f;
        fanAngle += fanSpeed;
        if (fanAngle >= 360f) fanAngle -= 360f;

        float[] Rspin = makeRotationZColumnMajor((float) Math.toRadians(fanAngle));

        // Left fan
        if (lefEngineStarted && fanEntityLeft != null && tm.hasComponent(fanEntityLeft)) {
            int inst = tm.getInstance(fanEntityLeft);
            float[] base = fanBaseLocal.get(fanEntityLeft);
            if (base != null) {
                float[] localNow = mulCM(base, Rspin);
                tm.setTransform(inst, localNow);
            }
        }
        // Right fan
        if (rightEngineStarted && fanEntityRight != null && tm.hasComponent(fanEntityRight)) {
            int inst = tm.getInstance(fanEntityRight);
            float[] base = fanBaseLocal.get(fanEntityRight);
            if (base != null) {
                float[] localNow = mulCM(base, Rspin);
                tm.setTransform(inst, localNow);
            }
        }
    }

    // Column-major rotation about Z
    private static float[] makeRotationZColumnMajor(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[] {
                // col 0
                c,  s,  0,  0,
                // col 1
                -s,  c,  0,  0,
                // col 2
                0,  0,  1,  0,
                // col 3 (translation)
                0,  0,  0,  1
        };
    }

    private static float[] makeRotationYColumnMajor(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[] {
                c,  0, -s, 0,
                0,  1,  0, 0,
                s,  0,  c, 0,
                0,  0,  0, 1
        };
    }

    private static float[] makeRotationXColumnMajor(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        return new float[] {
                1,  0,  0, 0,
                0,  c,  s, 0,
                0, -s,  c, 0,
                0,  0,  0, 1
        };
    }

    // out = a * b  (all column-major)
    private static float[] mulCM(float[] a, float[] b) {
        float[] o = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                o[col*4 + row] =
                        a[0*4 + row] * b[col*4 + 0] +
                                a[1*4 + row] * b[col*4 + 1] +
                                a[2*4 + row] * b[col*4 + 2] +
                                a[3*4 + row] * b[col*4 + 3];
            }
        }
        return o;
    }





    // Update loadGlb to allow airplane rotation (copy logic from loadSecondGlb)
    private void loadGlb(String name) {
        ByteBuffer buffer = readAsset("models/" + name + ".glb");
        modelViewer.loadModelGlb(buffer);

        ResourceLoader resourceLoader = new ResourceLoader(modelViewer.getEngine());
        resourceLoader.loadResources(modelViewer.getAsset());

        // Debug logs
        FilamentUtils.logAssetHierarchy(modelViewer.getAsset());
        FilamentUtils.logAllEntities(modelViewer.getAsset());
        FilamentUtils.logAllEntitiesWithMaterials(modelViewer.getAsset());

        // --- collect the blades you want to spin ---
        fanEntities.clear();
        fanBaseLocal.clear();
        fanEntityLeft = null;
        fanEntityRight = null;

        TransformManager tm = modelViewer.getEngine().getTransformManager();

        int[] entities = modelViewer.getAsset().getEntities();
        for (int e : entities) {
            String n = modelViewer.getAsset().getName(e);
            if (n == null) continue;

            if ("FanBlades_Left".equals(n) || "Fans.002".equals(n)) {
                fanEntityLeft = e;
                int inst = tm.getInstance(e);
                float[] base = new float[16];
                tm.getTransform(inst, base);
                fanBaseLocal.put(e, base);
                android.util.Log.d("Fans", "Found LEFT: " + n + " id=" + e);
            } else if ("FanBlades_Right".equals(n) || "Fans.003".equals(n)) {
                fanEntityRight = e;
                int inst = tm.getInstance(e);
                float[] base = new float[16];
                tm.getTransform(inst, base);
                fanBaseLocal.put(e, base);
                android.util.Log.d("Fans", "Found RIGHT: " + n + " id=" + e);
            }
        }

        // Scale and rotate airplane model (same logic as runway)
        int root = modelViewer.getAsset().getRoot();
        int instance = tm.getInstance(root);

        float scale = 0.8f;
        float angleX = -5f;
        float angleY = 0f;
        float angleZ = 0f;

        float[] matrix = createTransform(scale, angleX, angleY, angleZ);
        tm.setTransform(instance, matrix);

        modelViewer.getScene().setSkybox(null);
    }


    // New method to load the second GLB model
    private void loadSecondGlb(String name) {
        // Check if asset exists before loading
        boolean assetExists = false;
        try {
            String[] assetList = getAssets().list("models");
            if (assetList != null) {
                for (String asset : assetList) {
                    if (asset.equals(name + ".glb")) {
                        assetExists = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            Log.e("SimulationPage", "Error listing assets", e);
        }
        if (!assetExists) {
            Log.w("SimulationPage", "Asset models/" + name + ".glb not found. Skipping loadSecondGlb.");
            return;
        }

        ByteBuffer buffer = readAsset("models/" + name + ".glb");
        runwayGlbBuffer = buffer.duplicate();

        MaterialProvider provider = new UbershaderProvider(modelViewer.getEngine());
        AssetLoader assetLoader = new AssetLoader(
                modelViewer.getEngine(),
                provider,
                EntityManager.get()
        );
        runwayAsset = assetLoader.createAsset(buffer);

        ResourceLoader resourceLoader = new ResourceLoader(modelViewer.getEngine());
        resourceLoader.loadResources(runwayAsset);

        int root = runwayAsset.getRoot();
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        int instance = tm.getInstance(root);

        float scale = 0.001f;
        float angleX = -10f;
        float angleY = 90f;
        float angleZ = 0f;

        float[] matrix = createTransform(scale, angleX, angleY, angleZ);
        matrix[12] = runwayTranslateX;
        matrix[13] = runwayTranslateY;
        matrix[14] = runwayTranslateZ;
        tm.setTransform(instance, matrix);

        modelViewer.getScene().addEntities(runwayAsset.getEntities());
        // No cloning, no list, no startInfiniteRunway
    }

    private void loadGlbThird(String name) {
        ByteBuffer buffer = readAsset("models/" + name + ".glb");
        MaterialProvider provider = new UbershaderProvider(modelViewer.getEngine());
        AssetLoader assetLoader = new AssetLoader(
                modelViewer.getEngine(),
                provider,
                EntityManager.get()
        );
        chocksAsset = assetLoader.createAsset(buffer);

        ResourceLoader resourceLoader = new ResourceLoader(modelViewer.getEngine());
        resourceLoader.loadResources(chocksAsset);

        // Debug logs
        FilamentUtils.logAssetHierarchy(chocksAsset);
        FilamentUtils.logAllEntities(chocksAsset);
        FilamentUtils.logAllEntitiesWithMaterials(chocksAsset);

        // --- Collect the chocks you want to manipulate ---
        frontChockEntities.clear();
        backChockEntities.clear();
        chockBaseLocal.clear();

        TransformManager tm = modelViewer.getEngine().getTransformManager();
        int[] entities = chocksAsset.getEntities();

        for (int e : entities) {
            String n = chocksAsset.getName(e);
            if (n == null) continue;

            if (n.contains("Front")) {
                frontChockEntities.add(e);
                int inst = tm.getInstance(e);
                float[] base = new float[16];
                tm.getTransform(inst, base);
                chockBaseLocal.put(e, base);
                android.util.Log.d("Chocks", "Found FRONT chock: " + n + " id=" + e);
            } else if (n.contains("Back")) {
                backChockEntities.add(e);
                int inst = tm.getInstance(e);
                float[] base = new float[16];
                tm.getTransform(inst, base);
                chockBaseLocal.put(e, base);
                android.util.Log.d("Chocks", "Found BACK chock: " + n + " id=" + e);
            }
        }

        // Apply transform to root
        int root = chocksAsset.getRoot();
        int instance = tm.getInstance(root);

        float scale = 0.8f;
        float angleX = -5f;
        float angleY = 0f;
        float angleZ = 0f;

        float[] matrix = createTransform(scale, angleX, angleY, angleZ);
        tm.setTransform(instance, matrix);

        modelViewer.getScene().addEntities(chocksAsset.getEntities());
    }

    private ByteBuffer readAsset(String assetName) {
        try (InputStream input = getAssets().open(assetName)) {
            byte[] bytes = new byte[input.available()];
            int read = input.read(bytes);
            Log.d("SimulationPage", "Loaded asset " + assetName + ", bytes: " + bytes.length);
            if (read != bytes.length) {
                throw new IOException("Could not read full asset: " + assetName);
            }
            return ByteBuffer.wrap(bytes);
        } catch (IOException e) {
            Log.e("SimulationPage", "Error reading asset " + assetName, e);
            throw new RuntimeException("Error reading asset " + assetName, e);
        }
    }

    // Helper to create transform with scale + rotation (XYZ in degrees)
    private float[] createTransform(float scale, float angleX, float angleY, float angleZ) {
        double radX = toRadians(angleX);
        double radY = toRadians(angleY);
        double radZ = toRadians(angleZ);

        float cx = (float) cos(radX);
        float sx = (float) sin(radX);
        float cy = (float) cos(radY);
        float sy = (float) sin(radY);
        float cz = (float) cos(radZ);
        float sz = (float) sin(radZ);

        // Rotation order: Z * Y * X
        float[] m = new float[16];
        m[0] = cy * cz * scale;
        m[1] = (sx * sy * cz - cx * sz) * scale;
        m[2] = (cx * sy * cz + sx * sz) * scale;
        m[3] = 0;

        m[4] = cy * sz * scale;
        m[5] = (sx * sy * sz + cx * cz) * scale;
        m[6] = (cx * sy * sz - sx * cz) * scale;
        m[7] = 0;

        m[8] = -sy * scale;
        m[9] = sx * cy * scale;
        m[10] = cx * cy * scale;
        m[11] = 0;

        m[12] = 0;
        m[13] = 0;
        m[14] = 0;
        m[15] = 1;

        return m;
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Ensure rendering starts when activity resumes
        choreographer.postFrameCallback(frameCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ensure rendering stops when activity pauses
        choreographer.removeFrameCallback(frameCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        choreographer.removeFrameCallback(frameCallback);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                cameraProvider.unbindAll();

                CameraSelector selector;

                if (isUsingFrontCamera) {
                    selector = CameraSelector.DEFAULT_FRONT_CAMERA;
                } else {
                    selector = CameraSelector.DEFAULT_BACK_CAMERA;
                }

                Preview preview = new Preview.Builder().build();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::processImageProxy);

                // Use TextureView for CameraX preview
//                preview.setSurfaceProvider(request -> {
//                    Surface surface = new Surface(previewView.getSurfaceTexture());
//                    request.provideSurface(surface, ContextCompat.getMainExecutor(this), result -> {
//                        surface.release();
//                    });
//                });
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("SimulationPage", "Camera start failed", e);
            } catch (IllegalArgumentException iae) {
                Log.e("SimulationPage", "No camera matched selector", iae);
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private void processImageProxy(ImageProxy imageProxy) {
        @OptIn(markerClass = ExperimentalGetImage.class)
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
            long remainingCooldown = 7 - cooldownElapsed;

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
        long remainingDetection = 7 - elapsed;

        if (remainingDetection > 0) {
            runOnUiThread(() -> poseStatusText.setText(
                    "Detecting Action... (" + remainingDetection + ")"
            ));
        }

        // Run all detectors
        boolean normalStop = detectNormalStop(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder); // ✅ New detector
//        boolean emergencyStop = detectEmergencyStop(leftWrist, leftElbow, leftShoulder, rightWrist, rightElbow, rightShoulder); // ✅ New detector

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
            if (chockInstalled) {
                lastDetectionResult = "Chock Installed";
                resetChocks();
            }
            else if (normalStop) { //
                lastDetectionResult = "Normal Stop";
//                callMoveRunway(2);
                lefEngineStarted = false;
                rightEngineStarted = false;
            }
//            else if (emergencyStop) { //
//                lastDetectionResult = "Emergency Stop";
//                engineStarted = false;
//            }
            else if (passControlL) {
                lastDetectionResult = "Pass Control to Left";
//                callMoveRunway(4);
            }
            else if (passControlR) {
                lastDetectionResult = "Pass Control to Right";
//                callMoveRunway(4);
            }

            else if (startEngineL) {
                lastDetectionResult = "Start Left Engine";
                lefEngineStarted = true;
//                callMoveRunway(4);
            }
            else if (startEngineR) {
                lastDetectionResult = "Start Right Engine";
                rightEngineStarted = true;
//                callMoveRunway(4);
            }
            else if (turnRight) {
                lastDetectionResult = "Turn Right";
                if (lefEngineStarted && rightEngineStarted && (isCentered || turnedLeft) && !turnedRight) {
                    turnRight(4000);
                    turnedRight = true;
                    turnedLeft = false;
                    isCentered = false; // moved away from center
                }
            } else if (turnLeft) {
                lastDetectionResult = "Turn Left";
                if (lefEngineStarted && rightEngineStarted && (isCentered || turnedRight) && !turnedLeft) {
                    turnLeft(4000);
                    turnedLeft = true;
                    turnedRight = false;
                    isCentered = false; // moved away from center
                }
            }

            else if (engineFireL) {
                lastDetectionResult = "Left Engine on Fire";
                callMoveRunway(3);
            }
            else if (engineFireR) {
                lastDetectionResult = "Right Engine on Fire";
                callMoveRunway(3);
            }
            else if (brakeFireL) {
                lastDetectionResult = "Left Brakes on Fire";
                callMoveRunway(3);
            }
            else if (brakeFireR) {
                lastDetectionResult = "Right Brakes on Fire";
                callMoveRunway(3);
            }
            else if (slowDown) {
                lastDetectionResult = "Slow Down";
                callMoveRunway(1);
            }
            else if (shutOffEngine) {
                lastDetectionResult = "Shut Off Engine";
                lefEngineStarted = false;
                rightEngineStarted = false;
            } else if (negativeSignal) {
                lastDetectionResult = "Negative";
                callMoveRunway(3);
            }
            else if (holdPosition) {
                lastDetectionResult = "Hold Position";
                callMoveRunway(3);
            } else {
                lastDetectionResult = "Unknown";
                callMoveRunway(3);
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

    private boolean detectStartEngineL(PoseLandmark lw, PoseLandmark le, PoseLandmark ls,
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

    // Tracking phases (mirrored version)
    private boolean startEnginePose1Done2 = false;
    private boolean startEnginePose2Done2 = false;
    private long startEngineStartTime2 = 0;

    private boolean detectStartEngineR(PoseLandmark rw, PoseLandmark re, PoseLandmark rs,
                                        PoseLandmark lw, PoseLandmark le, PoseLandmark ls) {

        long now = System.currentTimeMillis();

        // Pose 1: right arm static "\" + left arm up, wrist right of elbow
        boolean rightArmStatic2 = (
                re.getPosition().x < rs.getPosition().x && // elbow is right of shoulder
                        rw.getPosition().y < re.getPosition().y &&
                        re.getPosition().y < rs.getPosition().y && // wrist above elbow
                        rw.getPosition().x < re.getPosition().x    // wrist left of elbow
        );
        boolean leftArmUp2 = (
                lw.getPosition().y < le.getPosition().y
        );

        if (!startEnginePose1Done2 &&
                rightArmStatic2 &&
                leftArmUp2 &&
                lw.getPosition().x > le.getPosition().x) {

            startEnginePose1Done2 = true;
            startEngineStartTime2 = now;
        }

        // Pose 2: right arm still static + left arm still up, wrist left of elbow
        if (startEnginePose1Done2 &&
                !startEnginePose2Done2 &&
                (now - startEngineStartTime2 <= 3000) && // within 3 seconds
                rightArmStatic2 &&
                leftArmUp2 &&
                lw.getPosition().x < le.getPosition().x) {

            startEnginePose2Done2 = true;
        }

        return startEnginePose1Done2 && startEnginePose2Done2;
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

    private boolean detectChockInstalled(
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
                        re.getPosition().y > rs.getPosition().y && // elbow below shoulder
                        rw.getPosition().x > rs.getPosition().x    // wrist left of elbow (X-axis)
        );

        // --- Phase 1: right wrist to the RIGHT of right elbow ---
        if (!shutOffEnginePose1Done &&
                leftArmDown &&
                rightArmUp &&
                rw.getPosition().x > rs.getPosition().x) { // wrist further right than elbow

            shutOffEnginePose1Done = true;
            shutOffEngineStartTime = now;
        }

        // --- Phase 2: right wrist to the LEFT of right elbow ---
        if (shutOffEnginePose1Done &&
                !shutOffEnginePose2Done &&
                (now - shutOffEngineStartTime <= 3000) &&
                leftArmDown &&
                rw.getPosition().x < rs.getPosition().x) { // wrist further left than elbow

            shutOffEnginePose2Done = true;
        }

        return shutOffEnginePose1Done && shutOffEnginePose2Done;
    }

    // Tracking phases for Pass Control
    private boolean passControlPose1Done = false;
    private boolean passControlPose2Done = false;
    private boolean passControlPose3Done = false;
    private boolean passControlPose4Done = false;
    private boolean passControlPose5Done = false;
    private boolean passControlPose6Done = false;
    private long passControlStartTime = 0;

    private boolean detectPassControlL(
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
        if (!passControlPose1Done &&
                leftArmRightAngle &&
                rightArmUp &&
                rw.getPosition().x < re.getPosition().x) {

            passControlPose1Done = true;
            passControlStartTime = now;
        }

        // --- Frame 2: wrist RIGHT of elbow ---
        if (passControlPose1Done &&
                !passControlPose2Done &&
                (now - passControlStartTime <= 4000) &&
                leftArmRightAngle &&
                rightArmUp &&
                rw.getPosition().x > re.getPosition().x) {

            passControlPose2Done = true;
        }

        // --- Frame 3: wrist LEFT of elbow ---
        if (passControlPose2Done &&
                !passControlPose3Done &&
                (now - passControlStartTime <= 4000) &&
                leftArmRightAngle &&
                rightArmUp &&
                rw.getPosition().x < re.getPosition().x) {

            passControlPose3Done = true;
        }

        // --- Frame 4: wrist RIGHT of elbow ---
        if (passControlPose3Done &&
                !passControlPose4Done &&
                (now - passControlStartTime <= 4000) &&
                leftArmRightAngle &&
                rightArmUp &&
                rw.getPosition().x > re.getPosition().x) {

            passControlPose4Done = true;
        }

        // --- Frame 5: Right arm vertical (shoulder above elbow above wrist) ---
        if (passControlPose4Done &&
                !passControlPose5Done &&
                (now - passControlStartTime <= 5000) &&
                rs.getPosition().y < re.getPosition().y &&
                re.getPosition().y < rw.getPosition().y) {

            passControlPose5Done = true;
        }

        // --- Frame 6: Right wrist passed left shoulder on X axis ---
        if (passControlPose5Done &&
                !passControlPose6Done &&
                (now - passControlStartTime <= 5000) &&
                rw.getPosition().x > ls.getPosition().x) {

            passControlPose6Done = true;
        }

        return passControlPose1Done &&
                passControlPose2Done &&
                passControlPose3Done &&
                passControlPose4Done &&
                passControlPose5Done &&
                passControlPose6Done;
    }

    // Tracking phases for Pass Control (mirrored version)
    private boolean passControlPose1Done2 = false;
    private boolean passControlPose2Done2 = false;
    private boolean passControlPose3Done2 = false;
    private boolean passControlPose4Done2 = false;
    private boolean passControlPose5Done2 = false;
    private boolean passControlPose6Done2 = false;
    private long passControlStartTime2 = 0;

    private boolean detectPassControlR(
            PoseLandmark rw, PoseLandmark re, PoseLandmark rs, // right wrist, elbow, shoulder
            PoseLandmark lw, PoseLandmark le, PoseLandmark ls  // left wrist, elbow, shoulder
    ) {
        long now = System.currentTimeMillis();

        boolean rightArmRightAngle2 = (
                rs.getPosition().x > re.getPosition().x && // shoulder X > elbow X
                        re.getPosition().x > rw.getPosition().x    // elbow X > wrist X
        );

        // --- LEFT ARM: raised up ---
        boolean leftArmUp2 = (
                lw.getPosition().y < le.getPosition().y    // wrist above elbow
        );

        // --- Frame 1: wrist RIGHT of elbow ---
        if (!passControlPose1Done2 &&
                rightArmRightAngle2 &&
                leftArmUp2 &&
                lw.getPosition().x > le.getPosition().x) {

            passControlPose1Done2 = true;
            passControlStartTime2 = now;
        }

        // --- Frame 2: wrist LEFT of elbow ---
        if (passControlPose1Done2 &&
                !passControlPose2Done2 &&
                (now - passControlStartTime2 <= 4000) &&
                rightArmRightAngle2 &&
                leftArmUp2 &&
                lw.getPosition().x < le.getPosition().x) {

            passControlPose2Done2 = true;
        }

        // --- Frame 3: wrist RIGHT of elbow ---
        if (passControlPose2Done2 &&
                !passControlPose3Done2 &&
                (now - passControlStartTime2 <= 4000) &&
                rightArmRightAngle2 &&
                leftArmUp2 &&
                lw.getPosition().x > le.getPosition().x) {

            passControlPose3Done2 = true;
        }

        // --- Frame 4: wrist LEFT of elbow ---
        if (passControlPose3Done2 &&
                !passControlPose4Done2 &&
                (now - passControlStartTime2 <= 4000) &&
                rightArmRightAngle2 &&
                leftArmUp2 &&
                lw.getPosition().x < le.getPosition().x) {

            passControlPose4Done2 = true;
        }

        // --- Frame 5: Left arm vertical (shoulder above elbow above wrist) ---
        if (passControlPose4Done2 &&
                !passControlPose5Done2 &&
                (now - passControlStartTime2 <= 5000) &&
                ls.getPosition().y < le.getPosition().y &&
                le.getPosition().y < lw.getPosition().y) {

            passControlPose5Done2 = true;
        }

        // --- Frame 6: Left wrist passed right shoulder on X axis ---
        if (passControlPose5Done2 &&
                !passControlPose6Done2 &&
                (now - passControlStartTime2 <= 5000) &&
                lw.getPosition().x < rs.getPosition().x) {

            passControlPose6Done2 = true;
        }

        return passControlPose1Done2 &&
                passControlPose2Done2 &&
                passControlPose3Done2 &&
                passControlPose4Done2 &&
                passControlPose5Done2 &&
                passControlPose6Done2;
    }


    // Tracking phases for Engine on Fire
    private boolean engineOnFirePose1Done = false;
    private boolean engineOnFirePose2Done = false;
    private boolean engineOnFirePose3Done = false;
    private boolean engineOnFirePose4Done = false;

    private boolean detectEngineOnFireL(
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
    // Tracking phases for Engine on Fire (mirrored version)
    private boolean engineOnFirePose1Done2 = false;
    private boolean engineOnFirePose2Done2 = false;
    private boolean engineOnFirePose3Done2 = false;
    private boolean engineOnFirePose4Done2 = false;

    private boolean detectEngineOnFireR(
            PoseLandmark rw, PoseLandmark re, PoseLandmark rs, // right wrist, elbow, shoulder
            PoseLandmark lw, PoseLandmark le, PoseLandmark ls  // left wrist, elbow, shoulder
    ) {

        // --- RIGHT ARM: static angled position ---
        boolean rightArmStatic2 = (
                re.getPosition().x < rs.getPosition().x && // elbow is right of shoulder
                        rw.getPosition().y < re.getPosition().y && // wrist above elbow
                        re.getPosition().y < rs.getPosition().y && // elbow above shoulder
                        rw.getPosition().x < re.getPosition().x    // wrist left of elbow
        );

        // --- LEFT ARM: up position ---
        boolean leftArmUp2 = (
                lw.getPosition().y < le.getPosition().y && // wrist above elbow
                        le.getPosition().y > ls.getPosition().y && // elbow below shoulder
                        lw.getPosition().x > le.getPosition().x    // wrist right of elbow (X-axis)
        );

        // --- LEFT ARM: down position ---
        boolean leftArmDown2 = (
                lw.getPosition().y > le.getPosition().y && // wrist below elbow
                        le.getPosition().y > ls.getPosition().y && // elbow below shoulder
                        lw.getPosition().x > le.getPosition().x    // wrist right of elbow (X-axis)
        );

        // Frame 1: Right arm static + Left arm up
        if (!engineOnFirePose1Done2 &&
                rightArmStatic2 &&
                leftArmUp2) {
            engineOnFirePose1Done2 = true;
        }

        // Frame 2: Right arm static + Left arm down
        if (engineOnFirePose1Done2 &&
                !engineOnFirePose2Done2 &&
                rightArmStatic2 &&
                leftArmDown2) {
            engineOnFirePose2Done2 = true;
        }

        // Frame 3: Right arm static + Left arm up again
        if (engineOnFirePose2Done2 &&
                !engineOnFirePose3Done2 &&
                rightArmStatic2 &&
                leftArmUp2) {
            engineOnFirePose3Done2 = true;
        }

        // Frame 4: Right arm static + Left arm down again
        if (engineOnFirePose3Done2 &&
                !engineOnFirePose4Done2 &&
                rightArmStatic2 &&
                leftArmDown2) {
            engineOnFirePose4Done2 = true;
        }

        // Return true only when all 4 fanning phases are complete
        return engineOnFirePose1Done2 &&
                engineOnFirePose2Done2 &&
                engineOnFirePose3Done2 &&
                engineOnFirePose4Done2;
    }




    // Tracking phases for Brakes on Fire
    private boolean brakesOnFirePose1Done = false;
    private boolean brakesOnFirePose2Done = false;
    private boolean brakesOnFirePose3Done = false;
    private boolean brakesOnFirePose4Done = false;

    private boolean detectBrakesOnFireL(
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

    // Tracking phases for Brakes on Fire (mirrored version)
    private boolean brakesOnFirePose1Done2 = false;
    private boolean brakesOnFirePose2Done2 = false;
    private boolean brakesOnFirePose3Done2 = false;
    private boolean brakesOnFirePose4Done2 = false;

    private boolean detectBrakesOnFireR(
            PoseLandmark rw, PoseLandmark re, PoseLandmark rs, // right wrist, elbow, shoulder
            PoseLandmark lw, PoseLandmark le, PoseLandmark ls  // left wrist, elbow, shoulder
    ) {

        // --- RIGHT ARM: down & diagonal ---
        boolean rightArmDown2 = (
                rw.getPosition().y > re.getPosition().y && // wrist lower than elbow
                        re.getPosition().y > rs.getPosition().y    // elbow lower than shoulder
        );

        boolean rightArmDiagonal2 = (
                rw.getPosition().x < re.getPosition().x && // wrist further left than elbow
                        re.getPosition().x < rs.getPosition().x    // elbow further left than shoulder
        );

        // --- LEFT ARM: up position ---
        boolean leftArmUp2 = (
                lw.getPosition().y < le.getPosition().y && // wrist above elbow
                        le.getPosition().y > ls.getPosition().y && // elbow below shoulder
                        lw.getPosition().x > le.getPosition().x    // wrist right of elbow (X-axis)
        );

        // --- LEFT ARM: down position ---
        boolean leftArmDown2 = (
                lw.getPosition().y > le.getPosition().y && // wrist below elbow
                        le.getPosition().y > ls.getPosition().y && // elbow below shoulder
                        lw.getPosition().x > le.getPosition().x    // wrist right of elbow (X-axis)
        );

        // Frame 1: Right arm down+diagonal + Left arm up
        if (!brakesOnFirePose1Done2 &&
                rightArmDown2 && rightArmDiagonal2 &&
                leftArmUp2) {
            brakesOnFirePose1Done2 = true;
        }

        // Frame 2: Right arm down+diagonal + Left arm down
        if (brakesOnFirePose1Done2 &&
                !brakesOnFirePose2Done2 &&
                rightArmDown2 && rightArmDiagonal2 &&
                leftArmDown2) {
            brakesOnFirePose2Done2 = true;
        }

        // Frame 3: Right arm down+diagonal + Left arm up again
        if (brakesOnFirePose2Done2 &&
                !brakesOnFirePose3Done2 &&
                rightArmDown2 && rightArmDiagonal2 &&
                leftArmUp2) {
            brakesOnFirePose3Done2 = true;
        }

        // Frame 4: Right arm down+diagonal + Left arm down again
        if (brakesOnFirePose3Done2 &&
                !brakesOnFirePose4Done2 &&
                rightArmDown2 && rightArmDiagonal2 &&
                leftArmDown2) {
            brakesOnFirePose4Done2 = true;
        }

        // Return true only when all 4 phases are complete
        return brakesOnFirePose1Done2 &&
                brakesOnFirePose2Done2 &&
                brakesOnFirePose3Done2 &&
                brakesOnFirePose4Done2;
    }


    // Tracking phases for Turn Left with continuous waving
    private boolean turnLeftPose1Done = false;
    private boolean turnLeftPose2Done = false;
    private boolean turnLeftPose3Done = false;
    private boolean turnLeftPose4Done = false;
    private long turnLeftStartTime = 0;

    private boolean detectTurnLeft(
            PoseLandmark lw, PoseLandmark le, PoseLandmark ls, // left wrist, elbow, shoulder
            PoseLandmark rw, PoseLandmark re, PoseLandmark rs  // right wrist, elbow, shoulder
    ) {
        long now = System.currentTimeMillis();

        // --- RIGHT ARM: static at right angle (shoulder X < elbow X < wrist X) ---
        boolean rightArmRightAngle = (
                rs.getPosition().x > re.getPosition().x && // shoulder X > elbow X
                        re.getPosition().x > rw.getPosition().x    // elbow X > wrist X
        );

        // --- LEFT ARM: raised up ---
        boolean leftArmUp = (
                lw.getPosition().y < le.getPosition().y    // wrist above elbow
        );

        // --- Frame 1: wrist RIGHT of elbow ---
        if (!turnLeftPose1Done &&
                rightArmRightAngle &&
                leftArmUp &&
                lw.getPosition().x > le.getPosition().x) { // wrist right of elbow

            turnLeftPose1Done = true;
            turnLeftStartTime = now;
        }

        // --- Frame 2: wrist LEFT of elbow ---
        if (turnLeftPose1Done &&
                !turnLeftPose2Done &&
                (now - turnLeftStartTime <= 3000) &&
                rightArmRightAngle &&
                leftArmUp &&
                lw.getPosition().x < le.getPosition().x) { // wrist left of elbow

            turnLeftPose2Done = true;
        }

        // --- Frame 3: wrist RIGHT of elbow ---
        if (turnLeftPose2Done &&
                !turnLeftPose3Done &&
                (now - turnLeftStartTime <= 3000) &&
                rightArmRightAngle &&
                leftArmUp &&
                lw.getPosition().x > le.getPosition().x) { // wrist right of elbow

            turnLeftPose3Done = true;
        }

        // --- Frame 4: wrist LEFT of elbow ---
        if (turnLeftPose3Done &&
                !turnLeftPose4Done &&
                (now - turnLeftStartTime <= 3000) &&
                rightArmRightAngle &&
                leftArmUp &&
                lw.getPosition().x < le.getPosition().x) { // wrist left of elbow

            turnLeftPose4Done = true;
        }

        return turnLeftPose1Done && turnLeftPose2Done && turnLeftPose3Done && turnLeftPose4Done;
    }


    private void resetGestureTracking() {
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


    // --- Overlay updater ---
    private void updateSkeletonOverlay(int imageWidth, int imageHeight,
                                       PoseLandmark lw, PoseLandmark rw,
                                       PoseLandmark le, PoseLandmark re,
                                       PoseLandmark ls, PoseLandmark rs) {
        int viewWidth = previewView.getWidth();
        int viewHeight = previewView.getHeight();
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
        runOnUiThread(() -> poseOverlayView.updatePosePoints(posePoints));
    }


    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void rotateAirplane(float toDegrees) {
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(
                movableImage,
                "rotation",
                movableImage.getRotation(),
                toDegrees
        );
        rotateAnimator.setDuration(2500); // in milliseconds
        rotateAnimator.start();
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

    private void makeTransparentBackground() {
        filamentView.setZOrderOnTop(true);
        filamentView.setBackgroundColor(Color.TRANSPARENT);
        filamentView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        // Set Filament View blend mode to TRANSLUCENT
        modelViewer.getView().setBlendMode(View.BlendMode.TRANSLUCENT);

        // Remove skybox for full transparency
        modelViewer.getScene().setSkybox(null);

        // Set renderer clear options to transparent
        Renderer.ClearOptions options = modelViewer.getRenderer().getClearOptions();
        options.clear = true;
        options.clearColor[0] = 0f;
        options.clearColor[1] = 0f;
        options.clearColor[2] = 0f;
        options.clearColor[3] = 0f; // alpha
        modelViewer.getRenderer().setClearOptions(options);
    }

    private void addDefaultLights() {
        Engine engine = modelViewer.getEngine();
        Scene scene = modelViewer.getScene();

        // === Top-down sunlight (main light) ===
        int sunlight = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(1.0f, 0.90f, 0.9f)       // slightly warm white
                .intensity(350_500.0f)           // strong but not bleaching
                .direction(0.0f, -1.0f, -0.3f)
                .castShadows(false)
                .build(engine, sunlight);
        scene.addEntity(sunlight);

        // === Front light (camera-facing fill) ===
        int frontLight = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(1.0f, .90f, 1.0f)
                .intensity(200_000.0f)
                .direction(0.0f, 0.0f, -1.0f)
                .castShadows(false)
                .build(engine, frontLight);
        scene.addEntity(frontLight);

        // === Bottom light (so underside isn't black) ===
        int bottomLight = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(0.9f, 0.90f, 1.0f)       // cooler tint
                .intensity(350_000.0f)
                .direction(0.0f, 1.0f, 0.0f)    // pointing upward
                .castShadows(false)
                .build(engine, bottomLight);
        scene.addEntity(bottomLight);

        // === Ambient fake IBL (big soft omni fill) ===
        int ambient = EntityManager.get().create();
        new LightManager.Builder(LightManager.Type.POINT)
                .color(1.0f, 1.0f, 1.0f)
                .intensity(250_000.0f)          // global base fill
                .falloff(200.0f)               // make it very broad
                .position(0.0f, 1.5f, 2.0f)    // slightly above/in front
                .castShadows(false)
                .build(engine, ambient);
        scene.addEntity(ambient);
    }

    float currentAngle = 0f; // keep track of the current rotation
    float scale = 0.7f;      // your model’s scale

    private void applyTransform() {
        int root = modelViewer.getAsset().getRoot();
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        int instance = tm.getInstance(root);

        float scale = 0.8f;
        float angleX = -5f; // your base X rotation
        float angleY = airplaneYaw; // dynamic yaw
        float angleZ = 0f;

        float[] matrix = createTransform(scale, angleX, angleY, angleZ);
        tm.setTransform(instance, matrix);
    }

    // Call this to rotate smoothly
    private void rotateBy(final float deltaAngle, int durationMs) {
        final float startYaw = airplaneYaw;
        final float endYaw = startYaw + deltaAngle;

        ValueAnimator animator = ValueAnimator.ofFloat(startYaw, endYaw);
        animator.setDuration(durationMs);
        animator.addUpdateListener(animation -> {
            airplaneYaw = (float) animation.getAnimatedValue();
            applyTransform();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                airplaneYaw = endYaw % 360f;
                applyTransform();
            }
        });
        animator.start();
    }

    // Turn left slightly
    public void turnLeft(int durationMs) {
        rotateBy(-5f, durationMs); // small steer left
    }

    // Turn right slightly
    public void turnRight(int durationMs) {
        rotateBy(5f, durationMs);  // small steer right
    }

    // --- Optional: public setters for moving the runway at runtime ---
    public void setRunwayTranslation(float x, float y, float z) {
        runwayTranslateX = x;
        runwayTranslateY = y;
        runwayTranslateZ = z;
        if (runwayAsset != null) {
            int root = runwayAsset.getRoot();
            TransformManager tm = modelViewer.getEngine().getTransformManager();
            int instance = tm.getInstance(root);
            float scale = 0.001f;
            float angleX = -10f;
            float angleY = 90f;
            float angleZ = 0f;
            float[] matrix = createTransform(scale, angleX, angleY, angleZ);
            matrix[12] = x;
            matrix[13] = y;
            matrix[14] = z;
            tm.setTransform(instance, matrix);
        }
    }

    private static final long CHOCK_ANIM_DURATION = 1000L; // 1 second

    // Animate all front chocks to the far left, back chocks to the far right
    public void hideChocks() {
        if (!isChocked) return;

        isChocked = false;
        TransformManager tm = modelViewer.getEngine().getTransformManager();

        // Animate fronts (to -100 on X)
        for (int e : frontChockEntities) {
            int inst = tm.getInstance(e);
            float[] base = chockBaseLocal.get(e);
            if (base == null) continue;

            float startX = base[12];
            float endX = -1f;

            ValueAnimator animator = ValueAnimator.ofFloat(startX, endX);
            animator.setDuration(CHOCK_ANIM_DURATION);
            animator.addUpdateListener(animation -> {
                float x = (float) animation.getAnimatedValue();
                float[] moved = base.clone();
                moved[12] = x;
                tm.setTransform(inst, moved);
            });
            animator.start();
        }

        // Animate backs (to +100 on X)
        for (int e : backChockEntities) {
            int inst = tm.getInstance(e);
            float[] base = chockBaseLocal.get(e);
            if (base == null) continue;

            float startX = base[12];
            float endX = 1f;

            ValueAnimator animator = ValueAnimator.ofFloat(startX, endX);
            animator.setDuration(CHOCK_ANIM_DURATION);
            animator.addUpdateListener(animation -> {
                float x = (float) animation.getAnimatedValue();
                float[] moved = base.clone();
                moved[12] = x;
                tm.setTransform(inst, moved);
            });
            animator.start();
        }

        android.util.Log.d("Chocks", "Animating chocks hide (front -> left, back -> right)");
    }

    // Animate chocks back to their original transform
    public void resetChocks() {
        if (isChocked) return;

        isChocked = true;
        TransformManager tm = modelViewer.getEngine().getTransformManager();

        // Animate fronts back
        for (int e : frontChockEntities) {
            int inst = tm.getInstance(e);
            float[] base = chockBaseLocal.get(e);
            if (base == null) continue;

            float startX = tm.getTransform(inst, new float[16])[12]; // current x
            float endX = base[12];

            ValueAnimator animator = ValueAnimator.ofFloat(startX, endX);
            animator.setDuration(CHOCK_ANIM_DURATION);
            animator.addUpdateListener(animation -> {
                float x = (float) animation.getAnimatedValue();
                float[] moved = base.clone();
                moved[12] = x;
                tm.setTransform(inst, moved);
            });
            animator.start();
        }

        // Animate backs back
        for (int e : backChockEntities) {
            int inst = tm.getInstance(e);
            float[] base = chockBaseLocal.get(e);
            if (base == null) continue;

            float startX = tm.getTransform(inst, new float[16])[12];
            float endX = base[12];

            ValueAnimator animator = ValueAnimator.ofFloat(startX, endX);
            animator.setDuration(CHOCK_ANIM_DURATION);
            animator.addUpdateListener(animation -> {
                float x = (float) animation.getAnimatedValue();
                float[] moved = base.clone();
                moved[12] = x;
                tm.setTransform(inst, moved);
            });
            animator.start();
        }

        android.util.Log.d("Chocks", "Animating chocks reset to original");
    }


}