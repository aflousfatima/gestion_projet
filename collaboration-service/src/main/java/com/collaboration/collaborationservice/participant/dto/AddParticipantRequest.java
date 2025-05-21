package com.collaboration.collaborationservice.participant.dto;

public class AddParticipantRequest {
    private String userId;
    private String role;

    // Getters et Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}