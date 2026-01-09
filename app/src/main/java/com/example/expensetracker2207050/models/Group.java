package com.example.expensetracker2207050.models;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String id;
    private String name;
    private String adminId; // Original creator - kept for backward compatibility
    private List<String> adminIds; // List of all admin user IDs
    private List<String> memberIds;

    public Group() {
        this.memberIds = new ArrayList<>();
        this.adminIds = new ArrayList<>();
    }

    public Group(String id, String name, String adminId) {
        this.id = id;
        this.name = name;
        this.adminId = adminId;
        this.memberIds = new ArrayList<>();
        this.memberIds.add(adminId);
        this.adminIds = new ArrayList<>();
        this.adminIds.add(adminId); // Creator is automatically admin
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public List<String> getAdminIds() {
        if (adminIds == null || adminIds.isEmpty()) {
            // Backward compatibility: if adminIds is empty, use adminId
            adminIds = new ArrayList<>();
            if (adminId != null) {
                adminIds.add(adminId);
            }
        }
        return adminIds;
    }
    public void setAdminIds(List<String> adminIds) { this.adminIds = adminIds; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    // Helper method to check if a user is admin
    public boolean isAdmin(String userId) {
        if (adminIds != null && adminIds.contains(userId)) {
            return true;
        }
        // Backward compatibility
        return userId != null && userId.equals(adminId);
    }
}
