package com.project.project_service.Controller;

import com.project.project_service.DTO.UserStoryDTO;
import com.project.project_service.DTO.UserStoryHistoryDto;
import com.project.project_service.DTO.UserStoryRequest;

import com.project.project_service.Service.HistoryService;
import com.project.project_service.Service.UserStoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class UserStoryController {

    @Autowired
    private UserStoryService userStoryService;
    @Autowired
    private HistoryService historyService;

    @PostMapping("/{projectId}/user-stories")
    public ResponseEntity<UserStoryDTO> createUserStory(
            @PathVariable Long projectId,
            @RequestBody UserStoryRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            UserStoryDTO createdUserStory = userStoryService.createUserStory(projectId, request, authorizationHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUserStory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/{projectId}/user-stories/{userStoryId}")
    public ResponseEntity<UserStoryDTO> updateUserStory(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestBody UserStoryRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            UserStoryDTO updatedUserStory = userStoryService.updateUserStory(projectId, userStoryId, request, authorizationHeader);
            return ResponseEntity.ok(updatedUserStory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{projectId}/user-stories")
    public ResponseEntity<List<UserStoryDTO>> getUserStories(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorizationHeader) {
        System.out.println("Appel de getUserStories pour projectId: " + projectId);
        try {
            List<UserStoryDTO> userStories = userStoryService.getUserStoriesByProjectId(projectId);
            return ResponseEntity.ok(userStories);
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    // New endpoint to delete a user story
    @DeleteMapping("/{projectId}/user-stories/{userStoryId}")
    public ResponseEntity<Void> deleteUserStory(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            userStoryService.deleteUserStory(projectId, userStoryId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Nouvel endpoint pour assigner une user story à un sprint
    @PutMapping("/{projectId}/user-stories/{userStoryId}/assign-sprint/{sprintId}")
    public ResponseEntity<UserStoryDTO> assignUserStoryToSprint(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            UserStoryDTO updatedUserStory = userStoryService.assignUserStoryToSprint(projectId, userStoryId, sprintId, authorizationHeader);
            return ResponseEntity.ok(updatedUserStory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/{projectId}/user-stories/{userStoryId}/remove-sprint")
    public ResponseEntity<UserStoryDTO> removeUserStoryFromSprint(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            UserStoryDTO updatedUserStory = userStoryService.removeUserStoryFromSprint(projectId, userStoryId, authorizationHeader);
            return ResponseEntity.ok(updatedUserStory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/{projectId}/user-stories/{userStoryId}/dependencies")
    public ResponseEntity<UserStoryDTO> updateDependencies(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestBody Map<String, List<Long>> requestBody,
            @RequestHeader("Authorization") String token) {
        List<Long> newDependsOn = requestBody.get("dependsOn");
        UserStoryDTO updatedStory = userStoryService.updateDependencies(projectId, userStoryId, newDependsOn, token);
        return ResponseEntity.ok(updatedStory);
    }

    @PutMapping("/{projectId}/user-stories/{userStoryId}/tags")
    public ResponseEntity<UserStoryDTO> updateTags(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestBody Map<String, List<String>> requestBody,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            List<String> tags = requestBody.get("tags");
            UserStoryDTO updatedStory = userStoryService.updateTags(projectId, userStoryId, tags, authorizationHeader);
            return ResponseEntity.ok(updatedStory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


    // 🔍 Récupérer l’historique d’une User Story
    @GetMapping("/user-story/{userStoryId}/history")
    public ResponseEntity<List<UserStoryHistoryDto>> getUserStoryHistory(
            @PathVariable Long userStoryId,
            @RequestHeader("Authorization") String authHeader) {

        String userToken = authHeader.replace("Bearer ", "");
        List<UserStoryHistoryDto> enrichedHistory = historyService.getUserStoryHistoryWithAuthorNames(userStoryId, userToken);

        if (enrichedHistory.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(enrichedHistory);
    }


    @GetMapping("/{projectId}/sprint/actif/user_stories")
    public List<Long> getUserStoryIdsOfActiveSprint(@PathVariable Long projectId) {
        return userStoryService.getUserStoryIdsOfActiveSprint(projectId);
    }
}
