package com.collaboration.collaborationservice.channel.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:8083", fallback = AuthClientFallback.class)
public interface AuthClient {
    @GetMapping("/api/auth/decode-token")
    String decodeToken(@RequestHeader("Authorization") String authorization);

    @GetMapping("/api/auth/users/{authId}")
    Map<String, Object> getUserDetailsByAuthId(
            @PathVariable("authId") String authId,
            @RequestHeader("Authorization") String authorization
    );
}
