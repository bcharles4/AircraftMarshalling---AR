package com.example.aircraftmarshalling.simulation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.SurfaceView;

import com.google.android.filament.Engine;
import com.google.android.filament.Renderer;
import com.google.android.filament.Scene;
import com.google.android.filament.TransformManager;
import com.google.android.filament.View;
import com.google.android.filament.android.UiHelper;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderProvider;
import com.google.android.filament.utils.ModelViewer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilamentManager {
    public final ModelViewer modelViewer;
    private final SurfaceView filamentView;

    // Assets
    private FilamentAsset airplaneAsset;
    private FilamentAsset runwayAsset;
    private FilamentAsset chocksAsset;
    private ByteBuffer runwayGlbBuffer;

    // Chock entity lists and base transforms
    private final List<Integer> frontChockEntities = new ArrayList<>();
    private final List<Integer> backChockEntities = new ArrayList<>();
    private final Map<Integer, float[]> chockBaseLocal = new HashMap<>();

    private float airplaneYaw = 0f;

    static {
        System.loadLibrary("filament-jni");
        System.loadLibrary("gltfio-jni");
        System.loadLibrary("filament-utils-jni"); // <-- Add this
    }

    public FilamentManager(Context context, SurfaceView filamentView) {
        this.filamentView = filamentView;
        Engine engine = Engine.create();
        UiHelper uiHelper = new UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK);
        uiHelper.setOpaque(false);
        this.modelViewer = new ModelViewer(filamentView, engine, uiHelper, null);
        makeTransparentBackground();
        loadAirplane(context, "3DAirplane");
        loadRunway(context, "LightRunway");
        loadChocks(context, "Chocks");
    }

    public void loadAirplane(Context context, String name) {
        ByteBuffer buffer = readAsset(context, "models/" + name + ".glb");
        modelViewer.loadModelGlb(buffer);
        airplaneAsset = modelViewer.getAsset();
        ResourceLoader resourceLoader = new ResourceLoader(modelViewer.getEngine());
        resourceLoader.loadResources(airplaneAsset);

        // Scale and rotate airplane model
        int root = airplaneAsset.getRoot();
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        int instance = tm.getInstance(root);

        float scale = 0.8f;
        float angleX = -5f;
        float angleY = 0f;
        float angleZ = 0f;

        float[] matrix = createTransform(scale, angleX, angleY, angleZ);
        tm.setTransform(instance, matrix);

        modelViewer.getScene().setSkybox(null);
    }

    public void loadRunway(Context context, String name) {
        ByteBuffer buffer = readAsset(context, "models/" + name + ".glb");
        runwayGlbBuffer = buffer.duplicate();

        MaterialProvider provider = new UbershaderProvider(modelViewer.getEngine());
        AssetLoader assetLoader = new AssetLoader(
                modelViewer.getEngine(),
                provider,
                com.google.android.filament.EntityManager.get()
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
        matrix[12] = 0f;
        matrix[13] = -0.025f;
        matrix[14] = 0f;
        tm.setTransform(instance, matrix);

        modelViewer.getScene().addEntities(runwayAsset.getEntities());
    }

    public void loadChocks(Context context, String name) {
        ByteBuffer buffer = readAsset(context, "models/" + name + ".glb");
        MaterialProvider provider = new UbershaderProvider(modelViewer.getEngine());
        AssetLoader assetLoader = new AssetLoader(
                modelViewer.getEngine(),
                provider,
                com.google.android.filament.EntityManager.get()
        );
        chocksAsset = assetLoader.createAsset(buffer);

        ResourceLoader resourceLoader = new ResourceLoader(modelViewer.getEngine());
        resourceLoader.loadResources(chocksAsset);

        // Collect chock entities and base transforms
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
            } else if (n.contains("Back")) {
                backChockEntities.add(e);
                int inst = tm.getInstance(e);
                float[] base = new float[16];
                tm.getTransform(inst, base);
                chockBaseLocal.put(e, base);
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

    private ByteBuffer readAsset(Context context, String assetName) {
        try (InputStream input = context.getAssets().open(assetName)) {
            byte[] bytes = new byte[input.available()];
            int read = input.read(bytes);
            if (read != bytes.length) {
                throw new IOException("Could not read full asset: " + assetName);
            }
            return ByteBuffer.wrap(bytes);
        } catch (IOException e) {
            Log.e("FilamentManager", "Error reading asset " + assetName, e);
            throw new RuntimeException("Error reading asset " + assetName, e);
        }
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

    private float[] createTransform(float scale, float angleX, float angleY, float angleZ) {
        double radX = Math.toRadians(angleX);
        double radY = Math.toRadians(angleY);
        double radZ = Math.toRadians(angleZ);

        float cx = (float) Math.cos(radX);
        float sx = (float) Math.sin(radX);
        float cy = (float) Math.cos(radY);
        float sy = (float) Math.sin(radY);
        float cz = (float) Math.cos(radZ);
        float sz = (float) Math.sin(radZ);

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

    // --- Public accessors for chock animation ---
    public List<Integer> getFrontChockEntities() {
        return frontChockEntities;
    }
    public List<Integer> getBackChockEntities() {
        return backChockEntities;
    }
    public Map<Integer, float[]> getChockBaseLocal() {
        return chockBaseLocal;
    }

    // Call this to rotate smoothly (deltaAngle in degrees, durationMs in ms)
    public void rotateAirplane(final float deltaAngle, int durationMs) {
        final float startYaw = airplaneYaw;
        final float endYaw = startYaw + deltaAngle;

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(startYaw, endYaw);
        animator.setDuration(durationMs);
        animator.addUpdateListener(animation -> {
            airplaneYaw = (float) animation.getAnimatedValue();
            applyTransform();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                airplaneYaw = endYaw % 360f;
                applyTransform();
            }
        });
        animator.start();
    }

    private void applyTransform() {
        if (airplaneAsset == null) return;
        int root = airplaneAsset.getRoot();
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        int instance = tm.getInstance(root);

        float scale = 0.8f;
        float angleX = -5f;
        float angleY = airplaneYaw;
        float angleZ = 0f;

        float[] matrix = createTransform(scale, angleX, angleY, angleZ);
        tm.setTransform(instance, matrix);
    }
}
