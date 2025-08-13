package com.example.aircraftmarshalling;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ModuleViewer extends AppCompatActivity {
    private String name, email, phone;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_module_viewer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get references to views
        ImageView itemImage = findViewById(R.id.item_image);

        TextView itemTitle = findViewById(R.id.item_title);
        TextView itemDescription = findViewById(R.id.item_description);
        ImageButton backButton = findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> finish());

        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra("title");
            String description = intent.getStringExtra("description");
            int imageResId = intent.getIntExtra("imageResId", R.drawable.empty_image);

            // Set the data to views
            itemTitle.setText(title);
            itemDescription.setText(description);
            itemImage.setImageResource(imageResId);
        }




        Button btnTutorial = findViewById(R.id.btn_tutorial);
        btnTutorial.setOnClickListener(v -> {
            Intent intent1 = new Intent(ModuleViewer.this, VideoViewer.class);
            intent1.putExtra("videoTitle", itemTitle.getText().toString());
            intent1.putExtra("videoResId", getIntent().getIntExtra("videoResId", -1)); // add this line
            startActivity(intent1);

        });


        Intent intent2 = getIntent();
        name = intent2.getStringExtra("name");
        email = intent2.getStringExtra("email");
        phone = intent2.getStringExtra("phone");

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_module);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_simulation) {
                Intent simulationtIntent = new Intent(ModuleViewer.this, SimulationPage.class);
                simulationtIntent.putExtra("name", name);
                simulationtIntent.putExtra("email", email);
                simulationtIntent.putExtra("phone", phone);
                startActivity(simulationtIntent);
            } else if (itemId == R.id.nav_assessment) {
                Intent intent1 = new Intent(ModuleViewer.this, AssessmentPage.class);
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