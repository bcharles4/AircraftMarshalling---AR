package com.example.aircraftmarshalling.core;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

public class CameraManager {
    private final Context context;
    private final TextureView previewView;
    private final PoseDetectionManager poseDetectionManager;
    private boolean isUsingFrontCamera = false;

    public CameraManager(Context context, TextureView previewView, PoseDetectionManager poseDetectionManager) {
        this.context = context;
        this.previewView = previewView;
        this.poseDetectionManager = poseDetectionManager;
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                CameraSelector selector = isUsingFrontCamera
                        ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA;

                Preview preview = new Preview.Builder().build();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), this::processImageProxy);

                preview.setSurfaceProvider(request -> {
                    Surface surface = new Surface(previewView.getSurfaceTexture());
                    request.provideSurface(surface, ContextCompat.getMainExecutor(context), result -> surface.release());
                });

                cameraProvider.bindToLifecycle((androidx.lifecycle.LifecycleOwner) context, selector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraManager", "Camera start failed", e);
            } catch (IllegalArgumentException iae) {
                Log.e("CameraManager", "No camera matched selector", iae);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void processImageProxy(ImageProxy imageProxy) {
        poseDetectionManager.processImageProxy(imageProxy);
    }

    public void toggleCamera() {
        isUsingFrontCamera = !isUsingFrontCamera;
        startCamera();
    }
}