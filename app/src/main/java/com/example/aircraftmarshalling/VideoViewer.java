package com.example.aircraftmarshalling;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class VideoViewer extends AppCompatActivity {

    private VideoView videoView;
    private Button btnPlay, btnStop;
    private String name, email, phone;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_video_viewer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        TextView videoTitle = findViewById(R.id.video_title);
        videoView = findViewById(R.id.video_view);
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        Button btnResume = findViewById(R.id.btn_resume);

        videoView.setZOrderOnTop(true);

        Intent intent = getIntent();
        if (intent != null) {
            // Set the title
            String title = intent.getStringExtra("videoTitle");
            if (title != null) {
                videoTitle.setText(title);
            }

            // Get video resource ID
            int videoResId = intent.getIntExtra("videoResId", -1);
            if (videoResId != -1) {
                Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + videoResId);
                videoView.setVideoURI(videoUri);

                btnPlay.setOnClickListener(v -> {
                    videoView.setVideoURI(videoUri);
                    videoView.start();
                });

                btnResume.setOnClickListener(v -> {
                    if (!videoView.isPlaying()) {
                        videoView.start();
                    }
                });

                btnStop.setOnClickListener(v -> {
                    if (videoView.isPlaying()) {
                        videoView.pause(); // pause retains current position
                    }
                });
            } else {
                videoTitle.setText("Video not available");
            }
        }


        Intent intent2 = getIntent();
        name = intent2.getStringExtra("name");
        email = intent2.getStringExtra("email");
        phone = intent2.getStringExtra("phone");

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_module);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_simulation) {
                Intent simulationtIntent = new Intent(VideoViewer.this, SimulationPage.class);
                simulationtIntent.putExtra("name", name);
                simulationtIntent.putExtra("email", email);
                simulationtIntent.putExtra("phone", phone);
                startActivity(simulationtIntent);
            } else if (itemId == R.id.nav_assessment) {
                Intent intent1 = new Intent(VideoViewer.this, AssessmentPage.class);
                intent1.putExtra("name", getIntent().getStringExtra("name")); // or pass stored variable
                intent1.putExtra("email", getIntent().getStringExtra("email"));
                intent1.putExtra("phone", getIntent().getStringExtra("phone"));
                startActivity(intent1);

            } else {
                return false; // No action for other items
            }

//            intent.putExtra("user_email", userEmail);
//            startActivity(intent);
//            overridePendingTransition(0, 0);
            return true;
        });
    }
}
