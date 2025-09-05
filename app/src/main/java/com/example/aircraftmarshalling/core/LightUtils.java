package com.example.aircraftmarshalling.core;

import com.google.android.filament.Engine;
import com.google.android.filament.Scene;
import com.google.android.filament.LightManager;
import com.google.android.filament.EntityManager;

public class LightUtils {
    public static void addDefaultLights(Engine engine, Scene scene) {
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
}