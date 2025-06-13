package com.example.givedonnationapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizationFragment extends Fragment {

    private static final String TAG = "OrganizationFragment";
    private static final int LIMIT = 5;

    // UI Components
    private ProgressBar progressBar;
    private TextView tvTotalCampaigns, tvActiveCampaigns, tvCompletedCampaigns, tvNoCampaigns;
    private RecyclerView rvRecentCampaigns;
    private FloatingActionButton fabCreateCampaign;
    private View overlay;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Campaign> campaigns = new ArrayList<>();
    private CampaignAdapter campaignAdapter;
    private ListenerRegistration campaignsListener;
    private String currentOrgId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_organization, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            initializeFirebase();
            initializeViews(view);
            setupRecyclerView();
            setupClickListeners();
            Log.d(TAG, "Fragment initialized successfully");

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                redirectToLogin();
            } else {
                currentOrgId = currentUser.getUid();
                loadOrganizationData(currentOrgId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Initialization error", e);
            showErrorAndFinish("App initialization failed");
        }
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initializeViews(View view) {
        progressBar = view.findViewById(R.id.progressBar);
        tvTotalCampaigns = view.findViewById(R.id.tvTotalCampaigns);
        tvActiveCampaigns = view.findViewById(R.id.tvActiveCampaigns);
        tvCompletedCampaigns = view.findViewById(R.id.tvCompletedCampaigns);
        tvNoCampaigns = view.findViewById(R.id.tvNoCampaigns);
        rvRecentCampaigns = view.findViewById(R.id.rvRecentCampaigns);
        fabCreateCampaign = view.findViewById(R.id.fabCreateCampaign);
        overlay = view.findViewById(R.id.overlay);
    }

    @Override
    public void onStop() {
        super.onStop();
        removeFirestoreListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cleanupResources();
    }

    private void removeFirestoreListeners() {
        if (campaignsListener != null) {
            campaignsListener.remove();
            campaignsListener = null;
        }
    }

    private void cleanupResources() {
        removeFirestoreListeners();
        if (campaignAdapter != null) {
            campaignAdapter.clear();
        }
    }

    private void setupRecyclerView() {
        rvRecentCampaigns.setLayoutManager(new LinearLayoutManager(requireContext()));
        campaignAdapter = new CampaignAdapter(campaigns);
        rvRecentCampaigns.setAdapter(campaignAdapter);
        rvRecentCampaigns.setHasFixedSize(true);
    }

    private void setupClickListeners() {
        fabCreateCampaign.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), CreateCampaignActivity.class));
        });
    }

    public void switchOrganization(String newOrgId) {
        if (getActivity() == null || getActivity().isFinishing()) return;

        currentOrgId = newOrgId;
        requireActivity().runOnUiThread(() -> {
            showLoading(true);
            loadOrganizationData(newOrgId);
        });
    }

    private void loadOrganizationData(String orgId) {
        showLoading(true);
        clearCurrentData();

        removeFirestoreListeners();

        campaignsListener = db.collection("campaigns")
                .whereEqualTo("orgId", orgId)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(LIMIT)
                .addSnapshotListener((snapshot, e) -> {
                    if (getActivity() == null || getActivity().isFinishing()) return;

                    showLoading(false);

                    if (e != null) {
                        handleLoadError(e);
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        showNoCampaignsUI();
                        return;
                    }

                    processCampaigns(snapshot);
                });
    }

    private void clearCurrentData() {
        campaigns.clear();
        if (campaignAdapter != null) {
            campaignAdapter.notifyDataSetChanged();
        }
    }

    private void processCampaigns(QuerySnapshot snapshot) {
        int total = 0, active = 0, completed = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Campaign campaign = doc.toObject(Campaign.class);
                if (campaign != null) {
                    campaign.setId(doc.getId());
                    campaign.setActive(campaign.getEndDate() == null);

                    campaigns.add(campaign);
                    total++;
                    if (campaign.isActive()) active++;
                    else completed++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing campaign", e);
            }
        }

        updateUI(total, active, completed);
    }

    private void updateUI(int total, int active, int completed) {
        if (getActivity() == null || getActivity().isFinishing()) return;

        requireActivity().runOnUiThread(() -> {
            tvTotalCampaigns.setText(String.valueOf(total));
            tvActiveCampaigns.setText(String.valueOf(active));
            tvCompletedCampaigns.setText(String.valueOf(completed));

            if (campaignAdapter != null) {
                campaignAdapter.notifyDataSetChanged();
            }

            tvNoCampaigns.setVisibility(campaigns.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showLoading(boolean isLoading) {
        if (getActivity() == null || getActivity().isFinishing()) return;

        requireActivity().runOnUiThread(() -> {
            overlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    private void handleLoadError(FirebaseFirestoreException e) {
        Log.e(TAG, "Load error", e);
        showError("Failed to load data: " + (e != null ? e.getMessage() : "Unknown error"));
        showNoCampaignsUI();
    }

    private void showNoCampaignsUI() {
        if (getActivity() == null || getActivity().isFinishing()) return;

        requireActivity().runOnUiThread(() -> {
            tvNoCampaigns.setVisibility(View.VISIBLE);
            updateUI(0, 0, 0);
        });
    }

    private void redirectToLogin() {
        startActivity(new Intent(requireActivity(), LoginActivity.class));
        requireActivity().finish();
    }

    private void showError(String message) {
        if (getActivity() == null || getActivity().isFinishing()) return;

        requireActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void showErrorAndFinish(String message) {
        showError(message);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private static class CampaignAdapter extends RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder> {
        private final List<Campaign> campaigns;

        CampaignAdapter(List<Campaign> campaigns) {
            this.campaigns = campaigns;
        }

        @NonNull
        @Override
        public CampaignViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_campaign, parent, false);
            return new CampaignViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CampaignViewHolder holder, int position) {
            Campaign campaign = campaigns.get(position);
            holder.bind(campaign);

            holder.itemView.setOnClickListener(v -> {
                if (campaign.getId() != null) {
                    Intent intent = new Intent(holder.itemView.getContext(),
                            CampaignDetailsActivity.class);
                    intent.putExtra("campaignId", campaign.getId());
                    holder.itemView.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return campaigns.size();
        }

        public void clear() {
            campaigns.clear();
            notifyDataSetChanged();
        }

        static class CampaignViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvTitle, tvCategory, tvStatus, tvProgress;
            private final ProgressBar progressBar;

            CampaignViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvCampaignTitle);
                tvCategory = itemView.findViewById(R.id.tvCampaignCategory);
                tvStatus = itemView.findViewById(R.id.tvCampaignStatus);
                tvProgress = itemView.findViewById(R.id.tvCampaignProgress);
                progressBar = itemView.findViewById(R.id.progressBarCampaign);
            }

            void bind(Campaign campaign) {
                tvTitle.setText(campaign.getTitle());
                tvCategory.setText(getDisplayType(campaign.getType()));

                if (campaign.isActive()) {
                    tvStatus.setText("Active");
                    tvStatus.setTextColor(itemView.getContext()
                            .getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    tvStatus.setText("Completed");
                    tvStatus.setTextColor(itemView.getContext()
                            .getResources().getColor(android.R.color.holo_red_dark));
                }

                int goal = Math.max(campaign.getGoalQuantity(), 1);
                int progress = Math.min((int) ((campaign.getCurrentQuantity() * 100f) / goal), 100);

                tvProgress.setText(progress + "%");
                progressBar.setProgress(progress);
            }

            private String getDisplayType(String type) {
                if (type == null) return "Unknown";
                switch (type) {
                    case "Food": return "Food Donation";
                    case "Blood": return "Blood Donation";
                    case "Financial": return "Financial Aid";
                    case "Education": return "Education";
                    default: return type;
                }
            }
        }
    }
}