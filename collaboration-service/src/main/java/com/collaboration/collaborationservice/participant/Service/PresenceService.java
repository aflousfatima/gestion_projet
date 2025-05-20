package com.collaboration.collaborationservice.participant.Service;

import com.collaboration.collaborationservice.participant.dto.PresenceMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class PresenceService {

    private final Set<String> onlineUsers = new HashSet<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void userConnected(String userId, Long channelId) {
        onlineUsers.add(userId);
        sendPresenceUpdate(userId, channelId, true);
    }

    public void userDisconnected(String userId, Long channelId) {
        onlineUsers.remove(userId);
        sendPresenceUpdate(userId, channelId, false);
    }

    private void sendPresenceUpdate(String userId, Long channelId, boolean online) {
        PresenceMessage message = new PresenceMessage(userId, online);
        messagingTemplate.convertAndSend("/topic/presence." + channelId, message);
    }

    public Set<String> getOnlineUsers() {
        return new HashSet<>(onlineUsers);
    }
}