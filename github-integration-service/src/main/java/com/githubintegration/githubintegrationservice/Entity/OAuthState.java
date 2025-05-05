package com.githubintegration.githubintegrationservice.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OAuthState {
    @Id
    private String state;
    private String userId;

    // Getters et setters
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}