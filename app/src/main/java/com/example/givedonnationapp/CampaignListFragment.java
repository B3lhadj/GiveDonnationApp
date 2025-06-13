package com.example.givedonnationapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CampaignListFragment extends Fragment {

    private static final String TAG = "UserCampaignList";
    private static final int LIMIT = 20;

    // Views
    private RecyclerView mRecyclerView;
    private ProgressBar mProgressBar;
    private TextView mEmptyStateView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // Adapter
    private CampaignAdapter mAdapter;

    // Firestore
    private ListenerRegistration mCampaignsListener;
    private FirebaseFirestore mFirestore;

    // Data
    private List<Campaign> mCampaigns = new ArrayList<>();
    private boolean mShowOnlyMyCampaigns = false;
    private String mCurrentUserId;

    public static CampaignListFragment newInstance(boolean showOnlyMyCampaigns) {
        CampaignListFragment fragment = new CampaignListFragment();
        Bundle args = new Bundle();
        args.putBoolean("show_only_mine", showOnlyMyCampaigns);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirestore = FirebaseFirestore.getInstance();

        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            mCurrentUserId = user.getUid();
        }

        // Get arguments
        if (getArguments() != null) {
            mShowOnlyMyCampaigns = getArguments().getBoolean("show_only_mine", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campaign_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadCampaigns();
    }

    private void initViews(View view) {
        mRecyclerView = view.findViewById(R.id.campaignsRecyclerView);
        mProgressBar = view.findViewById(R.id.progressBar);
        mEmptyStateView = view.findViewById(R.id.emptyState);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        mSwipeRefreshLayout.setOnRefreshListener(this::refreshCampaigns);
    }

    private void setupRecyclerView() {
        mAdapter = new CampaignAdapter(mCampaigns);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);

        // Add divider between items
        DividerItemDecoration divider = new DividerItemDecoration(
                mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL
        );
        mRecyclerView.addItemDecoration(divider);
    }

    private void loadCampaigns() {
        if (mShowOnlyMyCampaigns && mCurrentUserId != null) {
            loadOrganizationCampaigns(mCurrentUserId);
        } else {
            loadAllCampaigns();
        }
    }

    private void loadOrganizationCampaigns(String orgId) {
        showLoading(true);
        clearCurrentData();

        mCampaignsListener = mFirestore.collection("campaigns")
                .whereEqualTo("orgId", orgId)
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(LIMIT)
                .addSnapshotListener((snapshot, e) -> {
                    showLoading(false);
                    mSwipeRefreshLayout.setRefreshing(false);

                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        showError("Failed to load campaigns");
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }

                    processCampaigns(snapshot);
                });
    }

    private void loadAllCampaigns() {
        showLoading(true);
        clearCurrentData();

        mCampaignsListener = mFirestore.collection("campaigns")
                .orderBy("dateCreated", Query.Direction.DESCENDING)
                .limit(LIMIT)
                .addSnapshotListener((snapshot, e) -> {
                    showLoading(false);
                    mSwipeRefreshLayout.setRefreshing(false);

                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        showError("Failed to load campaigns");
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }

                    processCampaigns(snapshot);
                });
    }

    private void processCampaigns(QuerySnapshot snapshot) {
        mCampaigns.clear();
        for (QueryDocumentSnapshot doc : snapshot) {
            Campaign campaign = doc.toObject(Campaign.class);
            campaign.setId(doc.getId());
            mCampaigns.add(campaign);
        }

        mAdapter.notifyDataSetChanged();
        showEmptyState(mCampaigns.isEmpty());
    }

    private void refreshCampaigns() {
        if (mCampaignsListener != null) {
            mCampaignsListener.remove();
        }
        loadCampaigns();
    }

    private void clearCurrentData() {
        mCampaigns.clear();
        mAdapter.notifyDataSetChanged();
    }

    private void showLoading(boolean isLoading) {
        mProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        mEmptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeFirestoreListeners();
    }

    private void removeFirestoreListeners() {
        if (mCampaignsListener != null) {
            mCampaignsListener.remove();
            mCampaignsListener = null;
        }
    }

    private static class CampaignAdapter extends RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder> {

        private final List<Campaign> mCampaigns;

        CampaignAdapter(List<Campaign> campaigns) {
            this.mCampaigns = campaigns;
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
            holder.bind(mCampaigns.get(position));
        }

        @Override
        public int getItemCount() {
            return mCampaigns.size();
        }

    private static class CampaignViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView, organizerView, progressView, categoryView, daysLeftView;
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
            if (campaign == null) return;

            titleView.setText(campaign.getTitle() != null ? campaign.getTitle() : "Untitled");
            organizerView.setText("By: " + (campaign.getOrgName() != null ? campaign.getOrgName() : "Unknown"));
            categoryView.setText(campaign.getType() != null ? campaign.getType() : "General");

            int goal = Math.max(campaign.getGoalQuantity(), 1);
            int progress = (int) ((campaign.getCurrentQuantity() * 100f) / goal);
            progressView.setText(String.format("%d/%d (%d%%)",
                    campaign.getCurrentQuantity(), goal, progress));
            progressBarHorizontal.setProgress(progress);

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





    }





