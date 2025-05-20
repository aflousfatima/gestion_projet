package com.collaboration.collaborationservice.participant.dto;

public class PresenceMessage {
    private String userId;
    private boolean online;

    public PresenceMessage(String userId, boolean online) {
        this.userId = userId;
        this.online = online;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}