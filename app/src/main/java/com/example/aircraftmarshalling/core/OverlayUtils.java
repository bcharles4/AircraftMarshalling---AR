package com.example.aircraftmarshalling.core;

import android.graphics.PointF;
import java.util.Map;

public class OverlayUtils {
    public static void updateSkeletonOverlay(PoseOverlayView overlayView, int imageWidth, int imageHeight,
                                             int viewWidth, int viewHeight, Map<String, PointF> posePoints) {
        overlayView.updatePosePoints(posePoints);
    }
}