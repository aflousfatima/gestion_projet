package com.githubintegration.githubintegrationservice.Config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "authentification-service", url = "http://localhost:8083",fallback = AuthClientFallback.class)  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface AuthClient {
    @GetMapping("/api/auth/decode-token")
    String decodeToken(@RequestHeader("Authorization") String authorization);
}