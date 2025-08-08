package com.example.aircraftmarshalling;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class PoseOverlayView extends View {

    private final Paint dotPaint;
    private final Paint linePaint;
    private final float DOT_RADIUS = 20f;

    private Map<String, PointF> posePoints = new HashMap<>();

    public PoseOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        dotPaint = new Paint();
        dotPaint.setColor(Color.WHITE);
        dotPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(10f);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    public void updatePosePoints(Map<String, PointF> newPoints) {
        this.posePoints = newPoints;
        invalidate();  // Triggers onDraw()
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw dots
        for (PointF point : posePoints.values()) {
            canvas.drawCircle(point.x, point.y, DOT_RADIUS, dotPaint);
        }

        // Draw shoulder-wrist lines
        drawLineBetween(canvas, "LEFT_SHOULDER", "LEFT_WRIST");
        drawLineBetween(canvas, "RIGHT_SHOULDER", "RIGHT_WRIST");
    }

    private void drawLineBetween(Canvas canvas, String start, String end) {
        PointF startPt = posePoints.get(start);
        PointF endPt = posePoints.get(end);
        if (startPt != null && endPt != null) {
            canvas.drawLine(startPt.x, startPt.y, endPt.x, endPt.y, linePaint);
        }
    }
}
