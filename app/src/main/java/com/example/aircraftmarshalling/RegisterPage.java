package com.example.aircraftmarshalling;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterPage extends AppCompatActivity {

    private static final String REGISTER_URL = "https://aircraft-marshalling-f350337809da.herokuapp.com/api/users/register";
    private RequestQueue requestQueue;
    private EditText etName, etEmail, etStudentId, etPassword;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_page);

        requestQueue = Volley.newRequestQueue(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back button
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Go to login page
        TextView tvLogin = findViewById(R.id.tv_login);
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterPage.this, LoginPage.class);
            startActivity(intent);
        });

        // Inputs
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etStudentId = findViewById(R.id.et_student_no);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!validateInput(name, email, studentId, password)) return;

            registerUser(name, email, studentId, password);
        });
    }

    private boolean validateInput(String name, String email, String studentId, String password) {
        if (name.length() < 5 || name.length() > 64) {
            etName.setError("Name must be between 5 and 64 characters");
            etName.requestFocus();
            return false;
        }

        if (!email.contains("@")) {
            etEmail.setError("Invalid email address");
            etEmail.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        if (studentId.isEmpty() || studentId.length() < 6) {
            etStudentId.setError("Invalid student ID");
            etStudentId.requestFocus();
            return false;
        }
        return true;
    }

    private void registerUser(String name, String email, String studentId, String password) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("name", name);
            requestBody.put("email", email);
            requestBody.put("studentId", studentId);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest registerRequest = new JsonObjectRequest(
                Request.Method.POST,
                REGISTER_URL,
                requestBody,
                response -> {
                    try {
                        String message = response.getString("message");
                        if (message.toLowerCase().contains("registered")) {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            new android.os.Handler().postDelayed(() -> {
                                startActivity(new Intent(this, LoginPage.class));
                                finish();
                            }, 1000);
                        } else {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMessage = "Registration failed";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            JSONObject errorResponse = new JSONObject(new String(error.networkResponse.data));
                            errorMessage = errorResponse.getString("message");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(registerRequest);
    }
}
