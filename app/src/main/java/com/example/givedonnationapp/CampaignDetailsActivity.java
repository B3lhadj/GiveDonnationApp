package com.example.givedonnationapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CampaignDetailsActivity extends AppCompatActivity {

    // Campaign Views
    private ImageView campaignImageView;
    private TextView titleTextView, descriptionTextView;
    private TextView goalTextView, currentTextView, progressTextView;
    private TextView typeTextView, dateTextView, locationTextView, addressTextView;
    private TextView statusTextView, donorsCountTextView;
    private ProgressBar progressBar, donationProgressBar;
    private Button deleteButton, updateButton, viewDonationsButton;

    // Donations Views
    private RecyclerView donationsRecyclerView;
    private ProgressBar donationsProgressBar;
    private View donationsContainer;

    private FirebaseFirestore db;
    private String campaignId;
    private Campaign campaign;
    private List<Donation> donations = new ArrayList<>();
    private DonationAdapter donationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaign_details);

        db = FirebaseFirestore.getInstance();
        campaignId = getIntent().getStringExtra("campaignId");
        if (campaignId == null) {
            finishWithError("Campaign not found");
            return;
        }

        initializeViews();
        loadCampaignData();
        setupButtons();
    }

    private void initializeViews() {
        // Campaign views
        campaignImageView = findViewById(R.id.campaignImageView);
        titleTextView = findViewById(R.id.titleTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        goalTextView = findViewById(R.id.goalTextView);
        currentTextView = findViewById(R.id.currentTextView);
        progressTextView = findViewById(R.id.progressTextView);
        typeTextView = findViewById(R.id.typeTextView);
        dateTextView = findViewById(R.id.dateTextView);
        locationTextView = findViewById(R.id.locationTextView);
        addressTextView = findViewById(R.id.addressTextView);
        statusTextView = findViewById(R.id.statusTextView);
        donorsCountTextView = findViewById(R.id.donorsCountTextView);
        progressBar = findViewById(R.id.progressBar);
        donationProgressBar = findViewById(R.id.donationProgressBar);
        deleteButton = findViewById(R.id.deleteButton);
        updateButton = findViewById(R.id.updateButton);
        viewDonationsButton = findViewById(R.id.viewDonationsButton);

        // Donations views
        donationsRecyclerView = findViewById(R.id.donationsRecyclerView);
        donationsProgressBar = findViewById(R.id.donationsProgressBar);
        donationsContainer = findViewById(R.id.donationsContainer);

        // Setup donations RecyclerView
        donationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        donationAdapter = new DonationAdapter(donations);
        donationsRecyclerView.setAdapter(donationAdapter);
    }

    private void loadCampaignData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("campaigns").document(campaignId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            campaign = document.toObject(Campaign.class);
                            if (campaign != null) {
                                campaign.setId(document.getId());
                                updateUI();
                                loadDonations();
                            }
                        } else {
                            finishWithError("Campaign not found");
                        }
                    } else {
                        finishWithError("Failed to load campaign");
                    }
                });
    }

    private void updateUI() {
        // Load campaign image with null check
        if (campaign.getPhotoUrl() != null && !campaign.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(campaign.getPhotoUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .into(campaignImageView);
        } else {
            campaignImageView.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Set text with null checks
        titleTextView.setText(campaign.getTitle() != null ? campaign.getTitle() : "");
        descriptionTextView.setText(campaign.getDescription() != null ? campaign.getDescription() : "");

        // Format numbers with null/zero checks
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        double goalQuantity = campaign.getGoalQuantity() > 0 ? campaign.getGoalQuantity() : 0;
        double currentQuantity = campaign.getCurrentQuantity() > 0 ? campaign.getCurrentQuantity() : 0;

        goalTextView.setText(String.format("Goal: %s", numberFormat.format(goalQuantity)));
        currentTextView.setText(String.format("Collected: %s", numberFormat.format(currentQuantity)));

        // Calculate progress safely
        int progress = goalQuantity > 0 ?
                (int) ((currentQuantity * 100f) / goalQuantity) : 0;
        donationProgressBar.setProgress(Math.min(progress, 100)); // Ensure doesn't exceed 100%
        progressTextView.setText(String.format("%d%% Achieved", progress));

        // Other details with null checks
        typeTextView.setText(String.format("Type: %s",
                campaign.getType() != null ? campaign.getType() : "N/A"));

        // Date formatting with null checks


        locationTextView.setText(String.format("Location: %s",
                campaign.getLocation() != null ? campaign.getLocation() : "N/A"));
        addressTextView.setText(String.format("Address: %s",
                campaign.getAddress() != null ? campaign.getAddress() : "N/A"));
        statusTextView.setText(String.format("Status: %s",
                campaign.isActive() ? "Active" : "Completed"));
    }

    // Example formatDate method (adjust format pattern as needed)
    private String formatDate(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            return "Invalid Date";
        }
    }

    private void loadDonations() {
        donationsProgressBar.setVisibility(View.VISIBLE);
        db.collection("donations")
                .whereEqualTo("campaignId", campaignId)
                .get()
                .addOnCompleteListener(task -> {
                    donationsProgressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        donations.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Donation donation = document.toObject(Donation.class);
                            if (donation != null) {
                                donation.setDonationId(document.getId());
                                donations.add(donation);
                            }
                        }
                        donationAdapter.notifyDataSetChanged();
                        donorsCountTextView.setText("Donors: " + donations.size());

                        if (donations.isEmpty()) {
                            donationsContainer.setVisibility(View.GONE);
                        } else {
                            donationsContainer.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(this, "Failed to load donations", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupButtons() {
        deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        updateButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, UpdateCampaignActivity.class);
            intent.putExtra("campaignId", campaignId);
            startActivity(intent);
        });
        viewDonationsButton.setOnClickListener(v -> toggleDonationsVisibility());
    }

    private void toggleDonationsVisibility() {
        if (donationsContainer.getVisibility() == View.VISIBLE) {
            donationsContainer.setVisibility(View.GONE);
            viewDonationsButton.setText("View Donations");
        } else {
            donationsContainer.setVisibility(View.VISIBLE);
            viewDonationsButton.setText("Hide Donations");
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Campaign")
                .setMessage("Are you sure you want to delete this campaign?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCampaign())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCampaign() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("campaigns").document(campaignId)
                .delete()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Campaign deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete campaign", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void finishWithError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private class DonationAdapter extends RecyclerView.Adapter<DonationViewHolder> {
        private List<Donation> donations;

        DonationAdapter(List<Donation> donations) {
            this.donations = donations;
        }

        @NonNull
        @Override
        public DonationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_donations, parent, false);
            return new DonationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DonationViewHolder holder, int position) {
            Donation donation = donations.get(position);
            holder.bind(donation);
        }

        @Override
        public int getItemCount() {
            return donations.size();
        }
    }

    private static class DonationViewHolder extends RecyclerView.ViewHolder {
        private TextView donorTypeView, detailsView, dateView, amountView;

        public DonationViewHolder(@NonNull View itemView) {
            super(itemView);
            donorTypeView = itemView.findViewById(R.id.donorTypeTextView);
            detailsView = itemView.findViewById(R.id.donationDetailsTextView);
            dateView = itemView.findViewById(R.id.donationDateTextView);
            amountView = itemView.findViewById(R.id.donationAmountTextView);
        }

        @SuppressLint("DefaultLocale")
        public void bind(Donation donation) {
            // Format date
            String date = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                    .format(new Date(donation.getTimestamp()));
            dateView.setText(date);

            // Set donor type
            donorTypeView.setText(donation.getUserRole() != null ?
                    donation.getUserRole() : "Anonymous");

            // Set details based on donation type
            switch (donation.getCategory()) {
                case "Food Donation":
                    detailsView.setText(String.format("%s %s (%s)",
                            donation.getQuantity(),
                            "meals",
                            donation.getFoodType()));
                    amountView.setVisibility(View.GONE);
                    break;
                case "Blood Donation":
                    detailsView.setText(String.format("%s (%s)",
                            donation.getBloodType(),
                            donation.getHasHealthProblems() ? "With health issues" : "Healthy"));
                    amountView.setVisibility(View.GONE);
                    break;
                case "Financial Aid":
                    detailsView.setText("Monetary donation");
                    amountView.setText(String.format("$%.2f", donation.getAmount()));
                    amountView.setVisibility(View.VISIBLE);
                    break;
                case "Education":
                    detailsView.setText(String.format("%d %s (%s)",
                            donation.getQuantity(),
                            "items",
                            donation.getMaterialType()));
                    amountView.setVisibility(View.GONE);
                    break;
                default:
                    detailsView.setText("General donation");
                    amountView.setVisibility(View.GONE);
            }
        }
    }
}