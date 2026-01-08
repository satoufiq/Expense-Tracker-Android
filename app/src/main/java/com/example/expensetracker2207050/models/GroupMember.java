package com.example.expensetracker2207050.models;

/**
 * Model class to represent a group member with their spending stats
 */
public class GroupMember {
    private String uid;
    private String username;
    private String email;
    private double totalSpent;
    private int transactionCount;
    private boolean isAdmin;

    public GroupMember() {}

    public GroupMember(String uid, String username, String email) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.totalSpent = 0;
        this.transactionCount = 0;
        this.isAdmin = false;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}

