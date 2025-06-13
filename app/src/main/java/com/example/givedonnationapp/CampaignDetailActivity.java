package com.example.givedonnationapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CampaignDetailActivity extends BaseActivity {

    // Views
    private ImageView campaignImage;
    private TextView title, description, organizer, progressText, daysLeft;
    private ProgressBar progressBar;
    private Button donateButton;
    private ProgressBar progressLoader;
    private Campaign campaign;
    private FirebaseFirestore db;

    protected int getLayoutResourceId() {
        return R.layout.activity_campaign_detail;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        initializeViews();
        loadCampaignData();
    }

    private void initializeViews() {
        campaignImage = findViewById(R.id.campaign_image);
        title = findViewById(R.id.campaign_title);
        description = findViewById(R.id.campaign_description);
        organizer = findViewById(R.id.campaign_organizer);
        progressText = findViewById(R.id.progress_text);
        daysLeft = findViewById(R.id.days_left);
        progressBar = findViewById(R.id.progress_bar);
        donateButton = findViewById(R.id.donate_button);
        progressLoader = findViewById(R.id.progress_loader);

        donateButton.setOnClickListener(v -> navigateToDonation());
    }

    private void loadCampaignData() {
        String campaignId = getIntent().getStringExtra("campaignId");
        if (campaignId == null || campaignId.isEmpty()) {
            Log.e("CampaignDetail", "Invalid or missing campaignId");
            showErrorAndFinish("Invalid campaign ID");
            return;
        }

        Log.d("CampaignDetail", "Loading campaign with ID: " + campaignId);
        showLoading(true);
        db.collection("campaigns").document(campaignId)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d("CampaignDetail", "Firestore document retrieved: " + task.getResult().getData());
                        displayCampaign(task.getResult());
                    } else {
                        Log.e("CampaignDetail", "Failed to load campaign: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        showErrorAndFinish("Failed to load campaign");
                    }
                });
    }

    // Map Firestore type to display type for legacy data
    private String mapFirestoreTypeToCategory(String firestoreType) {
        if (firestoreType == null) return null;
        switch (firestoreType) {
            case "Food":
                return "Food Donation";
            case "Blood":
                return "Blood Donation";
            case "Financial":
                return "Financial Aid";
            case "Education":
                return "Education";
            default:
                return firestoreType;
        }
    }

    private void displayCampaign(DocumentSnapshot document) {
        campaign = document.toObject(Campaign.class);
        if (campaign == null) {
            Log.e("CampaignDetail", "Failed to parse document to Campaign object: " + document.getData());
            showErrorAndFinish("Invalid campaign data");
            return;
        }

        // Ensure campaign ID is set
        campaign.setId(document.getId());

        // Map legacy type to expected type
        String displayType = mapFirestoreTypeToCategory(campaign.getType());
        campaign.setType(displayType);

        // Log raw and mapped campaign data
        Log.d("CampaignDetail", "Campaign object: " +
                "id=" + campaign.getId() + ", " +
                "title=" + campaign.getTitle() + ", " +
                "type=" + campaign.getType() + ", " +
                "description=" + campaign.getDescription() + ", " +
                "orgName=" + campaign.getOrgName() + ", " +
                "photoUrl=" + campaign.getPhotoUrl());

        // Validate required fields
        if (campaign.getId() == null || campaign.getTitle() == null || campaign.getType() == null) {
            Log.e("CampaignDetail", "Missing required fields: " +
                    "id=" + campaign.getId() + ", " +
                    "title=" + campaign.getTitle() + ", " +
                    "type=" + campaign.getType());
            showErrorAndFinish("Invalid campaign data");
            return;
        }

        // Validate campaign type
        String campaignType = campaign.getType();
        if (!campaignType.equals("Food Donation") && !campaignType.equals("Blood Donation") &&
                !campaignType.equals("Education") && !campaignType.equals("Financial Aid")) {
            Log.e("CampaignDetail", "Invalid campaign type: " + campaignType);
            showErrorAndFinish("Unsupported campaign type");
            return;
        }

        // Load image
        Glide.with(this)
                .load(campaign.getPhotoUrl())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error_image)
                .into(campaignImage);

        // Set text values
        title.setText(campaign.getTitle());
        description.setText(campaign.getDescription() != null ? campaign.getDescription() : "");
        organizer.setText(getString(R.string.organized_by, campaign.getOrgName() != null ? campaign.getOrgName() : "Unknown"));

        // Calculate and display progress
        int currentQuantity = campaign.getCurrentQuantity();
        int goalQuantity = campaign.getGoalQuantity();
        int progress = calculateProgress(currentQuantity, goalQuantity);
        progressBar.setProgress(progress);
        progressText.setText(getString(R.string.progress_text,
                formatNumber(currentQuantity),
                formatNumber(goalQuantity),
                progress));

        // Calculate days remaining
        long daysRemaining = calculateDaysRemaining(campaign.getEndDate());
        daysLeft.setText(getResources().getQuantityString(
                R.plurals.days_left, (int) daysRemaining, daysRemaining));
    }

    private int calculateProgress(int currentQuantity, int goalQuantity) {
        if (goalQuantity <= 0) return 0;
        return (int) ((currentQuantity * 100.0) / goalQuantity);
    }

    private long calculateDaysRemaining(String endDate) {
        if (endDate == null || endDate.isEmpty()) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date end = sdf.parse(endDate);
            Date now = new Date();
            if (end == null || now.after(end)) return 0;
            long diffInMillis = end.getTime() - now.getTime();
            return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            Log.e("CampaignDetail", "Failed to parse endDate: " + endDate, e);
            return 0;
        }
    }

    private String formatNumber(int number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }

    private void navigateToDonation() {
        if (campaign == null || campaign.getId() == null || campaign.getTitle() == null || campaign.getType() == null) {
            Log.e("CampaignDetail", "Cannot navigate: " +
                    "id=" + (campaign != null ? campaign.getId() : null) + ", " +
                    "title=" + (campaign != null ? campaign.getTitle() : null) + ", " +
                    "type=" + (campaign != null ? campaign.getType() : null));
            Toast.makeText(this, "Invalid campaign data", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = campaign.getType();
        if (!type.equals("Food Donation") && !type.equals("Blood Donation") &&
                !type.equals("Education") && !type.equals("Financial Aid")) {
            Log.e("CampaignDetail", "Invalid campaign type: " + type);
            Toast.makeText(this, "Unsupported campaign type", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CampaignDetail", "Navigating to DonationActivity: " +
                "campaignId=" + campaign.getId() + ", " +
                "campaignTitle=" + campaign.getTitle() + ", " +
                "category=" + type);

        Intent intent = new Intent(this, DonationActivity.class)
                .putExtra("campaignId", campaign.getId())
                .putExtra("campaignTitle", campaign.getTitle())
                .putExtra("category", type);
        startActivity(intent);
    }

    private void showLoading(boolean isLoading) {
        progressLoader.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        donateButton.setEnabled(!isLoading);
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}