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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdateCampaignActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1000;
    private static final String TAG = "UpdateCampaign";

    // UI Components
    private ImageView campaignImageView;
    private EditText titleEditText, descriptionEditText, goalEditText;
    private EditText startDateEditText, endDateEditText, locationEditText, addressEditText;
    private Spinner statusSpinner, typeSpinner;
    private ProgressBar progressBar;
    private Button updateButton, uploadImageButton;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    // Variables
    private String campaignId;
    private Uri imageUri;
    private String currentPhotoUrl;
    private String orgId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_campaign);

        try {
            // Initialize Firebase with null checks
            auth = FirebaseAuth.getInstance();
            currentUser = auth.getCurrentUser();

            if (currentUser == null) {
                showErrorAndFinish("Please log in to update campaigns");
                return;
            }

            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();

            // Get campaign ID from intent with proper null check
            Bundle extras = getIntent().getExtras();
            if (extras == null || !extras.containsKey("campaignId")) {
                showErrorAndFinish("No campaign selected");
                return;
            }

            campaignId = extras.getString("campaignId");
            if (campaignId == null || campaignId.isEmpty()) {
                showErrorAndFinish("Invalid campaign ID");
                return;
            }

            Log.d(TAG, "Starting update for campaign: " + campaignId + " by user: " + currentUser.getUid());

            initializeViews();
            setupSpinners();
            setupDatePickers();
            verifyUserAndLoadCampaign();
            setupButtons();

        } catch (Exception e) {
            Log.e(TAG, "Initialization error", e);
            showErrorAndFinish("App initialization failed");
            finish();
        }
    }

    private void initializeViews() {
        try {
            campaignImageView = findViewById(R.id.campaignImageView);
            titleEditText = findViewById(R.id.titleEditText);
            descriptionEditText = findViewById(R.id.descriptionEditText);
            goalEditText = findViewById(R.id.goalEditText);
            startDateEditText = findViewById(R.id.startDateEditText);
            endDateEditText = findViewById(R.id.endDateEditText);
            locationEditText = findViewById(R.id.locationEditText);
            addressEditText = findViewById(R.id.addressEditText);
            statusSpinner = findViewById(R.id.statusSpinner);
            typeSpinner = findViewById(R.id.typeSpinner);
            progressBar = findViewById(R.id.progressBar);
            updateButton = findViewById(R.id.updateButton);
            uploadImageButton = findViewById(R.id.uploadImageButton);

            progressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize views", e);
            throw new RuntimeException("View initialization failed", e);
        }
    }

    private void setupSpinners() {
        try {
            // Status Spinner
            ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.campaign_status,
                    android.R.layout.simple_spinner_item
            );
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            statusSpinner.setAdapter(statusAdapter);

            // Type Spinner
            ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.campaign_types,
                    android.R.layout.simple_spinner_item
            );
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeSpinner.setAdapter(typeAdapter);
        } catch (Exception e) {
            Log.e(TAG, "Failed to setup spinners", e);
            Toast.makeText(this, "Failed to initialize form", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDatePickers() {
        startDateEditText.setOnClickListener(v -> showDatePicker(startDateEditText));
        endDateEditText.setOnClickListener(v -> showDatePicker(endDateEditText));
    }

    private void showDatePicker(EditText editText) {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Date picker error", e);
            Toast.makeText(this, "Failed to open date picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void verifyUserAndLoadCampaign() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful() && userTask.getResult() != null) {
                        DocumentSnapshot userDoc = userTask.getResult();
                        if (userDoc.exists()) {
                            String role = userDoc.getString("role");
                            Boolean approved = userDoc.getBoolean("approved");

                            if (!"organization".equals(role) || approved == null || !approved) {
                                progressBar.setVisibility(View.GONE);
                                showErrorAndFinish("Only approved organizations can update campaigns");
                                return;
                            }

                            loadCampaignData();
                        } else {
                            progressBar.setVisibility(View.GONE);
                            showErrorAndFinish("User data not found");
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        showErrorAndFinish("Failed to verify user permissions");
                    }
                });
    }

    private void loadCampaignData() {
        db.collection("campaigns").document(campaignId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            orgId = document.getString("orgId");
                            if (orgId == null || !orgId.equals(currentUser.getUid())) {
                                showErrorAndFinish("You can only update your own campaigns");
                                return;
                            }

                            currentPhotoUrl = document.getString("photoUrl");

                            // Load image
                            if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(currentPhotoUrl)
                                        .placeholder(R.drawable.ic_placeholder_image)
                                        .error(R.drawable.ic_error_image)
                                        .into(campaignImageView);
                            }

                            // Populate fields
                            titleEditText.setText(document.getString("title"));
                            descriptionEditText.setText(document.getString("description"));
                            goalEditText.setText(String.valueOf(document.getLong("goalQuantity")));
                            startDateEditText.setText(document.getString("startDate"));
                            endDateEditText.setText(document.getString("endDate"));
                            locationEditText.setText(document.getString("location"));
                            addressEditText.setText(document.getString("address"));

                            // Set spinner selections
                            setSpinnerSelection(statusSpinner,
                                    document.getBoolean("isActive") ? "Active" : "Completed");
                            setSpinnerSelection(typeSpinner, document.getString("type"));

                        } else {
                            showErrorAndFinish("Campaign not found");
                        }
                    } else {
                        showErrorAndFinish("Failed to load campaign");
                    }
                });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null || spinner == null) return;

        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setupButtons() {
        uploadImageButton.setOnClickListener(v -> {
            try {
                pickImageFromGallery();
            } catch (Exception e) {
                Log.e(TAG, "Image picker error", e);
                Toast.makeText(this, "Failed to open image picker", Toast.LENGTH_SHORT).show();
            }
        });

        updateButton.setOnClickListener(v -> {
            try {
                validateAndUpdateCampaign();
            } catch (Exception e) {
                Log.e(TAG, "Update error", e);
                Toast.makeText(this, "Failed to process update", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                campaignImageView.setImageURI(imageUri);
            }
        }
    }

    private void validateAndUpdateCampaign() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String goalStr = goalEditText.getText().toString().trim();
        String startDate = startDateEditText.getText().toString().trim();
        String endDate = endDateEditText.getText().toString().trim();
        String location = locationEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String status = statusSpinner.getSelectedItem().toString();
        String type = typeSpinner.getSelectedItem().toString();

        // Validate inputs
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

            if (!validateDates(startDate, endDate)) {
                return;
            }

            if (imageUri != null) {
                uploadImageAndUpdateCampaign(title, description, type, goalQuantity,
                        startDate, endDate, location, address, status);
            } else {
                updateCampaign(title, description, type, goalQuantity,
                        startDate, endDate, location, address, status, currentPhotoUrl);
            }
        } catch (NumberFormatException e) {
            goalEditText.setError("Invalid number");
        }
    }

    private boolean validateDates(String startDate, String endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            if (start.after(end)) {
                Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void uploadImageAndUpdateCampaign(String title, String description, String type,
                                              int goalQuantity, String startDate, String endDate,
                                              String location, String address, String status) {
        progressBar.setVisibility(View.VISIBLE);

        if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
            StorageReference oldImageRef = storage.getReferenceFromUrl(currentPhotoUrl);
            oldImageRef.delete().addOnCompleteListener(deleteTask -> {
                if (deleteTask.isSuccessful()) {
                    uploadNewImage(title, description, type, goalQuantity,
                            startDate, endDate, location, address, status);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to remove old image", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            uploadNewImage(title, description, type, goalQuantity,
                    startDate, endDate, location, address, status);
        }
    }

    private void uploadNewImage(String title, String description, String type,
                                int goalQuantity, String startDate, String endDate,
                                String location, String address, String status) {
        String filename = "campaign_" + campaignId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference()
                .child("campaign_images/" + filename);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateCampaign(title, description, type, goalQuantity,
                                startDate, endDate, location, address, status, uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_LONG).show();
                });
    }

    private void updateCampaign(String title, String description, String type,
                                int goalQuantity, String startDate, String endDate,
                                String location, String address, String status, String photoUrl) {
        progressBar.setVisibility(View.VISIBLE);

        boolean isActive = status.equals("Active");

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("type", type);
        updates.put("goalQuantity", goalQuantity);
        updates.put("startDate", startDate);
        updates.put("endDate", endDate);
        updates.put("location", location);
        updates.put("address", address);
        updates.put("isActive", isActive);
        updates.put("photoUrl", photoUrl);
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        db.collection("campaigns").document(campaignId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Campaign updated successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException firestoreEx = (FirebaseFirestoreException) e;
                            if (firestoreEx.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                Toast.makeText(this, "Permission denied. You can only update your own campaigns.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Database error", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Update failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}