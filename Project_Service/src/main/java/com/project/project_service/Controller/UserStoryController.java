package com.project.project_service.Controller;

import com.project.project_service.DTO.UserStoryDTO;
import com.project.project_service.DTO.UserStoryRequest;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Service.UserStoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class UserStoryController {

    @Autowired
    private UserStoryService userStoryService;

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
}
