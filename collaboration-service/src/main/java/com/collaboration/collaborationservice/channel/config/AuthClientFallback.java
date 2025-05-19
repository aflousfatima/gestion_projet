package com.collaboration.collaborationservice.channel.config;

import java.util.Map;

public class AuthClientFallback implements AuthClient {
    @Override
    public String decodeToken(String authorization) {
        throw new RuntimeException("Impossible de décoder le token : service d'authentification indisponible");
    }

    @Override
    public Map<String, Object> getUserDetailsByAuthId(String authId, String authorization) {
        return Map.of("authId", authId, "fallback", true);
    }
}