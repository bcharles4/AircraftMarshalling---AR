package com.example.aircraftmarshalling;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import java.util.HashMap;
import java.util.Map;

public class LoginPage extends AppCompatActivity {

    private static final String LOGIN_URL = "https://aircraft-marshalling-f350337809da.herokuapp.com/api/users/login";

    private RequestQueue requestQueue;
    private EditText etEmail, etPassword;
    private Button loginButton;

    private TextView forgot_pass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);

        requestQueue = Volley.newRequestQueue(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI Bindings
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        TextView registerText = findViewById(R.id.tv_register);
        registerText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, RegisterPage.class);
            startActivity(intent);
        });

        etEmail = findViewById(R.id.et_email);      // Input field for email
        etPassword = findViewById(R.id.et_password); // Input field for password
        loginButton = findViewById(R.id.btn_login);  // Login button

        loginButton.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        forgot_pass = findViewById(R.id.forgot_pass);

        forgot_pass.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, ForgotPassword.class);
            startActivity(intent);
        });

    }


    private void loginUser(String email, String password) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest loginRequest = new JsonObjectRequest(
                Request.Method.POST,
                LOGIN_URL,
                requestBody,
                response -> {
                    try {
                        String message = response.getString("message");
                        if (message.equalsIgnoreCase("Login successful")) {
                            JSONObject userJson = response.getJSONObject("user");
                            String userId = userJson.getString("_id");
                            String name = userJson.getString("name");
                            String emailResponse = userJson.getString("email");
                            String phone = userJson.getString("phone");
                            int score = userJson.optInt("score", 0);

                            Toast.makeText(LoginPage.this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();

                            // Go to module/dashboard page
                            Intent intent = new Intent(LoginPage.this, ModulePage.class);
                            intent.putExtra("userId", userId);
                            intent.putExtra("name", name);
                            intent.putExtra("email", emailResponse);
                            intent.putExtra("phone", phone);
                            intent.putExtra("score", score);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginPage.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginPage.this, "Response parsing error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMessage = "Login failed";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            JSONObject errorResponse = new JSONObject(new String(error.networkResponse.data));
                            errorMessage = errorResponse.getString("message");
                        } catch (JSONException e) {
                            errorMessage = new String(error.networkResponse.data);
                        }
                    }
                    Toast.makeText(LoginPage.this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("LoginError", errorMessage, error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(loginRequest);
    }
}
