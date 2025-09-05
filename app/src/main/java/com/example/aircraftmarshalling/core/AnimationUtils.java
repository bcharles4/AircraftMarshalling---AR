package com.example.aircraftmarshalling.core;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.ImageView;

public class AnimationUtils {
    public static void rotateAirplane(ImageView movableImage, float toDegrees) {
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(
                movableImage,
                "rotation",
                movableImage.getRotation(),
                toDegrees
        );
        rotateAnimator.setDuration(2500);
        rotateAnimator.start();
    }
    // ...other animation helpers...
}