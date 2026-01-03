package com.example.expensetracker2207050.models;

import java.util.Date;

public class Alert {
    private String id;
    private String userId;
    private String title;
    private String message;
    private String type; // "BUDGET", "SUGGESTION", "INVITATION"
    private Date timestamp;
    private boolean seen;

    public Alert() {}

    public Alert(String id, String userId, String title, String message, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = new Date();
        this.seen = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }
}
