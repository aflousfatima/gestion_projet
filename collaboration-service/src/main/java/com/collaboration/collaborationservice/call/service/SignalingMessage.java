package com.collaboration.collaborationservice.call.service;

import com.collaboration.collaborationservice.common.valueobjects.SdpInfo;

public class SignalingMessage {
    private String userId;
    private SdpInfo sdpInfo;
    private String type; // "offer", "answer", "ice-candidate"

    public SignalingMessage(String userId, SdpInfo sdpInfo, String type) {
        this.userId = userId;
        this.sdpInfo = sdpInfo;
        this.type = type;
    }

    // Getters et setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public SdpInfo getSdpInfo() { return sdpInfo; }
    public void setSdpInfo(SdpInfo sdpInfo) { this.sdpInfo = sdpInfo; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}