package com.collaboration.collaborationservice.config;

import com.collaboration.collaborationservice.participant.Service.UserStatusService;
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

    @Autowired
    private UserStatusService userStatusService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String jwtToken = authHeader.replace("Bearer ", "");
                    Jwt jwt = jwtDecoder.decode(jwtToken);
                    String userId = jwt.getSubject(); // Extrait l'userId depuis le claim "sub"
                    JwtAuthenticationToken authToken = new JwtAuthenticationToken(jwt, extractAuthorities(jwt));
                    accessor.setUser(authToken);
                    userStatusService.setUserOnline(userId); // Marque l'utilisateur comme ONLINE
                    accessor.getSessionAttributes().put("userId", userId); // Stocke l'userId dans la session
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid JWT token", e);
                }
            } else {
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String userId = (String) accessor.getSessionAttributes().get("userId");
            if (userId != null) {
                userStatusService.setUserOffline(userId); // Marque l'utilisateur comme OFFLINE
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