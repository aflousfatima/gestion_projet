package com.project.project_service.Controller;

import com.project.project_service.DTO.UserStoryRequest;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Service.UserStoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class UserStoryController {

    @Autowired
    private UserStoryService userStoryService;

    @PostMapping("/{projectId}/user-stories")
    public ResponseEntity<UserStory> createUserStory(
            @PathVariable Long projectId,
            @RequestBody UserStoryRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        // Appeler le service pour créer la User Story
        try {
            UserStory createdUserStory = userStoryService.createUserStory(projectId, request, authorizationHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUserStory);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Token invalide
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Autre erreur (ex. projet non trouvé)
        }
    }
}
