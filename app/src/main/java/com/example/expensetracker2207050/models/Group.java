package com.example.expensetracker2207050.models;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String id;
    private String name;
    private String adminId;
    private List<String> memberIds;

    public Group() {
        this.memberIds = new ArrayList<>();
    }

    public Group(String id, String name, String adminId) {
        this.id = id;
        this.name = name;
        this.adminId = adminId;
        this.memberIds = new ArrayList<>();
        this.memberIds.add(adminId);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
}
