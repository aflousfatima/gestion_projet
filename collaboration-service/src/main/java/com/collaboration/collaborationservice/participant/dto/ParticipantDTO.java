package com.collaboration.collaborationservice.participant.dto;

import java.time.LocalDateTime;


public class ParticipantDTO {
    private Long id;
    private String userId;
    private String firstName;
    private String lastName;
    private String role;
    private LocalDateTime joinedAt;
    private String status;

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}