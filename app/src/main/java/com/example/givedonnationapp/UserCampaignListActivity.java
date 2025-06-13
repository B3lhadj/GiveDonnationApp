package com.example.givedonnationapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class UserCampaignListActivity extends BaseActivity {

    private RecyclerView campaignsRecyclerView;
    private ProgressBar progressBar;
    private View emptyStateView;
    private List<Campaign> campaigns = new ArrayList<>();
    private FirebaseFirestore db;
    private CampaignAdapter adapter;
    private String selectedCategory;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_user_campaign_list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        selectedCategory = getIntent().getStringExtra("category");
        Log.d("UserCampaignList", "Selected Category: " + selectedCategory);
        initializeViews();
        checkAuthentication();
    }

    private void checkAuthentication() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d("UserCampaignList", "User not authenticated, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            Log.d("UserCampaignList", "User authenticated, loading campaigns");
            loadCampaignsByCategory();
        }
    }

    private void initializeViews() {
        campaignsRecyclerView = findViewById(R.id.campaignsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateView = findViewById(R.id.emptyState);

        if (campaignsRecyclerView == null || progressBar == null || emptyStateView == null) {
            Log.e("UserCampaignList", "Critical views not found: " +
                    "recyclerView=" + campaignsRecyclerView + ", " +
                    "progressBar=" + progressBar + ", " +
                    "emptyStateView=" + emptyStateView);
            showErrorAndFinish("Critical views not found in layout");
            return;
        }

        campaignsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        campaignsRecyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new CampaignAdapter(campaigns);
        campaignsRecyclerView.setAdapter(adapter);

        View refreshButton = findViewById(R.id.btnRefresh);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> loadCampaignsByCategory());
        } else {
            Log.w("UserCampaignList", "Refresh button not found in layout");
        }
    }

    // Handle both current and legacy type values
    private String mapCategoryToFirestoreType(String category) {
        if (category == null) return null;
        // Validate category
        if (!category.equals("Food Donation") && !category.equals("Blood Donation") &&
                !category.equals("Education") && !category.equals("Financial Aid")) {
            Log.e("UserCampaignList", "Invalid category received: " + category);
            return null;
        }
        return category;
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

    @SuppressLint("StringFormatInvalid")
    private void loadCampaignsByCategory() {
        showLoading(true);
        campaigns.clear();
        adapter.notifyDataSetChanged(); // Clear RecyclerView

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Log.e("UserCampaignList", "No category selected");
            showError("No category selected");
            showEmptyState(true);
            showLoading(false);
            return;
        }

        String firestoreType = mapCategoryToFirestoreType(selectedCategory);
        if (firestoreType == null) {
            Log.e("UserCampaignList", "Invalid Firestore type for category: " + selectedCategory);
            showError("Invalid campaign category");
            showEmptyState(true);
            showLoading(false);
            return;
        }

        Log.d("FirestoreQuery", "Querying campaigns with type: " + firestoreType);
        db.collection("campaigns")
                .whereEqualTo("type", firestoreType)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        Log.d("FirestoreQuery", "Documents retrieved: " + task.getResult().size());
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Log.d("FirestoreQuery", "Document data: " + document.getData());
                                Campaign campaign = document.toObject(Campaign.class);
                                if (campaign != null && campaign.getTitle() != null && campaign.getType() != null) {
                                    campaign.setId(document.getId());
                                    // Map legacy type to expected type
                                    String displayType = mapFirestoreTypeToCategory(campaign.getType());
                                    campaign.setType(displayType);
                                    campaigns.add(campaign);
                                    Log.d("FirestoreQuery", "Parsed campaign: " +
                                            "id=" + campaign.getId() + ", " +
                                            "title=" + campaign.getTitle() + ", " +
                                            "type=" + campaign.getType());
                                } else {
                                    Log.w("FirestoreQuery", "Skipping invalid campaign: " +
                                            "id=" + document.getId() + ", " +
                                            "title=" + (campaign != null ? campaign.getTitle() : null) + ", " +
                                            "type=" + (campaign != null ? campaign.getType() : null));
                                }
                            } catch (Exception e) {
                                Log.e("FirestoreError", "Error parsing document: " + document.getId(), e);
                            }
                        }

                        // Fallback query for legacy types
                        String legacyType = getLegacyType(firestoreType);
                        if (legacyType != null && campaigns.isEmpty()) {
                            Log.d("FirestoreQuery", "No campaigns found for " + firestoreType + ", trying legacy type: " + legacyType);
                            db.collection("campaigns")
                                    .whereEqualTo("type", legacyType)
                                    .get()
                                    .addOnCompleteListener(legacyTask -> {
                                        if (legacyTask.isSuccessful() && legacyTask.getResult() != null) {
                                            Log.d("FirestoreQuery", "Legacy documents retrieved: " + legacyTask.getResult().size());
                                            for (QueryDocumentSnapshot document : legacyTask.getResult()) {
                                                try {
                                                    Campaign campaign = document.toObject(Campaign.class);
                                                    if (campaign != null && campaign.getTitle() != null && campaign.getType() != null) {
                                                        campaign.setId(document.getId());
                                                        String displayType = mapFirestoreTypeToCategory(campaign.getType());
                                                        campaign.setType(displayType);
                                                        campaigns.add(campaign);
                                                        Log.d("FirestoreQuery", "Parsed legacy campaign: " +
                                                                "id=" + campaign.getId() + ", " +
                                                                "title=" + campaign.getTitle() + ", " +
                                                                "type=" + campaign.getType());
                                                    }
                                                } catch (Exception e) {
                                                    Log.e("FirestoreError", "Error parsing legacy document: " + document.getId(), e);
                                                }
                                            }
                                        }

                                        updateUIAfterQuery();
                                    });
                        } else {
                            updateUIAfterQuery();
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        showError(getString(R.string.load_error, error));
                        showEmptyState(true);
                        Log.e("FirestoreError", "Query failed: " + error);
                    }
                });
    }

    private void updateUIAfterQuery() {
        if (campaigns.isEmpty()) {
            showEmptyState(true);
            showMessage(getString(R.string.no_active_campaigns));
            Log.d("FirestoreQuery", "No valid campaigns found for category: " + selectedCategory);
        } else {
            adapter.notifyDataSetChanged();
            showEmptyState(false);
            Log.d("FirestoreQuery", "Campaigns loaded: " + campaigns.size());
        }
    }

    private String getLegacyType(String firestoreType) {
        switch (firestoreType) {
            case "Food Donation":
                return "Food";
            case "Blood Donation":
                return "Blood";
            case "Financial Aid":
                return "Financial";
            case "Education":
                return null; // No legacy type
            default:
                return null;
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        campaignsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        campaignsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void openCampaignDetails(Campaign campaign) {
        if (campaign == null || campaign.getId() == null || campaign.getTitle() == null || campaign.getType() == null) {
            Log.e("UserCampaignList", "Invalid campaign data: " +
                    "id=" + (campaign != null ? campaign.getId() : null) + ", " +
                    "title=" + (campaign != null ? campaign.getTitle() : null) + ", " +
                    "type=" + (campaign != null ? campaign.getType() : null));
            showError("Invalid campaign data");
            return;
        }

        try {
            Log.d("UserCampaignList", "Opening CampaignDetailActivity: " +
                    "campaignId=" + campaign.getId() + ", " +
                    "campaignTitle=" + campaign.getTitle() + ", " +
                    "type=" + campaign.getType());
            Intent intent = new Intent(this, CampaignDetailActivity.class)
                    .putExtra("campaignId", campaign.getId())
                    .putExtra("campaignTitle", campaign.getTitle());
            startActivity(intent);
        } catch (Exception e) {
            showError("Couldn't open campaign details");
            Log.e("NavigationError", "Failed to open campaign details", e);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showErrorAndFinish(String message) {
        showError(message);
        finish();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class CampaignAdapter extends RecyclerView.Adapter<CampaignViewHolder> {
        private final List<Campaign> campaigns;

        CampaignAdapter(List<Campaign> campaigns) {
            this.campaigns = campaigns;
        }

        @NonNull
        @Override
        public CampaignViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_campaign, parent, false);
            return new CampaignViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CampaignViewHolder holder, int position) {
            if (!isValidPosition(position)) {
                Log.w("CampaignAdapter", "Invalid position: " + position);
                return;
            }

            Campaign campaign = campaigns.get(position);
            holder.bind(campaign);
            holder.itemView.setOnClickListener(v -> openCampaignDetails(campaign));
        }

        @Override
        public int getItemCount() {
            return campaigns.size();
        }

        private boolean isValidPosition(int position) {
            return position >= 0 && position < campaigns.size();
        }
    }

    static class CampaignViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView organizerView;
        private final TextView progressView;
        private final TextView categoryView;
        private final TextView daysLeftView;
        private final ImageView campaignImage;
        private final ProgressBar progressBarHorizontal;

        CampaignViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.campaignTitle);
            organizerView = itemView.findViewById(R.id.campaignOrganizer);
            progressView = itemView.findViewById(R.id.campaignProgress);
            categoryView = itemView.findViewById(R.id.campaignCategory);
            daysLeftView = itemView.findViewById(R.id.campaignDaysLeft);
            campaignImage = itemView.findViewById(R.id.campaignImage);
            progressBarHorizontal = itemView.findViewById(R.id.progressBarHorizontal);
        }

        void bind(Campaign campaign) {
            if (campaign == null) {
                Log.w("CampaignViewHolder", "Campaign is null");
                return;
            }

            titleView.setText(campaign.getTitle() != null ? campaign.getTitle() : "Untitled");
            organizerView.setText(String.format("By: %s",
                    campaign.getOrgName() != null ? campaign.getOrgName() : "Unknown"));
            categoryView.setText(campaign.getType() != null ? campaign.getType() : "Unknown");

            int goal = campaign.getGoalQuantity() > 0 ? campaign.getGoalQuantity() : 1;
            int progressPercent = (int) ((campaign.getCurrentQuantity() * 100f) / goal);
            progressView.setText(String.format("%d/%d (%d%%)",
                    campaign.getCurrentQuantity(),
                    campaign.getGoalQuantity(),
                    progressPercent));
            progressBarHorizontal.setProgress(progressPercent);

            daysLeftView.setText(campaign.isActive() ? "Active" : "Inactive");

            if (campaign.getPhotoUrl() != null && !campaign.getPhotoUrl().isEmpty()) {
                Picasso.get().load(campaign.getPhotoUrl())
                        .placeholder(R.drawable.placeholder_campaign)
                        .error(R.drawable.placeholder_campaign)
                        .into(campaignImage);
            } else {
                campaignImage.setImageResource(R.drawable.placeholder_campaign);
            }
        }
    }
}