package com.example.givedonnationapp;

import android.content.Intent; import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup; import android.widget.GridView;

import androidx.annotation.NonNull; import androidx.annotation.Nullable; import androidx.fragment.app.Fragment;

import java.util.ArrayList; import java.util.List;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_dashboard, container, false);

        // Set up UI components
        setupMotivationalCard(view);
        setupDonationCategories(view);

        return view;
    }

    private void setupMotivationalCard(View view) {
        // Uncomment and implement if motivational card is used
    /*
    Button learnMoreBtn = view.findViewById(R.id.btnLearnMore);
    learnMoreBtn.setOnClickListener(v -> {
        // Show motivational stories or tutorial
        Intent intent = new Intent(requireActivity(), ImpactStoriesActivity.class);
        startActivity(intent);
    });
    */
    }

    private void setupDonationCategories(View view) {
        GridView gridView = view.findViewById(R.id.categoriesGrid);

        List<DonationCategory> categories = new ArrayList<>();
        categories.add(new DonationCategory(R.drawable.ic_food, "Food Donation"));
        categories.add(new DonationCategory(R.drawable.ic_blood, "Blood Donation"));
        categories.add(new DonationCategory(R.drawable.ic_education, "Education"));
        categories.add(new DonationCategory(R.drawable.ic_money, "Financial Aid"));

        CategoryAdapter adapter = new CategoryAdapter(requireContext(), categories);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, v, position, id) -> {
            DonationCategory selected = categories.get(position);
            openCampaignsForCategory(selected);
        });
    }

    private void openCampaignsForCategory(DonationCategory category) {
        Intent intent = new Intent(requireActivity(), UserCampaignListActivity.class);
        intent.putExtra("category", category.getName());
        startActivity(intent);
    }

}