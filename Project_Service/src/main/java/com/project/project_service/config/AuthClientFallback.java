package com.project.project_service.config;
import com.project.project_service.DTO.ProjectMemberDTO;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

public class AuthClientFallback implements AuthClient {
    @Override
    public List<ProjectMemberDTO> getProjectMembersByUserId(String userId) {
        return List.of(); // ou retourner une valeur par défaut
    }
    @Override
    public String extractUserIdFromToken(String authorization) {
        return "fallback-user";
    }
    @Override
    public Map<String, Object> getUserDetailsByAuthId(String authId, String authorization) {
        return Map.of("authId", authId, "fallback", true);
    }
    @Override
    public String decodeToken(String authorization) {
        return "fallback-token";
    }
}
