package com.project.project_service.Controller;

import com.project.project_service.DTO.UserStoryDTO;
import com.project.project_service.DTO.UserStoryHistoryDto;
import com.project.project_service.DTO.UserStoryRequest;

import com.project.project_service.Service.HistoryService;
import com.project.project_service.Service.UserStoryService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@OpenAPIDefinition(info = @Info(
        title = "API de Gestion des User Stories",
        version = "1.0",
        description = "Cette API permet de gérer les user stories associées aux projets."
),
        servers = @Server(
                url = "http://localhost:8085/"
        ))
public class UserStoryController {

    @Autowired
    private UserStoryService userStoryService;
    @Autowired
    private HistoryService historyService;

    @Operation(summary = "Créer une user story",
            description = "Cette méthode permet de créer une nouvelle user story pour un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User story créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
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
    @Operation(summary = "Mettre à jour une user story",
            description = "Cette méthode permet de modifier une user story existante pour un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User story mise à jour avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
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
    @Operation(summary = "Récupérer les user stories d'un projet",
            description = "Cette méthode permet de récupérer la liste des user stories associées à un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des user stories récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Projet ou user stories non trouvés")
    })
    @GetMapping("/{projectId}/user-stories")
    public ResponseEntity<List<UserStoryDTO>> getUserStories(
            @PathVariable Long projectId ) {
        System.out.println("Appel de getUserStories pour projectId: " + projectId);
        try {
            List<UserStoryDTO> userStories = userStoryService.getUserStoriesByProjectId(projectId);
            return ResponseEntity.ok(userStories);
        } catch (RuntimeException e) {
            System.out.println("Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    @Operation(summary = "Supprimer une user story",
            description = "Cette méthode permet de supprimer une user story spécifique d'un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User story supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
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

    @Operation(summary = "Assigner une user story à un sprint",
            description = "Cette méthode permet d'assigner une user story à un sprint spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User story assignée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "User story, sprint ou projet non trouvé")
    })
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
    @Operation(summary = "Retirer une user story d'un sprint",
            description = "Cette méthode permet de retirer une user story d'un sprint dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User story retirée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
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
    @Operation(summary = "Mettre à jour les dépendances d'une user story",
            description = "Cette méthode permet de mettre à jour les dépendances d'une user story spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dépendances mises à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
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
    @Operation(summary = "Mettre à jour les tags d'une user story",
            description = "Cette méthode permet de mettre à jour les tags associés à une user story spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tags mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
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


    @Operation(summary = "Récupérer l'historique d'une user story",
            description = "Cette méthode permet de récupérer l'historique des modifications d'une user story spécifique, incluant les noms des auteurs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historique de la user story récupéré avec succès"),
            @ApiResponse(responseCode = "204", description = "Aucun historique trouvé pour la user story"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
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

    @Operation(summary = "Récupérer les IDs des user stories d'un sprint actif",
            description = "Cette méthode permet de récupérer les IDs des user stories associées au sprint actif d'un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "IDs des user stories récupérés avec succès"),
            @ApiResponse(responseCode = "404", description = "Projet ou sprint actif non trouvé")
    })
    @GetMapping("/{projectId}/sprint/actif/user_stories")
    public List<Long> getUserStoryIdsOfActiveSprint(@PathVariable Long projectId) {
        return userStoryService.getUserStoryIdsOfActiveSprint(projectId);
    }
    @Operation(summary = "Vérifier et mettre à jour le statut d'une user story",
            description = "Cette méthode permet de vérifier et de mettre à jour le statut d'une user story spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut de la user story mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
    @PostMapping("/{projectId}/{userStoryId}/check-status")
    public ResponseEntity<UserStoryDTO> checkAndUpdateUserStoryStatus(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestHeader("Authorization") String token) {
        UserStoryDTO updatedUserStory = userStoryService.checkAndUpdateUserStoryStatus(projectId, userStoryId, token);
        return ResponseEntity.ok(updatedUserStory);
    }
}
