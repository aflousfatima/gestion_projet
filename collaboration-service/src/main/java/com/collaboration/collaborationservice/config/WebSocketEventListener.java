package com.collaboration.collaborationservice.config;


import com.collaboration.collaborationservice.participant.Service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private PresenceService presenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            Long channelId = extractChannelId(headerAccessor);
            if (userId != null && channelId != null) {
                logger.debug("User connected: userId={}, channelId={}", userId, channelId);
                presenceService.userConnected(userId, channelId);
            } else {
                logger.warn("Skipping connect event: userId={} or channelId={} is null", userId, channelId);
            }
        } else {
            logger.warn("Skipping connect event: No user principal found");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            Long channelId = extractChannelId(headerAccessor);
            if (userId != null && channelId != null) {
                logger.debug("User disconnected: userId={}, channelId={}", userId, channelId);
                presenceService.userDisconnected(userId, channelId);
            } else {
                logger.warn("Skipping disconnect event: userId={} or channelId={} is null", userId, channelId);
            }
        } else {
            logger.warn("Skipping disconnect event: No user principal found");
        }
    }

    private Long extractChannelId(StompHeaderAccessor headerAccessor) {
        // First, try to get channelId from headers
        String channelIdStr = headerAccessor.getFirstNativeHeader("channelId");
        if (channelIdStr == null && headerAccessor.getSessionAttributes() != null) {
            // Fallback to session attributes
            channelIdStr = (String) headerAccessor.getSessionAttributes().get("channelId");
        }
        try {
            return channelIdStr != null ? Long.parseLong(channelIdStr) : null;
        } catch (NumberFormatException e) {
            logger.error("Invalid channelId format: {}", channelIdStr, e);
            return null;
        }
    }
}