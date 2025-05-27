package com.task.taskservice.Configuration;

import com.task.taskservice.DTO.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "project-service", url = "http://localhost:8085",fallback = ProjectClientFallback.class)
public interface ProjectClient {


    @GetMapping("/api/projects/by-name")
    ProjectResponceChatbotDTO getProjectByName(@RequestParam("name") String name);

    @GetMapping("/api/projects/by-user")
    ProjectResponseWithRoleDTO getProjectsByUser(@RequestParam("authId") String authId);



    // Méthode pour récupérer les UserStoryIds du sprint actif pour un projet
    @GetMapping("/api/projects/{projectId}/sprint/actif/user_stories")
    List<Long> getUserStoriesOfActiveSprint(@PathVariable("projectId") Long projectId);

    @PostMapping("/api/projects/{projectId}/{userStoryId}/check-status")
    UserStoryDTO checkAndUpdateUserStoryStatus(
            @PathVariable("projectId") Long projectId,
            @PathVariable("userStoryId") Long userStoryId,
            @RequestHeader("Authorization") String token);

    @GetMapping("/api/projects/{projectId}/user-stories")
    ResponseEntity<List<UserStoryDTO>> getUserStoriesByProjectId(@PathVariable("projectId") Long projectId);

    @GetMapping("/api/{projectId}/github-user")
    ResponseEntity<Map<String, String>> getGitHubUserId(@PathVariable("projectId") Long projectId);

    @GetMapping("/api/{projectId}/github-link")
    ResponseEntity<Map<String, String>> getGitHubRepository(@PathVariable("projectId") Long projectId);

    @GetMapping("/api/active")
    ResponseEntity<List<Long>> getActiveProjectIds();
}


