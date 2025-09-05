package com.example.aircraftmarshalling.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.SurfaceView;
import android.view.Choreographer;
import com.google.android.filament.utils.ModelViewer;
import com.google.android.filament.Engine;
import com.google.android.filament.View;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.TransformManager;
import com.google.android.filament.Renderer;
import com.google.android.filament.Scene;
import com.google.android.filament.LightManager;
import com.google.android.filament.EntityManager;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.UbershaderProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FilamentManager {
    private final Context context;
    private final SurfaceView filamentView;
    private final ModelViewer modelViewer;
    private final Choreographer choreographer;
    private MaterialProvider materialProvider;
    private FilamentAsset runwayAsset;
    private ByteBuffer runwayGlbBuffer;
    private static float runwayTranslateX = 0f;
    private static float runwayTranslateY = -0.025f;
    private static float runwayTranslateZ = 0f;
    private static final float RUNWAY_LENGTH = 2.0f;
    private static final float RUNWAY_SPEED = 0.1f;
    private static final float RUNWAY_ANGLE_X = -10f;
    private static final float RUNWAY_ANGLE_Y = 90f;
    private static final float RUNWAY_ANGLE_Z = 0f;
    private static final float RUNWAY_SCALE = 0.001f;
    private boolean engineStarted = false;
    private int runwayMoved = 0;
    private final List<FilamentAsset> runwayClones = new ArrayList<>();
    private final List<Integer> fanEntities = new ArrayList<>();
    private final Map<Integer, float[]> fanBaseLocal = new HashMap<>();
    private boolean engineOn = false;
    private float fanAngle = 0f;
    private float fanSpeed = 0f;
    private float currentAngle = 0f;
    private float scale = 0.7f;

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            updateFanRotation();
            modelViewer.render(frameTimeNanos);
            choreographer.postFrameCallback(this);
        }
    };

    public FilamentManager(Context context, SurfaceView filamentView) {
        this.context = context;
        this.filamentView = filamentView;
        this.choreographer = Choreographer.getInstance();
        Engine engine = Engine.create();
        UiHelper uiHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
        uiHelper.setOpaque(false);
        modelViewer = new ModelViewer(filamentView, engine, uiHelper, null);
        makeTransparentBackground();
        loadGlb("3DAirplane");
        loadSecondGlb("LightRunway");
        createRunwayClone(0, -0.285f, 1.475f);
        addDefaultLights();
    }

    public void onResume() {
        choreographer.postFrameCallback(frameCallback);
    }

    public void onPause() {
        choreographer.removeFrameCallback(frameCallback);
    }

    public void onDestroy() {
        choreographer.removeFrameCallback(frameCallback);
    }

    public void startEngine() {
        engineOn = true;
    }

    public void callMoveRunway(int times) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        final int[] count = {0};
        Runnable task = () -> {
            moveRunway();
            count[0]++;
            if (count[0] >= times) {
                scheduler.shutdown();
            }
        };
        scheduler.scheduleWithFixedDelay(task, 0, 800, TimeUnit.MILLISECONDS);
    }

    private void moveRunway() {
        if (!engineStarted) {
            return;
        }
        if (runwayAsset != null) {
            TransformManager tm = modelViewer.getEngine().getTransformManager();
            int instance = tm.getInstance(runwayAsset.getRoot());
            float[] matrix = new float[16];
            tm.getTransform(instance, matrix);
            matrix[13] += 0.005f;
            matrix[14] -= 0.0288f;
            tm.setTransform(instance, matrix);
        }
        for (FilamentAsset clone : runwayClones) {
            TransformManager tm = modelViewer.getEngine().getTransformManager();
            int instance = tm.getInstance(clone.getRoot());
            float[] matrix = new float[16];
            tm.getTransform(instance, matrix);
            matrix[13] += 0.005f;
            matrix[14] -= 0.0288f;
            tm.setTransform(instance, matrix);
        }
        runwayMoved += 1;
        if (runwayMoved % 51 == 0) {
            createRunwayClone(0, -0.289f, 1.475f);
        }
    }

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
        if (!engineOn || fanEntities.isEmpty()) return;
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        if (fanSpeed < 20f) fanSpeed += 0.2f;
        fanAngle += fanSpeed;
        if (fanAngle >= 360f) fanAngle -= 360f;
        float[] Rspin = TransformUtils.makeRotationZColumnMajor((float) Math.toRadians(fanAngle));
        for (int e : fanEntities) {
            if (!tm.hasComponent(e)) continue;
            int inst = tm.getInstance(e);
            float[] base = fanBaseLocal.get(e);
            if (base == null) continue;
            float[] localNow = TransformUtils.mulCM(base, Rspin);
            tm.setTransform(inst, localNow);
        }
    }

    private void loadGlb(String name) {
        ByteBuffer buffer = readAsset("models/" + name + ".glb");
        modelViewer.loadModelGlb(buffer);
        ResourceLoader resourceLoader = new ResourceLoader(modelViewer.getEngine());
        resourceLoader.loadResources(modelViewer.getAsset());
        fanEntities.clear();
        fanBaseLocal.clear();
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        int[] entities = modelViewer.getAsset().getEntities();
        for (int e : entities) {
            String n = modelViewer.getAsset().getName(e);
            if (n == null) continue;
            if ("FanBlades_Left".equals(n) || "FanBlades_Right".equals(n)
                    || "Fans.002".equals(n) || "Fans.003".equals(n)) {
                fanEntities.add(e);
                int inst = tm.getInstance(e);
                float[] base = new float[16];
                tm.getTransform(inst, base);
                fanBaseLocal.put(e, base);
            }
        }
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

    private void loadSecondGlb(String name) {
        boolean assetExists = false;
        try {
            String[] assetList = context.getAssets().list("models");
            if (assetList != null) {
                for (String asset : assetList) {
                    if (asset.equals(name + ".glb")) {
                        assetExists = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            return;
        }
        if (!assetExists) return;
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
    }

    private ByteBuffer readAsset(String assetName) {
        try (InputStream input = context.getAssets().open(assetName)) {
            byte[] bytes = new byte[input.available()];
            int read = input.read(bytes);
            if (read != bytes.length) {
                throw new IOException("Could not read full asset: " + assetName);
            }
            return ByteBuffer.wrap(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Error reading asset " + assetName, e);
        }
    }

    private float[] createTransform(float scale, float angleX, float angleY, float angleZ) {
        return TransformUtils.createTransform(scale, angleX, angleY, angleZ);
    }

    private void makeTransparentBackground() {
        filamentView.setZOrderOnTop(true);
        filamentView.setBackgroundColor(Color.TRANSPARENT);
        filamentView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        modelViewer.getView().setBlendMode(View.BlendMode.TRANSLUCENT);
        modelViewer.getScene().setSkybox(null);
        Renderer.ClearOptions options = modelViewer.getRenderer().getClearOptions();
        options.clear = true;
        options.clearColor[0] = 0f;
        options.clearColor[1] = 0f;
        options.clearColor[2] = 0f;
        options.clearColor[3] = 0f;
        modelViewer.getRenderer().setClearOptions(options);
    }

    private void addDefaultLights() {
        LightUtils.addDefaultLights(modelViewer.getEngine(), modelViewer.getScene());
    }

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

    // ...add any other public methods needed for gesture/pose actions...
}