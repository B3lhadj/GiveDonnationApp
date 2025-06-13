package com.example.givedonnationapp;

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.webkit.URLUtil;
import java.util.Date;

public class Campaign {
    private String id;
    private String title;
    private String description;
    private String type;
    private String orgId;
    private String orgName;
    private int goalQuantity;
    private int currentQuantity;
    private String dateCreated;
    private String startDate;
    private String endDate;
    private String photoUrl;
    private String location;
    private String address;
    private boolean isActive;

    // Empty constructor required for Firestore
    public Campaign() {
        this.currentQuantity = 0;
        this.isActive = true;
        this.dateCreated = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault()).format(new Date());
    }

    // Getters
    public String getId() { return id; }
    @NonNull public String getTitle() { return title != null ? title : ""; }
    public String getDescription() { return description != null ? description : ""; }
    @NonNull public String getType() { return type != null ? type : ""; }
    public String getOrgId() { return orgId; }
    public String getOrgName() { return orgName != null ? orgName : ""; }
    public int getGoalQuantity() { return goalQuantity; }
    public int getCurrentQuantity() { return currentQuantity; }
    public String getDateCreated() { return dateCreated; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getPhotoUrl() { return photoUrl; }
    public String getLocation() { return location != null ? location : ""; }
    public String getAddress() { return address != null ? address : ""; }
    public boolean isActive() { return isActive; }

    // Setters with validation
    public void setId(String id) { this.id = id; }

    public void setTitle(String title) {
        if (TextUtils.isEmpty(title)) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(String type) {
        if (TextUtils.isEmpty(type)) {
            throw new IllegalArgumentException("Type cannot be empty");
        }
        this.type = type;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public void setGoalQuantity(int goalQuantity) {
        if (goalQuantity < 0) {
            throw new IllegalArgumentException("Goal quantity cannot be negative");
        }
        this.goalQuantity = goalQuantity;
    }

    public void setCurrentQuantity(int currentQuantity) {
        if (currentQuantity < 0) {
            throw new IllegalArgumentException("Current quantity cannot be negative");
        }
        this.currentQuantity = currentQuantity;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setPhotoUrl(String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty() && !URLUtil.isValidUrl(photoUrl)) {
            throw new IllegalArgumentException("Invalid URL format");
        }
        this.photoUrl = photoUrl;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}