package com.example.givedonnationapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {

    protected FirebaseAuth mAuth;
    protected FirebaseFirestore db;
    protected String userRole;

    // Map to store button IDs to their corresponding activities
    private final Class<?>[] activityClasses = {

            DonationHistoryActivity.class,

    };

    private final int[] buttonIds = {
            R.id.dashboardBtn,
            R.id.campaignBtn,
            R.id.historyBtn,
            R.id.profileBtn,
            R.id.organizationBtn
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupFooter();
    }

    protected abstract int getLayoutResourceId();

    protected void setupFooter() {
        try {
            // Set up click listeners for all footer buttons
            for (int buttonId : buttonIds) {
                View button = findViewById(buttonId);
                if (button != null) {
                    button.setOnClickListener(this);
                    button.setClickable(true);
                    button.setFocusable(true);
                }
            }

            // Set up role-based visibility
            checkUserRoleAndAdjustFooter();

            // Log to verify setup
            Log.d("FooterSetup", "Footer buttons initialized");

        } catch (Exception e) {
            Log.e("FooterSetup", "Initialization failed: " + e.getMessage(), e);
            showToast("Navigation system error");
        }
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        Log.d("Navigation", "Button clicked: " + viewId);

        // Find which button was clicked
        for (int i = 0; i < buttonIds.length; i++) {
            if (viewId == buttonIds[i]) {
                Class<?> targetActivity = activityClasses[i];
                Log.d("Navigation", "Launching: " + targetActivity.getSimpleName());

                // Launch the corresponding activity
                try {
                    if (!this.getClass().equals(targetActivity)) {
                        Intent intent = new Intent(BaseActivity.this, targetActivity);
                        startActivity(intent);
                        finish(); // Close current activity
                    }
                } catch (Exception e) {
                    Log.e("Navigation", "Failed to start activity", e);
                    Toast.makeText(this, "Navigation failed", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private void checkUserRoleAndAdjustFooter() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w("Auth", "No authenticated user");
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains("role")) {
                        userRole = document.getString("role");
                        updateFooterVisibility("organization".equals(userRole));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Role check failed", e);
                    updateFooterVisibility(false); // Default to regular user
                });
    }

    private void updateFooterVisibility(boolean isOrganization) {
        try {
            View campaignBtn = findViewById(R.id.campaignBtn);
            View historyBtn = findViewById(R.id.historyBtn);
            View organizationBtn = findViewById(R.id.organizationBtn);

            if (campaignBtn != null) campaignBtn.setVisibility(isOrganization ? View.VISIBLE : View.GONE);
            if (historyBtn != null) historyBtn.setVisibility(isOrganization ? View.GONE : View.VISIBLE);
            if (organizationBtn != null) organizationBtn.setVisibility(isOrganization ? View.VISIBLE : View.GONE);

        } catch (Exception e) {
            Log.e("Visibility", "Failed to update footer", e);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}