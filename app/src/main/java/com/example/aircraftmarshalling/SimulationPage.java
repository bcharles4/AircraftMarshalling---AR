package com.example.aircraftmarshalling;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.SurfaceView;
import android.view.TextureView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.aircraftmarshalling.core.CameraManager;
import com.example.aircraftmarshalling.core.PoseDetectionManager;
import com.example.aircraftmarshalling.core.FilamentManager;

public class SimulationPage extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private String name, email, phone;
    private TextureView previewView;
    private PoseOverlayView poseOverlayView;
    private TextView poseStatusText;
    private SurfaceView filamentView;
    private CameraManager cameraManager;
    private PoseDetectionManager poseDetectionManager;
    private FilamentManager filamentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_simulation_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.camera_page), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        previewView = findViewById(R.id.previewView);
        poseOverlayView = findViewById(R.id.poseOverlayView);
        poseStatusText = findViewById(R.id.poseStatusText);
        filamentView = findViewById(R.id.filamentView);

        filamentManager = new FilamentManager(this, filamentView);
        poseDetectionManager = new PoseDetectionManager(this, poseStatusText, poseOverlayView, filamentManager);
        cameraManager = new CameraManager(this, previewView, poseDetectionManager);

        Button startSimButton = findViewById(R.id.startSim_button);
        ImageButton flipButton = findViewById(R.id.flipButton);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        flipButton.setOnClickListener(v -> cameraManager.toggleCamera());
        startSimButton.setOnClickListener(v -> {
            startSimButton.setVisibility(android.view.View.GONE);
            poseStatusText.setVisibility(android.view.View.VISIBLE);
            flipButton.setVisibility(android.view.View.VISIBLE);
            filamentView.setVisibility(android.view.View.VISIBLE);
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraManager.startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }

        Intent intent2 = getIntent();
        name = intent2.getStringExtra("name");
        email = intent2.getStringExtra("email");
        phone = intent2.getStringExtra("phone");

        bottomNavigationView.setSelectedItemId(R.id.nav_simulation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_simulation) {
                return true;
            } else if (itemId == R.id.nav_module || itemId == R.id.nav_assessment) {
                Intent intent1 = new Intent(SimulationPage.this, AssessmentPage.class);
                intent1.putExtra("name", name);
                intent1.putExtra("email", email);
                intent1.putExtra("phone", phone);
                startActivity(intent1);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        Button moveButton = findViewById(R.id.MoveButton);
        moveButton.setOnClickListener(v -> filamentManager.startEngine());
    }

    @Override
    protected void onResume() {
        super.onResume();
        filamentManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        filamentManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        filamentManager.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraManager.startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}