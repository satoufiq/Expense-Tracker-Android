package com.example.expensetracker2207050.models;

public class Invitation {
    private String id;
    private String senderId;
    private String receiverEmail;
    private String targetId; // Group ID or Parent ID
    private String type; // "GROUP" or "PARENT"
    private String status; // "PENDING", "ACCEPTED", "REJECTED"

    public Invitation() {}

    public Invitation(String id, String senderId, String receiverEmail, String targetId, String type) {
        this.id = id;
        this.senderId = senderId;
        this.receiverEmail = receiverEmail;
        this.targetId = targetId;
        this.type = type;
        this.status = "PENDING";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
