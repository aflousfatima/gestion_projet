package com.collaboration.collaborationservice.channel.config;
public class AuthClientFallback implements AuthClient {
    @Override
    public String decodeToken(String authorization) {
        throw new RuntimeException("Impossible de décoder le token : service d'authentification indisponible");
    }
}