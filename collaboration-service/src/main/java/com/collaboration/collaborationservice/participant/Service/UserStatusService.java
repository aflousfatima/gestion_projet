package com.collaboration.collaborationservice.participant.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStatusService {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, String> userStatuses = new ConcurrentHashMap<>();

    @Autowired
    public UserStatusService(@Lazy SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void setUserOnline(String userId) {
        userStatuses.put(userId, "ONLINE");
        broadcastStatus(userId, "ONLINE");
    }

    public void setUserOffline(String userId) {
        userStatuses.put(userId, "OFFLINE");
        broadcastStatus(userId, "OFFLINE");
    }

    public String getUserStatus(String userId) {
        return userStatuses.getOrDefault(userId, "OFFLINE");
    }

    private void broadcastStatus(String userId, String status) {
        messagingTemplate.convertAndSend("/topic/users.status",
                Map.of("userId", userId, "status", status));
    }
}