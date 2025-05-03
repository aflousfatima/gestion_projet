package com.project.project_service.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "auth-service", url = "http://localhost:8083")  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface AuthClient {

    @GetMapping("/api/assign-manager-role")
    String extractUserIdFromToken(@RequestHeader("Authorization") String authorization);

    // New method to fetch user details by authId
    @GetMapping("/api/auth/users/{authId}")
    Map<String, Object> getUserDetailsByAuthId(
            @PathVariable("authId") String authId,
            @RequestHeader("Authorization") String authorization
    );

    // Nouvelle méthode pour décoder le token
    @GetMapping("/api/auth/decode-token")
    String decodeToken(@RequestHeader("Authorization") String authorization);

    @GetMapping("/api/project-members/by-user")
    List<Long> getProjectIdsByUserId(@RequestParam("userId") String userId);

}

