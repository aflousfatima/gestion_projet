package com.collaboration.collaborationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtDecoder jwtDecoder;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            String channelId = accessor.getFirstNativeHeader("channelId"); // Ensure channelId is sent
            if (token != null && token.startsWith("Bearer ") && channelId != null) {
                try {
                    String jwtToken = token.replace("Bearer ", "");
                    Jwt jwt = jwtDecoder.decode(jwtToken);
                    String userId = jwt.getSubject(); // Extract userId from JWT subject
                    logger.debug("WebSocket authenticated: userId={}, channelId={}", userId, channelId);
                    JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt, extractAuthorities(jwt), userId);
                    accessor.setUser(authToken);
                } catch (Exception e) {
                    logger.error("WebSocket authentication failed: Invalid JWT token", e);
                    throw new IllegalArgumentException("Invalid JWT token", e);
                }
            } else {
                logger.warn("WebSocket connection rejected: Missing Authorization or channelId header");
                throw new IllegalArgumentException("Missing or invalid Authorization/channelId header");
            }
        }
        return message;
    }

    private List<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = realmAccess != null && realmAccess.containsKey("roles")
                ? (List<String>) realmAccess.get("roles")
                : new ArrayList<>();
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}