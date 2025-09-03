package com.example.aircraftmarshalling;

import android.util.Log;

import com.google.android.filament.Engine;
import com.google.android.filament.LightManager;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.TransformManager;
import com.google.android.filament.gltfio.FilamentAsset;

import java.util.HashMap;
import java.util.Map;

import com.google.android.filament.MaterialInstance;

public class FilamentUtils {

    private static final String TAG = "FilamentHierarchy";

    public static void logAssetHierarchy(FilamentAsset asset) {
        Engine engine = asset.getEngine();
        TransformManager tm = engine.getTransformManager();

        int root = asset.getRoot();
        int[] entities = asset.getEntities();

        // Build parent → children mapping
        Map<Integer, java.util.List<Integer>> tree = new HashMap<>();
        for (int e : entities) {
            if (tm.hasComponent(e)) {
                int inst = tm.getInstance(e);
                int parent = tm.getParent(inst);
                if (!tree.containsKey(parent)) {
                    tree.put(parent, new java.util.ArrayList<>());
                }
                tree.get(parent).add(e);
            }
        }

        Log.d(TAG, "===== Filament Asset Hierarchy =====");
        logEntityRecursive(asset, root, tree, 0);
        Log.d(TAG, "====================================");
    }

    private static void logEntityRecursive(FilamentAsset asset, int entity,
                                           Map<Integer, java.util.List<Integer>> tree, int depth) {
        String indent = new String(new char[depth]).replace("\0", "  ");

        String name = asset.getName(entity);
        if (name == null) name = "Unnamed";

        StringBuilder info = new StringBuilder();
        info.append(indent).append("- Entity ID: ").append(entity)
                .append(" | Name: ").append(name);

        Engine engine = asset.getEngine();
        RenderableManager rm = engine.getRenderableManager();
        TransformManager tm = engine.getTransformManager();
        LightManager lm = engine.getLightManager();

        if (rm.hasComponent(entity)) info.append(" [Renderable]");
        if (tm.hasComponent(entity)) info.append(" [Transform]");
        if (lm.hasComponent(entity)) info.append(" [Light]");

        Log.d(TAG, info.toString());

        // Recurse into children
        java.util.List<Integer> children = tree.getOrDefault(entity, new java.util.ArrayList<>());
        for (int child : children) {
            logEntityRecursive(asset, child, tree, depth + 1);
        }
    }

    public static void logAllEntities(FilamentAsset asset) {
        Engine engine = asset.getEngine();
        TransformManager tm = engine.getTransformManager();
        RenderableManager rm = engine.getRenderableManager();
        LightManager lm = engine.getLightManager();

        int[] entities = asset.getEntities();

        Log.d(TAG, "===== All Entities in Asset =====");
        for (int e : entities) {
            String name = asset.getName(e);
            if (name == null) name = "Unnamed";

            StringBuilder info = new StringBuilder();
            info.append("- Entity ID: ").append(e)
                    .append(" | Name: ").append(name);

            if (tm.hasComponent(e)) info.append(" [Transform]");
            if (rm.hasComponent(e)) info.append(" [Renderable]");
            if (lm.hasComponent(e)) info.append(" [Light]");

            Log.d(TAG, info.toString());
        }
        Log.d(TAG, "=================================");
    }

    public static void logAllEntitiesWithMaterials(FilamentAsset asset) {
        Engine engine = asset.getEngine();
        TransformManager tm = engine.getTransformManager();
        RenderableManager rm = engine.getRenderableManager();
        LightManager lm = engine.getLightManager();

        int[] entities = asset.getEntities();

        Log.d(TAG, "===== All Entities (with materials) =====");
        for (int e : entities) {
            String name = asset.getName(e);
            if (name == null) name = "Unnamed";

            StringBuilder info = new StringBuilder();
            info.append("- Entity ID: ").append(e)
                    .append(" | Name: ").append(name);

            if (tm.hasComponent(e)) info.append(" [Transform]");
            if (rm.hasComponent(e)) {
                info.append(" [Renderable]");

                // Log material instances
                int renderable = rm.getInstance(e);
                int primCount = rm.getPrimitiveCount(renderable);
                for (int i = 0; i < primCount; i++) {
                    MaterialInstance mi = rm.getMaterialInstanceAt(renderable, i);
                    if (mi != null) {
                        info.append("\n    → Material[").append(i).append("]: ")
                                .append(mi.toString());
                    }
                }
            }
            if (lm.hasComponent(e)) info.append(" [Light]");

            Log.d(TAG, info.toString());
        }
        Log.d(TAG, "=========================================");
    }

}
