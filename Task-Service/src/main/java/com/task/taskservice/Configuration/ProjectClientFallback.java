package com.task.taskservice.Configuration;


import com.task.taskservice.DTO.ProjectDTO;
import com.task.taskservice.DTO.ProjectResponceChatbotDTO;
import com.task.taskservice.DTO.ProjectResponseWithRoleDTO;
import com.task.taskservice.DTO.UserStoryDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class ProjectClientFallback implements ProjectClient {

    @Override
    public ProjectResponceChatbotDTO getProjectByName(String name) {
        return null; // Retourne null pour déclencher 404 dans le contrôleur
    }

    @Override
    public ProjectResponseWithRoleDTO getProjectsByUser(String authId) {
        // Return an empty project response
        return new ProjectResponseWithRoleDTO(Collections.emptyList().toString(),null);
    }

    @Override
    public List<Long> getUserStoriesOfActiveSprint(Long projectId) {
        // Return an empty list of user story IDs
        return Collections.emptyList();
    }

    @Override
    public UserStoryDTO checkAndUpdateUserStoryStatus(Long projectId, Long userStoryId, String token) {
        // Return a default UserStoryDTO with minimal data
        return new UserStoryDTO();
    }

    @Override
    public ResponseEntity<List<UserStoryDTO>> getUserStoriesByProjectId(Long projectId) {
        // Return an empty list of user stories
        return ResponseEntity.ok(Collections.emptyList());
    }

    @Override
    public ResponseEntity<Map<String, String>> getGitHubUserId(Long projectId) {
        // Return an empty map
        return ResponseEntity.ok(Collections.emptyMap());
    }

    @Override
    public ResponseEntity<Map<String, String>> getGitHubRepository(Long projectId) {
        // Return an empty map
        return ResponseEntity.ok(Collections.emptyMap());
    }

    @Override
    public ResponseEntity<List<Long>> getActiveProjectIds() {
        // Return an empty list of project IDs
        return ResponseEntity.ok(Collections.emptyList());
    }
}