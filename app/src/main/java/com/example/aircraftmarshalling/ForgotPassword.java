package com.example.aircraftmarshalling;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ForgotPassword extends AppCompatActivity {

    EditText etName, etEmail, etPassword;
    Button btnSubmit;
    OkHttpClient client = new OkHttpClient();

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSubmit = findViewById(R.id.btn_reset_password);

        btnSubmit.setOnClickListener(v -> sendChangePasswordRequest());
    }

    private void sendChangePasswordRequest() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("email", email);
            json.put("newPassword", password); // ✅ match API field name


            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url("https://aircraft-marshalling-f350337809da.herokuapp.com/api/users/change-password")
                    .put(body) // ✅ Use PUT instead of POST
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(ForgotPassword.this, "Request failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String res = response.body().string();

                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            // ✅ Success
                            Toast.makeText(ForgotPassword.this, "Password changed successfully!", Toast.LENGTH_LONG).show();
                            etName.setText("");
                            etEmail.setText("");
                            etPassword.setText("");
                        } else {
                            try {
                                JSONObject errorJson = new JSONObject(res);
                                String errorMsg = errorJson.optString("message", "Failed to update password");
                                Toast.makeText(ForgotPassword.this, errorMsg, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(ForgotPassword.this, "Error: " + res, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }

            });

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
