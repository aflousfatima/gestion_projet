package com.collaboration.collaborationservice.channel.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "http://localhost:8083", fallback = AuthClientFallback.class)
public interface AuthClient {
    @GetMapping("/api/auth/decode-token")
    String decodeToken(@RequestHeader("Authorization") String authorization);
}
