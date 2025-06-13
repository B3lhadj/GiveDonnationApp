package com.example.givedonnationapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener);

        // Verify menu is loaded
        Log.d(TAG, "BottomNavigationView menu: " + bottomNavigationView.getMenu().size() + " items");

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new OrganizationFragment())
                    .commit();
        }

        // Check initial auth state
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "Initial user state: " + (currentUser == null ? "null" : currentUser.getUid()));
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                Log.d(TAG, "Navigation item selected: " + itemId + ", Title: " + item.getTitle());

                if (itemId == R.id.nav_dashboard) {
                    selectedFragment = new OrganizationFragment();
                } else if (itemId == R.id.nav_campaigns) {
                    selectedFragment = new CampaignListFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                } else if (itemId == R.id.nav_logout) {
                    mAuth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class); // Replace with your login activity
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }


                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }

                return true;
            };

    private void showLogoutConfirmationDialog() {
        Log.d(TAG, "Showing logout confirmation dialog");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    Log.d(TAG, "Logout confirmed");
                    performLogout();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Log.d(TAG, "Logout cancelled");
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    private void performLogout() {
        Log.d(TAG, "Performing logout");
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        try {
            mAuth.signOut();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            Log.d(TAG, "After signOut, current user: " + (currentUser == null ? "null" : currentUser.getUid()));
            if (currentUser == null) {
                Log.d(TAG, "Sign-out successful");
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Log.d(TAG, "Sign-out failed, user still signed in");
                Toast.makeText(this, "Logout failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Logout exception: ", e);
            Toast.makeText(this, "Logout error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}