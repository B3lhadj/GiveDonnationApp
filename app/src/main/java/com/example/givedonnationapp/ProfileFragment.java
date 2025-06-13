package com.example.givedonnationapp;

import android.content.Intent; import android.net.Uri; import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup; import android.widget.Button; import android.widget.ProgressBar; import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; import androidx.activity.result.contract.ActivityResultContracts; import androidx.annotation.NonNull; import androidx.annotation.Nullable; import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide; import com.google.android.material.textfield.TextInputEditText; import com.google.android.material.textfield.TextInputLayout; import com.google.firebase.auth.AuthCredential; import com.google.firebase.auth.EmailAuthProvider; import com.google.firebase.auth.FirebaseAuth; import com.google.firebase.auth.FirebaseUser; import com.google.firebase.firestore.FirebaseFirestore; import com.google.firebase.storage.FirebaseStorage; import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImage;
    private TextInputEditText etFullName, etEmail, etPhone;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private Button btnSave;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image);
        etFullName = view.findViewById(R.id.et_full_name);
        etEmail = view.findViewById(R.id.et_email);
        etPhone = view.findViewById(R.id.et_phone);
        etCurrentPassword = view.findViewById(R.id.et_current_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        tilCurrentPassword = view.findViewById(R.id.til_current_password);
        tilNewPassword = view.findViewById(R.id.til_new_password);
        tilConfirmPassword = view.findViewById(R.id.til_confirm_password);
        btnSave = view.findViewById(R.id.btn_save);
        progressBar = view.findViewById(R.id.progress_bar);

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                profileImage.setImageURI(imageUri);
                uploadProfileImage(imageUri);
            }
        });

        // Set click listeners
        view.findViewById(R.id.btn_change_photo).setOnClickListener(v -> changeProfilePhoto());
        btnSave.setOnClickListener(v -> saveChanges());

        // Load user data
        loadUserData();

        return view;
    }

    private void loadUserData() {
        if (currentUser != null) {
            // Load basic auth info
            etEmail.setText(currentUser.getEmail());

            // Load additional user data from Firestore
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            etFullName.setText(documentSnapshot.getString("name"));
                            etPhone.setText(documentSnapshot.getString("phone"));

                            // Load profile image if available
                            String photoUrl = documentSnapshot.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(photoUrl)
                                        .placeholder(R.drawable.ic_profile)
                                        .into(profileImage);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void changeProfilePhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri imageUri) {
        if (currentUser == null) return;

        progressBar.setVisibility(View.VISIBLE);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + currentUser.getUid() + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateProfileImageUrl(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileImageUrl(String imageUrl) {
        db.collection("users").document(currentUser.getUid())
                .update("photoUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Profile image updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveChanges() {
        if (!validateInputs()) return;

        progressBar.setVisibility(View.VISIBLE);
        String newPassword = etNewPassword.getText().toString().trim();

        if (!newPassword.isEmpty()) {
            // Update password if changed
            updatePassword(newPassword);
        } else {
            // Just update profile info
            updateProfileInfo();
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate current password if changing password
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!newPassword.isEmpty()) {
            if (currentPassword.isEmpty()) {
                tilCurrentPassword.setError("Enter current password");
                isValid = false;
            } else {
                tilCurrentPassword.setError(null);
            }

            if (newPassword.length() < 6) {
                tilNewPassword.setError("Password must be at least 6 characters");
                isValid = false;
            } else {
                tilNewPassword.setError(null);
            }

            if (!confirmPassword.equals(newPassword)) {
                tilConfirmPassword.setError("Passwords don't match");
                isValid = false;
            } else {
                tilConfirmPassword.setError(null);
            }
        }

        return isValid;
    }

    private void updatePassword(String newPassword) {
        AuthCredential credential = EmailAuthProvider
                .getCredential(currentUser.getEmail(), etCurrentPassword.getText().toString().trim());

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                updateProfileInfo();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(requireContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Authentication failed. Wrong password?", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileInfo() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", etFullName.getText().toString().trim());
        updates.put("phone", etPhone.getText().toString().trim());

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

}