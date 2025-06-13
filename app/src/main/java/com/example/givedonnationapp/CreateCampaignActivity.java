package com.example.givedonnationapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateCampaignActivity extends AppCompatActivity {

    private static final String TAG = "CreateCampaign";
    private static final int IMAGE_PICK_CODE = 1000;

    private EditText titleEditText, descriptionEditText, goalEditText, startDateEditText, endDateEditText, locationEditText, addressEditText;
    private Spinner typeSpinner;
    private ProgressBar progressBar;
    private ImageView campaignImageView;
    private Button uploadImageButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    private Uri imageUri;
    private String photoUrl = "";
    protected int getLayoutResourceId() {
        return R.layout.activity_create_campaign; // Return your layout resource ID
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_campaign);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views
        initializeViews();

        // Setup spinner
        setupSpinner();

        // Set click listeners
        setClickListeners();
    }

    private void initializeViews() {
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        goalEditText = findViewById(R.id.goalEditText);
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        locationEditText = findViewById(R.id.locationEditText);
        addressEditText = findViewById(R.id.addressEditText);
        typeSpinner = findViewById(R.id.typeSpinner);
        progressBar = findViewById(R.id.progressBar);
        campaignImageView = findViewById(R.id.campaignImageView);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        Button createButton = findViewById(R.id.createButton);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.campaign_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
    }

    private void setClickListeners() {
        startDateEditText.setOnClickListener(v -> showDatePicker(startDateEditText));
        endDateEditText.setOnClickListener(v -> showDatePicker(endDateEditText));
        uploadImageButton.setOnClickListener(v -> pickImageFromGallery());
        findViewById(R.id.createButton).setOnClickListener(v -> validateAndCreateCampaign());
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    editText.setText(selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Campaign Image"), IMAGE_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE && data != null) {
            imageUri = data.getData();
            try {
                Glide.with(this)
                        .load(imageUri)
                        .into(campaignImageView);
            } catch (Exception e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImage(OnImageUploadCompleteListener listener) {
        if (imageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        StorageReference storageRef = storage.getReference()
                .child("campaign_images/" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        photoUrl = uri.toString();
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onComplete(true);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onComplete(false);
                    }
                });
    }

    private void validateAndCreateCampaign() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String goalStr = goalEditText.getText().toString().trim();
        String type = typeSpinner.getSelectedItem().toString();
        String startDate = startDateEditText.getText().toString().trim();
        String endDate = endDateEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();

        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            return;
        }
        if (description.isEmpty()) {
            descriptionEditText.setError("Description is required");
            return;
        }
        if (goalStr.isEmpty()) {
            goalEditText.setError("Goal is required");
            return;
        }
        if (startDate.isEmpty()) {
            startDateEditText.setError("Start date is required");
            return;
        }
        if (endDate.isEmpty()) {
            endDateEditText.setError("End date is required");
            return;
        }
        if (location.isEmpty()) {
            locationEditText.setError("Location is required");
            return;
        }

        try {
            int goalQuantity = Integer.parseInt(goalStr);
            if (goalQuantity <= 0) {
                goalEditText.setError("Must be greater than 0");
                return;
            }

            if (imageUri != null) {
                uploadImage(success -> {
                    if (success) {
                        createCampaign(title, description, type, goalQuantity, startDate, endDate, location, address);
                    }
                });
            } else {
                createCampaign(title, description, type, goalQuantity, startDate, endDate, location, address);
            }
        } catch (NumberFormatException e) {
            goalEditText.setError("Invalid number");
        }
    }

    private void createCampaign(String title, String description, String type, int goalQuantity,
                                String startDate, String endDate, String location, String address) {
        progressBar.setVisibility(View.VISIBLE);

        String orgId = mAuth.getCurrentUser().getUid();
        String orgName = "Organization Name"; // Fetch this from Firestore

        Map<String, Object> campaign = new HashMap<>();
        campaign.put("title", title);
        campaign.put("description", description);
        campaign.put("type", type);
        campaign.put("orgId", orgId);
        campaign.put("orgName", orgName);
        campaign.put("goalQuantity", goalQuantity);
        campaign.put("currentQuantity", 0);
        campaign.put("dateCreated", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        campaign.put("startDate", startDate);
        campaign.put("endDate", endDate);
        campaign.put("photoUrl", photoUrl);
        campaign.put("location", location);
        campaign.put("address", address);
        campaign.put("isActive", true);

        db.collection("campaigns")
                .add(campaign)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        DocumentReference document = task.getResult();
                        Log.d(TAG, "Campaign created with ID: " + document.getId());
                        Toast.makeText(this, "Campaign created successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.e(TAG, "Error creating campaign", task.getException());
                        Toast.makeText(this, "Failed to create campaign: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    interface OnImageUploadCompleteListener {
        void onComplete(boolean success);
    }
}