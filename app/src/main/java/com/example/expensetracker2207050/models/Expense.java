package com.example.expensetracker2207050.models;

import java.util.Date;

public class Expense {
    private String id;
    private String userId;
    private String groupId; // Null if personal
    private double amount;
    private String category;
    private Date date;
    private String description;
    private String contributorId; // For group expenses

    public Expense() {}

    public Expense(String id, String userId, double amount, String category, Date date, String description) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContributorId() { return contributorId; }
    public void setContributorId(String contributorId) { this.contributorId = contributorId; }
}
