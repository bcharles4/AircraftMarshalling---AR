package com.example.aircraftmarshalling.simulation;

import android.animation.ValueAnimator;
import android.util.Log;

import com.google.android.filament.TransformManager;
import com.google.android.filament.utils.ModelViewer;

import java.util.List;
import java.util.Map;

public class ChockAnimator {
    private static final long CHOCK_ANIM_DURATION = 1000L;

    public static void hideChocks(ModelViewer modelViewer, List<Integer> frontChockEntities, List<Integer> backChockEntities, Map<Integer, float[]> chockBaseLocal) {
        TransformManager tm = modelViewer.getEngine().getTransformManager();
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
        Log.d("Chocks", "Animating chocks hide (front -> left, back -> right)");
    }

    public static void resetChocks(ModelViewer modelViewer, List<Integer> frontChockEntities, List<Integer> backChockEntities, Map<Integer, float[]> chockBaseLocal) {
        TransformManager tm = modelViewer.getEngine().getTransformManager();
        for (int e : frontChockEntities) {
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
        Log.d("Chocks", "Animating chocks reset to original");
    }
}
