package com.project.project_service.config;
import com.project.project_service.DTO.ProjectMemberDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(name = "authentification-service", url = "http://authentification-service:8083",fallback = AuthClientFallback.class)  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface AuthClient {
    @GetMapping("/api/project-members/by-user")
    List<ProjectMemberDTO> getProjectMembersByUserId(@RequestParam("userId") String userId);

    @GetMapping("/api/auth/users/{authId}")
    Map<String, Object> getUserDetailsByAuthId(
            @PathVariable("authId") String authId,
            @RequestHeader("Authorization") String authorization
    );

    @GetMapping("/api/assign-manager-role")
    String extractUserIdFromToken(@RequestHeader("Authorization") String authorization);



    // Nouvelle méthode pour décoder le token
    @GetMapping("/api/auth/decode-token")
    String decodeToken(@RequestHeader("Authorization") String authorization);


}

