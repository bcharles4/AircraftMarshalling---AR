package com.example.aircraftmarshalling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

<<<<<<< HEAD
import androidx.activity.EdgeToEdge;
=======
>>>>>>> step2-SimulationPage
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
<<<<<<< HEAD
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

=======
>>>>>>> step2-SimulationPage
import android.content.Intent;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

<<<<<<< HEAD
=======
import android.widget.Button;
import android.widget.FrameLayout;

>>>>>>> step2-SimulationPage
public class SimulationPage extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private PreviewView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
<<<<<<< HEAD
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_simulation_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.camera_page), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        previewView = findViewById(R.id.previewView);

        // Handle camera permission
=======
        setContentView(R.layout.activity_simulation_page);

        // 🔧 View references
        previewView = findViewById(R.id.previewView);
        FrameLayout runwayContainer = findViewById(R.id.runwayContainer);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        Button startSimButton = findViewById(R.id.startSim_button); // This is from your XML Button

        // 🛬 Hide runway at start
        runwayContainer.setVisibility(View.GONE);

// 📷 Start camera right away (permission check)
>>>>>>> step2-SimulationPage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }

<<<<<<< HEAD
        ImageView movableImage = findViewById(R.id.movableImage);

        movableImage.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        view.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;

                    default:
                        return false;
                }
                return true;
=======
// ▶️ Start Button Logic
        startSimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSimButton.setVisibility(View.GONE);        // hide the button
                runwayContainer.setVisibility(View.VISIBLE);    // show runway container
>>>>>>> step2-SimulationPage
            }
        });

        // ✅ Bottom Navigation setup
<<<<<<< HEAD
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_simulation); // highlight current tab
=======
        bottomNavigationView.setSelectedItemId(R.id.nav_simulation);
>>>>>>> step2-SimulationPage

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_simulation) {
                return true; // Already on this page
            } else if (itemId == R.id.nav_module) {
                startActivity(new Intent(SimulationPage.this, ModulePage.class));
            } else if (itemId == R.id.nav_assessment) {
                startActivity(new Intent(SimulationPage.this, AssessmentPage.class));
            } else {
                return false;
            }

<<<<<<< HEAD
            overridePendingTransition(0, 0); // optional: no animation between screens
=======
            overridePendingTransition(0, 0);
>>>>>>> step2-SimulationPage
            return true;
        });
    }


<<<<<<< HEAD
=======

>>>>>>> step2-SimulationPage
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("SimulationPage", "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
<<<<<<< HEAD
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
=======
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
>>>>>>> step2-SimulationPage
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
