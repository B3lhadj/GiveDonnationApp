package com.example.givedonnationapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextPhone, editTextOrgName, editTextOrgDesc;
    private RadioGroup radioGroupRole;
    private RadioButton radioButtonUser, radioButtonOrg;
    private Button buttonSignup;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private View orgInfoLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextOrgName = findViewById(R.id.editTextOrgName);
        editTextOrgDesc = findViewById(R.id.editTextOrgDesc);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        radioButtonUser = findViewById(R.id.radioButtonUser);
        radioButtonOrg = findViewById(R.id.radioButtonOrg);
        buttonSignup = findViewById(R.id.buttonSignup);
        progressBar = findViewById(R.id.progressBar);
        orgInfoLayout = findViewById(R.id.orgInfoLayout);

        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioButtonOrg) {
                orgInfoLayout.setVisibility(View.VISIBLE);
            } else {
                orgInfoLayout.setVisibility(View.GONE);
            }
        });

        buttonSignup.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        final String name = editTextName.getText().toString().trim();
        final String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        final String phone = editTextPhone.getText().toString().trim();
        final String orgName = editTextOrgName.getText().toString().trim();
        final String orgDesc = editTextOrgDesc.getText().toString().trim();
        final String role = radioButtonOrg.isChecked() ? "organization" : "user";

        // Validation
        if (name.isEmpty()) {
            editTextName.setError("Name is required");
            editTextName.requestFocus();
            return;
        }

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

        if (phone.isEmpty()) {
            editTextPhone.setError("Phone is required");
            editTextPhone.requestFocus();
            return;
        }

        if (role.equals("organization") && orgName.isEmpty()) {
            editTextOrgName.setError("Organization name is required");
            editTextOrgName.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Store additional user info in Firestore
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("name", name);
                            userMap.put("email", email);
                            userMap.put("phone", phone);
                            userMap.put("role", role);

                            if (role.equals("organization")) {
                                userMap.put("orgName", orgName);
                                userMap.put("orgDesc", orgDesc);
                                userMap.put("approved", false); // Organizations need admin approval
                            }

                            db.collection("users").document(user.getUid())
                                    .set(userMap)
                                    .addOnCompleteListener(dbTask -> {
                                        progressBar.setVisibility(View.GONE);
                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(SignupActivity.this,
                                                    role.equals("organization") ?
                                                            "Registration successful! Waiting for admin approval." :
                                                            "Registration successful!",
                                                    Toast.LENGTH_LONG).show();

                                            // Redirect to login or appropriate activity
                                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(SignupActivity.this, "Failed to store user data: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SignupActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
