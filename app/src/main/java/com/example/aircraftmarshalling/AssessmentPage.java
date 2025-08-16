package com.example.aircraftmarshalling;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.view.View;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

public class AssessmentPage extends AppCompatActivity {
    private String name, email, phone;

    RadioGroup[] questions = new RadioGroup[8];
    int[] correctAnswers = {
            0, // q1: "Start the engine"
            2, // q2: "Stop immediately"
            1, // q3: "Turn right"
            1, // q4: "Hold your current spot"
            1, // q5: "No, do not proceed"
            0, // q6: "Stop slowly"
            2, // q7: "Reduce taxiing speed"
            0  // q8: "Transfer control to next marshal"
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_assessment_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent2 = getIntent();
        name = intent2.getStringExtra("name");
        email = intent2.getStringExtra("email");
        phone = intent2.getStringExtra("phone");

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_assessment);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_module) {
                intent = new Intent(AssessmentPage.this, ModulePage.class);
                startActivity(intent);
            } else if (itemId == R.id.nav_assessment) {
                intent = new Intent(AssessmentPage.this, AssessmentPage.class);
                startActivity(intent);
            }
            else if (itemId == R.id.nav_simulation) {

                intent = new Intent(AssessmentPage.this, SimulationPage.class);
                startActivity(intent);
            }
            else {
                return false; // No action for other items
            }

//            intent.putExtra("user_email", userEmail);
//            startActivity(intent);
//            overridePendingTransition(0, 0);
            return true;
        });

        questions[0] = findViewById(R.id.q1);
        questions[1] = findViewById(R.id.q2);
        questions[2] = findViewById(R.id.q3);
        questions[3] = findViewById(R.id.q4);
        questions[4] = findViewById(R.id.q5);
        questions[5] = findViewById(R.id.q6);
        questions[6] = findViewById(R.id.q7);
        questions[7] = findViewById(R.id.q8);

        Button submitBtn = findViewById(R.id.submitButton);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswers();
            }
        });


        ImageView logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(AssessmentPage.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent intent = new Intent(AssessmentPage.this, LoginPage.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish(); // Close current activity
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });



    }


    private void checkAnswers() {
        int score = 0;

        for (int i = 0; i < questions.length; i++) {
            int selectedId = questions[i].getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(this, "Please answer all questions.", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedBtn = findViewById(selectedId);
            int index = questions[i].indexOfChild(selectedBtn);

            if (index == correctAnswers[i]) {
                score++;
            }
        }

        // Show result locally
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Assessment Result")
                .setMessage("You got " + score + " out of 8 correct.")
                .setPositiveButton("OK", null)
                .show();

        // Send score to backend
        updateUserScore(name,  score);
    }
    private void updateUserScore(String name, int score) {
        String url = "https://aircraft-marshalling-f350337809da.herokuapp.com/api/users/score/" + name;

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.PUT, url,
                response -> Toast.makeText(AssessmentPage.this, "Assessment Score Updated Successfully!", Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(AssessmentPage.this, "Error updating score: " + error.getMessage(), Toast.LENGTH_LONG).show()
        ) {
            @Override
            public byte[] getBody() {
                JSONObject body = new JSONObject();
                try {
                    body.put("score", score); // ✅ only send score
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return body.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        queue.add(stringRequest);
    }

}