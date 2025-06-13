package com.example.givedonnationapp;

public class DonationCategory {
    private int iconResId;
    private String name;

    public DonationCategory(int iconResId, String name) {
        this.iconResId = iconResId;
        this.name = name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getName() {
        return name;
    }
}