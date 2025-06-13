package com.example.givedonnationapp;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DonationActivity extends AppCompatActivity {

    private static final String TAG = "DonationActivity";
    private static final int MIN_BLOOD_DONOR_AGE = 18;
    private static final int MAX_BLOOD_DONOR_AGE = 65;
    private static final double MIN_FINANCIAL_DONATION = 1.0;
    private static final int MAX_FOOD_QUANTITY = 1000;
    private static final int MAX_EDUCATION_ITEMS = 500;

    // UI Components
    private LinearLayout formContainer;
    private Button submitButton;
    private ProgressBar progressBar;
    private LottieAnimationView successAnimation;

    // Form Fields - General
    private TextInputLayout nameLayout, emailLayout, phoneLayout, addressLayout;
    private TextInputEditText nameInput, emailInput, phoneInput, addressInput;

    // Food Donation Fields
    private RadioGroup donorTypeGroup, foodTypeGroup;
    private TextInputLayout quantityLayout, expiryLayout, notesLayout, pickupTimeLayout;
    private TextInputEditText quantityInput, expiryInput, notesInput, pickupTimeInput;

    // Blood Donation Fields
    private Spinner bloodTypeSpinner;
    private TextInputLayout ageLayout, weightLayout, lastDonationLayout;
    private TextInputEditText ageInput, weightInput, lastDonationInput;
    private RadioGroup healthProblemsGroup, lifestyleGroup;
    private CheckBox smokedCheckbox, alcoholCheckbox, tattooCheckbox, travelCheckbox;

    // Education Donation Fields
    private Spinner materialTypeSpinner, conditionSpinner;
    private TextInputLayout quantityLayoutEdu, descriptionLayout;
    private TextInputEditText quantityInputEdu, descriptionInput;
    private RadioGroup deliveryGroup;

    // Financial Aid Fields
    private TextInputLayout amountLayout, cardNumberLayout, expiryDateLayout, cvvLayout;
    private TextInputEditText amountInput, cardNumberInput, expiryDateInput, cvvInput;
    private Spinner paymentMethodSpinner;
    private RadioGroup recurringGroup;

    // Firebase
    private FirebaseFirestore db;
    private String campaignId, campaignTitle, category;
    private String userId;
    private String userRole;
    private boolean isApproved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);

        // Initialize Firebase and views
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            showErrorAndFinish("Please login first");
            return;
        }
        userId = currentUser.getUid();

        // Get campaign data from intent
        campaignId = getIntent().getStringExtra("campaignId");
        campaignTitle = getIntent().getStringExtra("campaignTitle");
        category = getIntent().getStringExtra("category");

        if (campaignId == null || campaignTitle == null || category == null) {
            showErrorAndFinish("Invalid campaign data");
            return;
        }

        initializeViews();
        setupToolbar();
        verifyUserRoleAndSetupForm();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        TextView titleView = findViewById(R.id.campaignTitle);
        titleView.setText(campaignTitle);
    }

    private void initializeViews() {
        formContainer = findViewById(R.id.formContainer);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);
        successAnimation = findViewById(R.id.successAnimation);

        submitButton.setOnClickListener(v -> confirmDonation());
    }
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    expiryInput.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    String selectedTime = hourOfDay + ":" + minute;
                    pickupTimeInput.setText(selectedTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }
    private void verifyUserRoleAndSetupForm() {
        showLoading(true);
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            userRole = document.getString("role");
                            isApproved = Boolean.TRUE.equals(document.getBoolean("approved"));

                            if (userRole == null) {
                                showErrorAndFinish("User role not defined");
                                return;
                            }

                            if (userRole.equals("organization") && !isApproved) {
                                showErrorAndFinish("Your organization account is not approved yet");
                                return;
                            }

                            if (canUserDonate()) {
                                setupForm();
                            } else {
                                showErrorAndFinish(getDonationPermissionError());
                            }
                        } else {
                            showErrorAndFinish("User data not found");
                        }
                    } else {
                        showError("Failed to verify user role: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                        Log.e(TAG, "Error verifying role", task.getException());
                        finish();
                    }
                });
    }

    private boolean canUserDonate() {
        return userRole.equals("user") ||
                (userRole.equals("organization") && category.equals("Food Donation"));
    }

    private String getDonationPermissionError() {
        return userRole.equals("organization") ?
                "Organizations can only donate to food campaigns" :
                "You don't have permission to donate to this campaign";
    }

    private void setupForm() {
        formContainer.removeAllViews();

        // Add header
        addSectionTitle("Donation Details");

        switch (category) {
            case "Food Donation":
                setupFoodDonationForm();
                break;
            case "Blood Donation":
                setupBloodDonationForm();
                break;
            case "Education":
                setupEducationDonationForm();
                break;
            case "Financial Aid":
                setupFinancialAidForm();
                break;
            default:
                showErrorAndFinish("Unsupported campaign type");
        }

        // Add donor information section for all types
        addDonorInformationSection();
    }

    private void addSectionTitle(String title) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(title);
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        sectionTitle.setTextColor(ContextCompat.getColor(this, R.color.primaryColor));
        sectionTitle.setTypeface(null, Typeface.BOLD);
        sectionTitle.setPadding(0, 16, 0, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 8);
        sectionTitle.setLayoutParams(params);

        formContainer.addView(sectionTitle);
    }

    private void setupFoodDonationForm() {
        // Donor Type
        addSubsectionTitle("Donor Type");
        donorTypeGroup = new RadioGroup(this);
        donorTypeGroup.setOrientation(LinearLayout.VERTICAL);

        RadioButton restaurantRadio = createRadioButton("Restaurant/Food Business");
        RadioButton individualRadio = createRadioButton("Individual Donor");

        donorTypeGroup.addView(restaurantRadio);
        donorTypeGroup.addView(individualRadio);
        formContainer.addView(donorTypeGroup);

        // Food Type
        addSubsectionTitle("Food Type");
        foodTypeGroup = new RadioGroup(this);
        foodTypeGroup.setOrientation(LinearLayout.VERTICAL);

        RadioButton cookedRadio = createRadioButton("Cooked/Prepared Food");
        RadioButton packagedRadio = createRadioButton("Packaged/Non-perishable");
        RadioButton rawRadio = createRadioButton("Raw Ingredients");

        foodTypeGroup.addView(cookedRadio);
        foodTypeGroup.addView(packagedRadio);
        foodTypeGroup.addView(rawRadio);
        formContainer.addView(foodTypeGroup);

        // Food Details
        addSubsectionTitle("Food Details");

        // Quantity
        quantityLayout = createTextInputLayout("Quantity (meals/packages)");
        quantityInput = new TextInputEditText(this);
        quantityInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantityLayout.addView(quantityInput);
        formContainer.addView(quantityLayout);
        addRealTimeValidation(quantityInput, quantityLayout, this::validateFoodDonation);

        // Expiry Date
        expiryLayout = createTextInputLayout("Expiry Date (if applicable)");
        expiryInput = new TextInputEditText(this);
        expiryInput.setFocusable(false);
        expiryInput.setOnClickListener(v -> showDatePicker());
        expiryLayout.addView(expiryInput);
        formContainer.addView(expiryLayout);

        // Special Notes
        notesLayout = createTextInputLayout("Special Notes (allergies, preparation, etc.)");
        notesInput = new TextInputEditText(this);
        notesInput.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        notesInput.setMinLines(3);
        notesLayout.addView(notesInput);
        formContainer.addView(notesLayout);

        // Pickup Information
        addSubsectionTitle("Pickup Information");

        // Preferred Pickup Time
        pickupTimeLayout = createTextInputLayout("Preferred Pickup Time");
        pickupTimeInput = new TextInputEditText(this);
        pickupTimeInput.setFocusable(false);
        pickupTimeInput.setOnClickListener(v -> showTimePicker());
        pickupTimeLayout.addView(pickupTimeInput);
        formContainer.addView(pickupTimeLayout);
    }

    private void setupBloodDonationForm() {
        // Donor Information
        addSubsectionTitle("Donor Information");

        // Blood Type
        bloodTypeSpinner = createSpinner(new String[]{
                "Select Blood Type", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
        });
        formContainer.addView(bloodTypeSpinner);

        // Age
        ageLayout = createTextInputLayout("Age");
        ageInput = new TextInputEditText(this);
        ageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        ageLayout.addView(ageInput);
        formContainer.addView(ageLayout);
        addRealTimeValidation(ageInput, ageLayout, this::validateBloodDonation);

        // Weight
        weightLayout = createTextInputLayout("Weight (kg)");
        weightInput = new TextInputEditText(this);
        weightInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        weightLayout.addView(weightInput);
        formContainer.addView(weightLayout);

        // Health Questions
        addSubsectionTitle("Health Questionnaire");

        // Last Donation Date
        lastDonationLayout = createTextInputLayout("Last Donation Date (if any)");
        lastDonationInput = new TextInputEditText(this);
        lastDonationInput.setFocusable(false);
        lastDonationInput.setOnClickListener(v -> showDatePicker());
        lastDonationLayout.addView(lastDonationInput);
        formContainer.addView(lastDonationLayout);

        // Health Problems
        healthProblemsGroup = new RadioGroup(this);
        healthProblemsGroup.setOrientation(LinearLayout.VERTICAL);

        TextView healthQuestion = new TextView(this);
        healthQuestion.setText("Do you have any health problems?");
        healthQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        healthQuestion.setPadding(0, 8, 0, 8);

        RadioButton healthYesRadio = createRadioButton("Yes");
        RadioButton healthNoRadio = createRadioButton("No");

        healthProblemsGroup.addView(healthQuestion);
        healthProblemsGroup.addView(healthYesRadio);
        healthProblemsGroup.addView(healthNoRadio);
        formContainer.addView(healthProblemsGroup);

        // Lifestyle Questions
        lifestyleGroup = new RadioGroup(this);
        lifestyleGroup.setOrientation(LinearLayout.VERTICAL);

        TextView lifestyleQuestion = new TextView(this);
        lifestyleQuestion.setText("In the last 6 months, have you:");
        lifestyleQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        lifestyleQuestion.setPadding(0, 16, 0, 8);

        smokedCheckbox = createCheckBox("Smoked or used tobacco products");
        alcoholCheckbox = createCheckBox("Consumed alcohol regularly");
        tattooCheckbox = createCheckBox("Gotten a tattoo or piercing");
        travelCheckbox = createCheckBox("Traveled to high-risk areas");

        lifestyleGroup.addView(lifestyleQuestion);
        lifestyleGroup.addView(smokedCheckbox);
        lifestyleGroup.addView(alcoholCheckbox);
        lifestyleGroup.addView(tattooCheckbox);
        lifestyleGroup.addView(travelCheckbox);
        formContainer.addView(lifestyleGroup);
    }

    private void setupEducationDonationForm() {
        // Donation Type
        addSubsectionTitle("Donation Type");
        materialTypeSpinner = createSpinner(new String[]{
                "Select Item Type", "Books", "Stationery", "Electronics", "School Uniforms", "Other"
        });
        formContainer.addView(materialTypeSpinner);

        // Item Details
        addSubsectionTitle("Item Details");

        // Quantity
        quantityLayoutEdu = createTextInputLayout("Quantity");
        quantityInputEdu = new TextInputEditText(this);
        quantityInputEdu.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantityLayoutEdu.addView(quantityInputEdu);
        formContainer.addView(quantityLayoutEdu);
        addRealTimeValidation(quantityInputEdu, quantityLayoutEdu, this::validateEducationDonation);

        // Condition
        conditionSpinner = createSpinner(new String[]{
                "Select Condition", "New", "Like New", "Good", "Fair", "Needs Repair"
        });
        formContainer.addView(conditionSpinner);

        // Description
        descriptionLayout = createTextInputLayout("Description (titles, subjects, etc.)");
        descriptionInput = new TextInputEditText(this);
        descriptionInput.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        descriptionInput.setMinLines(3);
        descriptionLayout.addView(descriptionInput);
        formContainer.addView(descriptionLayout);

        // Delivery Options
        addSubsectionTitle("Delivery Options");

        deliveryGroup = new RadioGroup(this);
        deliveryGroup.setOrientation(LinearLayout.VERTICAL);

        RadioButton pickupRadio = createRadioButton("I can deliver to the organization");
        RadioButton collectRadio = createRadioButton("Organization can collect from me");
        RadioButton shippingRadio = createRadioButton("I will ship the items");

        deliveryGroup.addView(pickupRadio);
        deliveryGroup.addView(collectRadio);
        deliveryGroup.addView(shippingRadio);
        formContainer.addView(deliveryGroup);
    }

    private void setupFinancialAidForm() {
        // Payment Information
        addSubsectionTitle("Payment Information");

        // Amount
        amountLayout = createTextInputLayout("Donation Amount ($)");
        amountInput = new TextInputEditText(this);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountLayout.addView(amountInput);
        formContainer.addView(amountLayout);
        addRealTimeValidation(amountInput, amountLayout, this::validateFinancialAid);

        // Payment Method
        paymentMethodSpinner = createSpinner(new String[]{
                "Select Payment Method", "Credit Card", "PayPal", "Bank Transfer", "Mobile Payment"
        });
        formContainer.addView(paymentMethodSpinner);

        // Credit Card Fields (shown when credit card is selected)
        cardNumberLayout = createTextInputLayout("Card Number");
        cardNumberInput = new TextInputEditText(this);
        cardNumberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        cardNumberLayout.setVisibility(View.GONE);
        cardNumberLayout.addView(cardNumberInput);
        formContainer.addView(cardNumberLayout);

        LinearLayout cardDetailsRow = new LinearLayout(this);
        cardDetailsRow.setOrientation(LinearLayout.HORIZONTAL);
        cardDetailsRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Expiry Date
        expiryDateLayout = createTextInputLayout("MM/YY");
        expiryDateLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        expiryDateInput = new TextInputEditText(this);
        expiryDateInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        expiryDateLayout.addView(expiryDateInput);
        expiryDateLayout.setVisibility(View.GONE);

        // CVV
        cvvLayout = createTextInputLayout("CVV");
        cvvLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        cvvInput = new TextInputEditText(this);
        cvvInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        cvvLayout.addView(cvvInput);
        cvvLayout.setVisibility(View.GONE);

        cardDetailsRow.addView(expiryDateLayout);
        cardDetailsRow.addView(cvvLayout);
        formContainer.addView(cardDetailsRow);

        // Payment method listener
        paymentMethodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean showCardFields = position == 1; // Credit Card selected
                cardNumberLayout.setVisibility(showCardFields ? View.VISIBLE : View.GONE);
                expiryDateLayout.setVisibility(showCardFields ? View.VISIBLE : View.GONE);
                cvvLayout.setVisibility(showCardFields ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Recurring Donation
        addSubsectionTitle("Recurring Donation");

        recurringGroup = new RadioGroup(this);
        recurringGroup.setOrientation(LinearLayout.VERTICAL);

        RadioButton oneTimeRadio = createRadioButton("One-time donation");
        RadioButton monthlyRadio = createRadioButton("Monthly recurring donation");

        recurringGroup.addView(oneTimeRadio);
        recurringGroup.addView(monthlyRadio);
        formContainer.addView(recurringGroup);
    }

    private void addDonorInformationSection() {
        addSectionTitle("Your Information");

        // Name
        nameLayout = createTextInputLayout("Full Name");
        nameInput = new TextInputEditText(this);
        nameInput.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        nameLayout.addView(nameInput);
        formContainer.addView(nameLayout);

        // Email
        emailLayout = createTextInputLayout("Email");
        emailInput = new TextInputEditText(this);
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailLayout.addView(emailInput);
        formContainer.addView(emailLayout);

        // Phone
        phoneLayout = createTextInputLayout("Phone Number");
        phoneInput = new TextInputEditText(this);
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneLayout.addView(phoneInput);
        formContainer.addView(phoneLayout);

        // Address (for physical donations)
        if (!category.equals("Financial Aid")) {
            addressLayout = createTextInputLayout("Address (for pickup/delivery)");
            addressInput = new TextInputEditText(this);
            addressInput.setInputType(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
            addressLayout.addView(addressInput);
            formContainer.addView(addressLayout);
        }
    }

    // Helper methods for creating form elements
    private TextInputLayout createTextInputLayout(String hint) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(hint);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(8, 8, 8, 8);
        layout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.primaryColor));
        layout.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 16);
        layout.setLayoutParams(params);

        return layout;
    }

    private Spinner createSpinner(String[] items) {
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        @SuppressLint("ResourceType") ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.drawable.spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 16);
        spinner.setLayoutParams(params);

        return spinner;
    }

    private RadioButton createRadioButton(String text) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(text);
        radioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        radioButton.setPadding(8, 16, 8, 16);
        return radioButton;
    }

    private CheckBox createCheckBox(String text) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        checkBox.setPadding(8, 8, 8, 8);
        return checkBox;
    }

    private void addSubsectionTitle(String title) {
        TextView subsectionTitle = new TextView(this);
        subsectionTitle.setText(title);
        subsectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        subsectionTitle.setTypeface(null, Typeface.BOLD);
        subsectionTitle.setPadding(0, 8, 0, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        subsectionTitle.setLayoutParams(params);

        formContainer.addView(subsectionTitle);
    }

    private void addRealTimeValidation(TextInputEditText input, TextInputLayout layout, Runnable validator) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validator.run();
            }
        });
    }

    private void confirmDonation() {
        if (!isNetworkAvailable()) {
            showError("No internet connection");
            return;
        }

        if (!validateDonationForm()) {
            animateError();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Donation")
                .setMessage("Are you sure you want to submit this donation?")
                .setPositiveButton("Yes", (dialog, which) -> submitDonation())
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_donate)
                .show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void submitDonation() {
        Donation donation = createDonationObject();
        if (donation == null) {
            Log.e(TAG, "Failed to create donation object");
            showError("Invalid donation data");
            return;
        }

        String donationId = UUID.randomUUID().toString();
        donation.setDonationId(donationId);
        donation.setCampaignId(campaignId);

        Log.d(TAG, "Attempting to save donation: " + new Gson().toJson(donation));

        showLoading(true);

        // First save the donation document
        db.collection("donations").document(donationId)
                .set(donation)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Donation saved successfully");
                    updateCampaign(donation);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error saving donation", e);
                    showError("Failed to save donation: " + e.getMessage());
                });
    }

    private void updateCampaign(Donation donation) {
        Map<String, Object> updates = new HashMap<>();

        if (category.equals("Financial Aid") && donation.getAmount() != null) {
            updates.put("currentAmount", FieldValue.increment(donation.getAmount()));
        } else if (donation.getQuantity() != null) {
            updates.put("currentQuantity", FieldValue.increment(donation.getQuantity()));
        }

        if (updates.isEmpty()) {
            showSuccess();
            return;
        }

        db.collection("campaigns").document(campaignId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Campaign updated successfully");
                    showSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating campaign", e);
                    showError("Donation saved but campaign update failed: " + e.getMessage());
                    showLoading(false);
                });
    }

    private void showSuccess() {
        showLoading(false);
        showSuccessAnimation();
        Toast.makeText(this, "Donation saved successfully!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
    }

    private Donation createDonationObject() {
        Donation donation = new Donation();
        donation.setUserId(userId);
        donation.setUserRole(userRole);
        donation.setTimestamp(System.currentTimeMillis());
        donation.setCategory(category);

        // Set donor information
        donation.setDonorName(nameInput.getText().toString());
        donation.setDonorEmail(emailInput.getText().toString());
        donation.setDonorPhone(phoneInput.getText().toString());
        if (addressInput != null) {
            donation.setDonorAddress(addressInput.getText().toString());
        }

        try {
            switch (category) {
                case "Food Donation":
                    if (donorTypeGroup.getCheckedRadioButtonId() == -1 ||
                            foodTypeGroup.getCheckedRadioButtonId() == -1 ||
                            quantityInput.getText() == null) {
                        return null;
                    }

                    RadioButton selectedDonorType = findViewById(donorTypeGroup.getCheckedRadioButtonId());
                    RadioButton selectedFoodType = findViewById(foodTypeGroup.getCheckedRadioButtonId());

                    donation.setDonorType(selectedDonorType.getText().toString());
                    donation.setFoodType(selectedFoodType.getText().toString());
                    donation.setQuantity(Integer.parseInt(quantityInput.getText().toString()));
                    donation.setExpiryDate(expiryInput.getText().toString());
                    donation.setSpecialNotes(notesInput.getText().toString());
                    donation.setPickupTime(pickupTimeInput.getText().toString());
                    break;

                case "Blood Donation":
                    if (bloodTypeSpinner.getSelectedItemPosition() == 0 ||
                            ageInput.getText() == null ||
                            healthProblemsGroup.getCheckedRadioButtonId() == -1) {
                        return null;
                    }

                    donation.setBloodType(bloodTypeSpinner.getSelectedItem().toString());
                    donation.setAge(Integer.parseInt(ageInput.getText().toString()));
                    donation.setWeight(Double.parseDouble(weightInput.getText().toString()));
                    donation.setLastDonationDate(lastDonationInput.getText().toString());

                    RadioButton selectedHealthStatus = findViewById(healthProblemsGroup.getCheckedRadioButtonId());
                    donation.setHasHealthProblems(selectedHealthStatus.getText().equals("Yes"));
                    donation.setHasSmoked(smokedCheckbox.isChecked());
                    donation.setDrinksAlcohol(alcoholCheckbox.isChecked());
                    donation.setHasTattoo(tattooCheckbox.isChecked());
                    donation.setTraveledRecently(travelCheckbox.isChecked());
                    donation.setQuantity(1); // Blood donation is always quantity 1
                    break;

                case "Education":
                    if (materialTypeSpinner.getSelectedItemPosition() == 0 ||
                            conditionSpinner.getSelectedItemPosition() == 0 ||
                            quantityInputEdu.getText() == null) {
                        return null;
                    }

                    donation.setMaterialType(materialTypeSpinner.getSelectedItem().toString());
                    donation.setCondition(conditionSpinner.getSelectedItem().toString());
                    donation.setQuantity(Integer.parseInt(quantityInputEdu.getText().toString()));
                    donation.setDescription(descriptionInput.getText().toString());

                    RadioButton selectedDelivery = findViewById(deliveryGroup.getCheckedRadioButtonId());
                    donation.setDeliveryMethod(selectedDelivery.getText().toString());
                    break;

                case "Financial Aid":
                    if (amountInput.getText() == null ||
                            paymentMethodSpinner.getSelectedItemPosition() == 0) {
                        return null;
                    }

                    donation.setAmount(Double.parseDouble(amountInput.getText().toString()));
                    donation.setPaymentMethod(paymentMethodSpinner.getSelectedItem().toString());

                    if (paymentMethodSpinner.getSelectedItemPosition() == 1) { // Credit Card
                        donation.setCardLastFour(cardNumberInput.getText().toString().length() > 4 ?
                                cardNumberInput.getText().toString().substring(cardNumberInput.getText().toString().length() - 4) :
                                cardNumberInput.getText().toString());
                    }

                    RadioButton selectedRecurring = findViewById(recurringGroup.getCheckedRadioButtonId());
                    donation.setRecurring(selectedRecurring.getText().equals("Monthly recurring donation"));
                    break;

                default:
                    return null;
            }
            return donation;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Number parsing error", e);
            return null;
        }
    }

    private boolean validateDonationForm() {
        // Validate donor information first
        if (nameInput.getText() == null || nameInput.getText().toString().trim().isEmpty()) {
            nameLayout.setError("Name is required");
            return false;
        }
        nameLayout.setError(null);

        if (emailInput.getText() == null || emailInput.getText().toString().trim().isEmpty()) {
            emailLayout.setError("Email is required");
            return false;
        }
        emailLayout.setError(null);

        if (phoneInput.getText() == null || phoneInput.getText().toString().trim().isEmpty()) {
            phoneLayout.setError("Phone number is required");
            return false;
        }
        phoneLayout.setError(null);

        if (addressInput != null && (addressInput.getText() == null || addressInput.getText().toString().trim().isEmpty())) {
            addressLayout.setError("Address is required");
            return false;
        }
        if (addressInput != null) {
            addressLayout.setError(null);
        }

        // Validate category-specific fields
        switch (category) {
            case "Food Donation":
                return validateFoodDonation();
            case "Blood Donation":
                return validateBloodDonation();
            case "Education":
                return validateEducationDonation();
            case "Financial Aid":
                return validateFinancialAid();
            default:
                return false;
        }
    }

    private boolean validateFoodDonation() {
        if (donorTypeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select donor type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (foodTypeGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select food type", Toast.LENGTH_SHORT).show();
            return false;
        }

        String quantityStr = quantityInput.getText() != null ? quantityInput.getText().toString().trim() : "";
        if (quantityStr.isEmpty()) {
            quantityLayout.setError("Quantity is required");
            return false;
        }
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                quantityLayout.setError("Enter a positive number");
                return false;
            }
            if (quantity > MAX_FOOD_QUANTITY) {
                quantityLayout.setError("Maximum " + MAX_FOOD_QUANTITY + " meals allowed");
                return false;
            }
        } catch (NumberFormatException e) {
            quantityLayout.setError("Enter a valid number");
            return false;
        }
        quantityLayout.setError(null);

        return true;
    }

    private boolean validateBloodDonation() {
        if (bloodTypeSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select blood type", Toast.LENGTH_SHORT).show();
            return false;
        }

        String ageStr = ageInput.getText() != null ? ageInput.getText().toString().trim() : "";
        if (ageStr.isEmpty()) {
            ageLayout.setError("Age is required");
            return false;
        }
        try {
            int age = Integer.parseInt(ageStr);
            if (age < MIN_BLOOD_DONOR_AGE || age > MAX_BLOOD_DONOR_AGE) {
                ageLayout.setError("Must be " + MIN_BLOOD_DONOR_AGE + "-" + MAX_BLOOD_DONOR_AGE + " years");
                return false;
            }
        } catch (NumberFormatException e) {
            ageLayout.setError("Enter a valid age");
            return false;
        }
        ageLayout.setError(null);

        String weightStr = weightInput.getText() != null ? weightInput.getText().toString().trim() : "";
        if (weightStr.isEmpty()) {
            weightLayout.setError("Weight is required");
            return false;
        }
        try {
            double weight = Double.parseDouble(weightStr);
            if (weight < 50) {
                weightLayout.setError("Minimum weight is 50kg");
                return false;
            }
        } catch (NumberFormatException e) {
            weightLayout.setError("Enter a valid weight");
            return false;
        }
        weightLayout.setError(null);

        if (healthProblemsGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please answer health question", Toast.LENGTH_SHORT).show();
            return false;
        }

        RadioButton selectedHealthStatus = findViewById(healthProblemsGroup.getCheckedRadioButtonId());
        if (selectedHealthStatus.getText().equals("Yes")) {
            Toast.makeText(this, "You cannot donate blood with health problems", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private boolean validateEducationDonation() {
        if (materialTypeSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select item type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (conditionSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select condition", Toast.LENGTH_SHORT).show();
            return false;
        }

        String quantityStr = quantityInputEdu.getText() != null ? quantityInputEdu.getText().toString().trim() : "";
        if (quantityStr.isEmpty()) {
            quantityLayoutEdu.setError("Quantity is required");
            return false;
        }
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                quantityLayoutEdu.setError("Enter a positive number");
                return false;
            }
            if (quantity > MAX_EDUCATION_ITEMS) {
                quantityLayoutEdu.setError("Maximum " + MAX_EDUCATION_ITEMS + " items allowed");
                return false;
            }
        } catch (NumberFormatException e) {
            quantityLayoutEdu.setError("Enter a valid number");
            return false;
        }
        quantityLayoutEdu.setError(null);

        if (deliveryGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select delivery option", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validateFinancialAid() {
        String amountStr = amountInput.getText() != null ? amountInput.getText().toString().trim() : "";
        if (amountStr.isEmpty()) {
            amountLayout.setError("Amount is required");
            return false;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount < MIN_FINANCIAL_DONATION) {
                amountLayout.setError("Minimum donation is $" + MIN_FINANCIAL_DONATION);
                return false;
            }
            if (amount > 10000) {
                amountLayout.setError("Maximum donation is $10,000");
                return false;
            }
        } catch (NumberFormatException e) {
            amountLayout.setError("Enter a valid amount");
            return false;
        }
        amountLayout.setError(null);

        if (paymentMethodSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select payment method", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (paymentMethodSpinner.getSelectedItemPosition() == 1) { // Credit Card
            if (cardNumberInput.getText() == null || cardNumberInput.getText().toString().trim().length() < 16) {
                cardNumberLayout.setError("Enter valid card number");
                return false;
            }
            cardNumberLayout.setError(null);

            if (expiryDateInput.getText() == null || expiryDateInput.getText().toString().trim().length() != 4) {
                expiryDateLayout.setError("Enter valid expiry date (MMYY)");
                return false;
            }
            expiryDateLayout.setError(null);

            if (cvvInput.getText() == null || cvvInput.getText().toString().trim().length() < 3) {
                cvvLayout.setError("Enter valid CVV");
                return false;
            }
            cvvLayout.setError(null);
        }

        if (recurringGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select donation type", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!isLoading);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        showLoading(false);
    }

    private void showErrorAndFinish(String message) {
        showError(message);
        finish();
    }

    private void animateError() {
        ObjectAnimator shake = ObjectAnimator.ofFloat(formContainer, "translationX", 0, 25, -25, 25, -25, 15, -15, 0);
        shake.setDuration(600);
        shake.setInterpolator(new DecelerateInterpolator());
        shake.start();
    }

    private void showSuccessAnimation() {
        successAnimation.setVisibility(View.VISIBLE);
        successAnimation.playAnimation();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            successAnimation.setVisibility(View.GONE);
        }, 2000);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}