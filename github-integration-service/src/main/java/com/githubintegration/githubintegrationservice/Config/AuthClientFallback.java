package com.githubintegration.githubintegrationservice.Config;


public class AuthClientFallback implements AuthClient {

    @Override
    public String decodeToken(String authorization) {
        // Return a fallback user ID
        return "fallback-token";
    }
}