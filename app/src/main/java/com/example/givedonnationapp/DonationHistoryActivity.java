package com.example.givedonnationapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class DonationHistoryActivity extends AppCompatActivity {

    private static final String TAG = "DonationHistory";
    private LinearLayout donationsContainer;
    private ProgressBar progressBar;
    private TextView noDonationsText;
    private FirebaseFirestore db;
    protected int getLayoutResourceId() {
        return R.layout.activity_donation_history;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation_history);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        donationsContainer = findViewById(R.id.donationsContainer);
        progressBar = findViewById(R.id.progressBar);
        noDonationsText = findViewById(R.id.noDonationsText);

        // Load donation history
        loadDonationHistory();
    }

    private void loadDonationHistory() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showErrorAndFinish("Please login first");
            return;
        }

        showLoadingState();

        db.collection("donations")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    hideLoadingState();

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!task.isSuccessful()) {
                        handleTaskFailure(task.getException());
                        return;
                    }

                    try {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot == null || querySnapshot.isEmpty()) {
                            showNoDonations();
                            return;
                        }
                        processDonations(querySnapshot);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing results", e);
                        showError("Error processing donation data");
                    }
                });
    }

    private void processDonations(QuerySnapshot querySnapshot) {
        runOnUiThread(() -> {
            donationsContainer.removeAllViews();

            for (QueryDocumentSnapshot document : querySnapshot) {
                try {
                    // Get the raw data map
                    Map<String, Object> donationData = document.getData();
                    addDonationCard(donationData);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing document " + document.getId(), e);
                }
            }

            if (donationsContainer.getChildCount() == 0) {
                showNoDonations();
            } else {
                donationsContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void addDonationCard(Map<String, Object> donationData) {
        try {
            View donationItem = LayoutInflater.from(this)
                    .inflate(R.layout.item_donation, donationsContainer, false);

            TextView tvCategory = donationItem.findViewById(R.id.tvCategory);
            TextView tvDetails = donationItem.findViewById(R.id.tvDetails);
            TextView tvDate = donationItem.findViewById(R.id.tvDate);

            // Set category
            String category = getSafeString(donationData.get("category"));
            tvCategory.setText(category != null ? category : "Donation");

            // Set formatted details
            tvDetails.setText(formatDonationDetails(donationData));

            // Set formatted date
            Long timestamp = donationData.get("timestamp") != null ?
                    ((Number) donationData.get("timestamp")).longValue() : null;
            tvDate.setText(formatDonationDate(timestamp));

            donationsContainer.addView(donationItem);
        } catch (Exception e) {
            Log.e(TAG, "Error creating donation card", e);
        }
    }

    private String formatDonationDetails(Map<String, Object> donationData) {
        if (donationData == null) return "No details available";

        String category = getSafeString(donationData.get("category"));
        if (category == null) return "General donation";

        try {
            switch (category) {
                case "Food Donation":
                    return String.format(Locale.getDefault(),
                            "Quantity: %d meals\nType: %s\nDonor: %s",
                            getSafeInt(donationData.get("quantity")),
                            getSafeString(donationData.get("foodType"), "Not specified"),
                            getSafeString(donationData.get("donorType"), "Anonymous"));

                case "Blood Donation":
                    Boolean hasHealthProblems = donationData.get("hasHealthProblems") != null ?
                            (Boolean) donationData.get("hasHealthProblems") : null;
                    return String.format(Locale.getDefault(),
                            "Blood Type: %s\nAge: %d\nHealth Issues: %s",
                            getSafeString(donationData.get("bloodType"), "Unknown"),
                            getSafeInt(donationData.get("age")),
                            hasHealthProblems != null ? (hasHealthProblems ? "Yes" : "No") : "Unknown");

                case "Education":
                    return String.format(Locale.getDefault(),
                            "Material: %s\nQuantity: %d items",
                            getSafeString(donationData.get("materialType"), "Various"),
                            getSafeInt(donationData.get("quantity")));

                case "Financial Aid":
                    Double amount = donationData.get("amount") != null ?
                            ((Number) donationData.get("amount")).doubleValue() : 0.0;
                    return String.format(Locale.getDefault(), "Amount: $%.2f", amount);

                default:
                    return "General donation";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting details", e);
            return "Details available";
        }
    }

    private String formatDonationDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return "Date not available";
        }
        try {
            return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(new Date(timestamp));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
            return "Date unknown";
        }
    }

    private void showLoadingState() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            donationsContainer.setVisibility(View.GONE);
            noDonationsText.setVisibility(View.GONE);
        });
    }

    private void hideLoadingState() {
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
    }

    private void showNoDonations() {
        runOnUiThread(() -> {
            noDonationsText.setVisibility(View.VISIBLE);
            donationsContainer.setVisibility(View.GONE);
        });
    }

    private void handleTaskFailure(Exception exception) {
        runOnUiThread(() -> {
            String errorMsg = "Failed to load donations";
            if (exception != null) {
                errorMsg += ": " + exception.getMessage();
                Log.e(TAG, "Firestore error", exception);
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showErrorAndFinish(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (!isFinishing() && !isDestroyed()) {
                finish();
            }
        });
    }

    private String getSafeString(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }

    private String getSafeString(Object value) {
        return getSafeString(value, null);
    }

    private int getSafeInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
    }
}