package com.example.expensetracker2207050.models;

public class Budget {
    private String id;
    private String userId;
    private String groupId; // Null for personal budget
    private double amount;
    private String type; // "PERSONAL" or "GROUP"

    public Budget() {}

    public Budget(String id, String userId, String groupId, double amount, String type) {
        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.amount = amount;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
