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

public class ModuleViewer extends AppCompatActivity {

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

    }
}