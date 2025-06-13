package com.example.givedonnationapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewSignup;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignup = findViewById(R.id.textViewSignup);
        progressBar = findViewById(R.id.progressBar);

        buttonLogin.setOnClickListener(v -> loginUser());
        textViewSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Minimum password length is 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Attempting login for: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Login successful, checking role for: " + user.getUid());
                            checkUserRole(user.getUid());
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Log.w(TAG, "Login succeeded but user is null");
                            Toast.makeText(LoginActivity.this, "Error: User not found", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.w(TAG, "Login failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Login failed! Please check your credentials", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRole(String uid) {
        Log.d(TAG, "Checking role for user: " + uid);
        db.collection("users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String role = document.getString("role");
                            Boolean isApproved = document.getBoolean("approved");

                            Log.d(TAG, "User data - Role: " + role + ", Approved: " + isApproved);

                            if ("organization".equals(role)) {
                                if (Boolean.TRUE.equals(isApproved)) {
                                    Log.d(TAG, "Redirecting to Organization Dashboard");
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                } else {
                                    Log.d(TAG, "Organization not approved yet");
                                    Toast.makeText(LoginActivity.this,
                                            "Your organization account is pending approval",
                                            Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            } else {
                                Log.d(TAG, "Redirecting to Main Activity");
                                startActivity(new Intent(LoginActivity.this, UserMainActivity.class));
                            }
                            finish();
                        } else {
                            Log.w(TAG, "User document doesn't exist");
                            startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                            finish();
                        }
                    } else {
                        Log.e(TAG, "Error checking user role", task.getException());
                        startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                        finish();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !isFinishing()) {
            Log.d(TAG, "User already logged in, checking role");
            checkUserRole(user.getUid());
        }
    }
}