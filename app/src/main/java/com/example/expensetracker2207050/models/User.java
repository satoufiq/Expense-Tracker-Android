package com.example.expensetracker2207050.models;

public class User {
    private String uid;
    private String username;
    private String email;
    private String accountType; // "Normal" or "Parent"
    private String linkedParentUid; // For child accounts linked to a parent

    public User() {}

    public User(String uid, String username, String email, String accountType) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.accountType = accountType;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getLinkedParentUid() { return linkedParentUid; }
    public void setLinkedParentUid(String linkedParentUid) { this.linkedParentUid = linkedParentUid; }
}
