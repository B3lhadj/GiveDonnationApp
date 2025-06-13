package com.example.givedonnationapp;

import com.google.firebase.firestore.PropertyName;

public class Donation {
    @PropertyName("userId") private String userId;
    @PropertyName("userRole") private String userRole;
    @PropertyName("timestamp") private long timestamp;
    @PropertyName("campaignId") private String campaignId;
    @PropertyName("category") private String category;
    @PropertyName("donationId")
    private String donationId;
    // Donor information
    @PropertyName("donorName") private String donorName;
    @PropertyName("donorPhone") private String donorPhone;
    @PropertyName("donorEmail") private String donorEmail;
    @PropertyName("donorAddress") private String donorAddress;

    // Food Donation
    @PropertyName("donorType") private String donorType;
    @PropertyName("quantity") private Integer quantity;
    @PropertyName("foodType") private String foodType;
    @PropertyName("expiryDate") private String expiryDate;
    @PropertyName("specialNotes") private String specialNotes;
    @PropertyName("pickupTime") private String pickupTime;

    // Blood Donation
    @PropertyName("bloodType") private String bloodType;
    @PropertyName("age") private Integer age;
    @PropertyName("weight") private Double weight;
    @PropertyName("lastDonationDate") private String lastDonationDate;
    @PropertyName("hasHealthProblems") private Boolean hasHealthProblems;
    @PropertyName("hasSmoked") private Boolean hasSmoked;
    @PropertyName("drinksAlcohol") private Boolean drinksAlcohol;
    @PropertyName("hasTattoo") private Boolean hasTattoo;
    @PropertyName("traveledRecently") private Boolean traveledRecently;

    // Education
    @PropertyName("materialType") private String materialType;
    @PropertyName("condition") private String condition;
    @PropertyName("description") private String description;
    @PropertyName("deliveryMethod") private String deliveryMethod;

    // Financial Aid
    @PropertyName("amount") private Double amount;
    @PropertyName("paymentMethod") private String paymentMethod;
    @PropertyName("cardLastFour") private String cardLastFour;
    @PropertyName("recurring") private Boolean recurring;

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }
    public String getDonorPhone() { return donorPhone; }
    public void setDonorPhone(String donorPhone) { this.donorPhone = donorPhone; }
    public String getDonorEmail() { return donorEmail; }
    public String getDonationId() {
        return donationId;
    }

    public void setDonationId(String donationId) {
        this.donationId = donationId;
    }
    public void setDonorEmail(String donorEmail) { this.donorEmail = donorEmail; }
    public String getDonorAddress() { return donorAddress; }
    public void setDonorAddress(String donorAddress) { this.donorAddress = donorAddress; }
    public String getDonorType() { return donorType; }
    public void setDonorType(String donorType) { this.donorType = donorType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public String getSpecialNotes() { return specialNotes; }
    public void setSpecialNotes(String specialNotes) { this.specialNotes = specialNotes; }
    public String getPickupTime() { return pickupTime; }
    public void setPickupTime(String pickupTime) { this.pickupTime = pickupTime; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public String getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(String lastDonationDate) { this.lastDonationDate = lastDonationDate; }
    public Boolean getHasHealthProblems() { return hasHealthProblems; }
    public void setHasHealthProblems(Boolean hasHealthProblems) { this.hasHealthProblems = hasHealthProblems; }
    public Boolean getHasSmoked() { return hasSmoked; }
    public void setHasSmoked(Boolean hasSmoked) { this.hasSmoked = hasSmoked; }
    public Boolean getDrinksAlcohol() { return drinksAlcohol; }
    public void setDrinksAlcohol(Boolean drinksAlcohol) { this.drinksAlcohol = drinksAlcohol; }
    public Boolean getHasTattoo() { return hasTattoo; }
    public void setHasTattoo(Boolean hasTattoo) { this.hasTattoo = hasTattoo; }
    public Boolean getTraveledRecently() { return traveledRecently; }
    public void setTraveledRecently(Boolean traveledRecently) { this.traveledRecently = traveledRecently; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }
    public Boolean getRecurring() { return recurring; }
    public void setRecurring(Boolean recurring) { this.recurring = recurring; }
}