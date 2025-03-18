package com.project.project_service.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service", url = "http://localhost:8083")  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface AuthClient {

    @GetMapping("/api/user-id")
    String extractUserIdFromToken(@RequestHeader("Authorization") String authorization);
}

