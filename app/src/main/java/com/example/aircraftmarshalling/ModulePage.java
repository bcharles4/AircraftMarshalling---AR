package com.example.aircraftmarshalling;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ModulePage extends AppCompatActivity {

    private String name, email, phone;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_module_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView start_engine = findViewById(R.id.start_engine);
        start_engine.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Start Engine");
            intent.putExtra("description", "This signal instructs the pilot to begin starting one or more engines. It is only given once safety checks have been completed and ground crew are clear of danger areas. The marshaller typically points at the engine and uses a circular motion with the hand.");
            intent.putExtra("imageResId", R.drawable.s_start_engine);
            intent.putExtra("videoResId", R.raw.start_engine);

            // Start the activity
            startActivity(intent);
        });


        ImageView negative = findViewById(R.id.negative);
        negative.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Negative/No Signal");
            intent.putExtra("description", "This is a refusal or denial signal given by the marshaller. It clearly tells the pilot not to proceed with the requested or expected action. Usually, it involves shaking the head and extending both arms outward with palms facing down or waving them side to side.");
            intent.putExtra("imageResId", R.drawable.s_negative_no_signal);
            intent.putExtra("videoResId", R.raw.sample_vid);




            // Start the activity
            startActivity(intent);
        });

        ImageView normal_stop = findViewById(R.id.normal_stop);
        normal_stop.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Normal Stop Signal");
            intent.putExtra("description", "This signal directs the pilot to bring the aircraft to a safe and controlled stop. It is used during normal parking operations or when taxiing needs to be paused. The marshaller raises both arms above the head and brings them down simultaneously to chest level.");
            intent.putExtra("imageResId", R.drawable.s_normal_stop);


            // Start the activity
            startActivity(intent);
        });


        ImageView emergency_stop = findViewById(R.id.emergency_stop);
        emergency_stop.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Emergency Stop Signal");
            intent.putExtra("description", "This urgent signal tells the pilot to stop the aircraft immediately due to danger. It is used in situations involving personnel, equipment, or environmental threats. The marshaller makes exaggerated downward chopping motions with both arms repeatedly." );
            intent.putExtra("imageResId", R.drawable.s_emergency_stop);


            // Start the activity
            startActivity(intent);
        });

        ImageView hold_position = findViewById(R.id.hold_position);
        hold_position.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Hold Position");
            intent.putExtra("description", "This signal instructs the pilot to stay in the current position and wait for further instructions. It is commonly used during aircraft ground movement coordination. The marshaller holds up one hand with the palm facing the aircraft.");
            intent.putExtra("imageResId", R.drawable.s_hold_position);


            // Start the activity
            startActivity(intent);
        });


        ImageView pass_control = findViewById(R.id.pass_control);
        pass_control.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Pass Control");
            intent.putExtra("description", "This gesture is used to hand over marshalling responsibilities to another ground crew member. It ensures coordinated control and safe movement as aircraft transitions from one marshaller to another. Typically, the current marshaller points to the next marshaller and then steps aside.");
            intent.putExtra("imageResId", R.drawable.s_pass_control);


            // Start the activity
            startActivity(intent);
        });


        ImageView engine_on_fire = findViewById(R.id.engine_on_fire);
        engine_on_fire.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Engine on Fire Signal");
            intent.putExtra("description", "This emergency signal warns the pilot that an engine is on fire. It requires immediate shutdown and activation of fire suppression systems. The marshaller points at the engine and mimics flames by waving the hand upward in a fanning motion.");
            intent.putExtra("imageResId", R.drawable.s_engine_on_fire);


            // Start the activity
            startActivity(intent);
        });

        ImageView brakes_on_fire = findViewById(R.id.brakes_on_fire);
        brakes_on_fire.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Brakes on Fire Signal");
            intent.putExtra("description", "This signal notifies the pilot that the aircraft’s brake system is overheating or on fire. Immediate action must be taken to prevent a fire spread or brake failure. The marshaller points at the wheel area and mimics fire using a rapid fanning motion.");
            intent.putExtra("imageResId", R.drawable.s_brakes_on_fire);


            // Start the activity
            startActivity(intent);
        });


        ImageView turn_right = findViewById(R.id.turn_right);
        turn_right.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Turn Right Signal");
            intent.putExtra("description", "This signal tells the pilot to steer the aircraft’s nose to the right. It is used during taxiing or parking. The marshaller raises their left arm and moves their right hand in a circular motion in the direction of the turn.");
            intent.putExtra("imageResId", R.drawable.s_turn_right);


            // Start the activity
            startActivity(intent);
        });

        ImageView chocks = findViewById(R.id.chocks);
        chocks.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Chocks are Installed Signal");
            intent.putExtra("description", "This signal informs the pilot that wheel chocks have been placed and the aircraft is secure. It is safe to shut down engines or begin unloading. The marshaller mimics the placement of chocks by holding fists together in front of the body." );
            intent.putExtra("imageResId", R.drawable.s_chocks);


            // Start the activity
            startActivity(intent);
        });


        ImageView slow_down = findViewById(R.id.slow_down);
        slow_down.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Slow Down Signal");
            intent.putExtra("description", "This signal is used to reduce the aircraft’s taxi speed. It is usually given when approaching tight spaces, equipment, or personnel. The marshaller lowers both arms slowly in a downward sweeping motion.");
            intent.putExtra("imageResId", R.drawable.s_slow_down);


            // Start the activity
            startActivity(intent);
        });


        ImageView shutoff_engine = findViewById(R.id.shutoff_engine);
        shutoff_engine.setOnClickListener(v -> {
            // Create an intent to launch the CuisineViewer
            Intent intent = new Intent(ModulePage.this, ModuleViewer.class);

            // Pass data to the intent
            intent.putExtra("title", "Shut Off Engine Signal");
            intent.putExtra("description", "This signal instructs the pilot to shut down the engines. It is given once the aircraft is parked and chocked, or during an emergency. The marshaller makes a slashing motion across the throat with one hand.");
            intent.putExtra("imageResId", R.drawable.s_shutoff_engine);


            // Start the activity
            startActivity(intent);
        });

        ImageView logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(ModulePage.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent intent = new Intent(ModulePage.this, LoginPage.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish(); // Close current activity
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });



        Intent intent2 = getIntent();
        name = intent2.getStringExtra("name");
        email = intent2.getStringExtra("email");
        phone = intent2.getStringExtra("phone");

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_module);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_simulation) {
                Intent simulationtIntent = new Intent(ModulePage.this, SimulationPage.class);
                simulationtIntent.putExtra("name", name);
                simulationtIntent.putExtra("email", email);
                simulationtIntent.putExtra("phone", phone);
                startActivity(simulationtIntent);
            } else if (itemId == R.id.nav_assessment) {
                Intent intent1 = new Intent(ModulePage.this, AssessmentPage.class);
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