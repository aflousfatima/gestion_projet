package com.project.project_service.Controller;

import com.project.project_service.DTO.*;
import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Service.ProjectService;
import com.project.project_service.config.AuthClient;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api")
@OpenAPIDefinition(info = @Info(
        title = "API de Gestion des Projets",
        version = "1.0",
        description = "Cette API permet de gérer les projets "
),
        servers = @Server(
                url = "http://localhost:8085/"
        ))
public class ProjectController {
    @Autowired
    private AuthClient authClient;  // Injecter le client Auth pour extraire l'ID de l'utilisateur
    @Autowired
    private ProjectService projectService;

    @Operation(summary = "Créer un projet ",
            description = "Cette méthode permet de créer un projet associés à un utilisateur authentifié.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "projet  créé avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la création du projet")
    })
    @PostMapping("/create-project")
    public ResponseEntity<String> createProject(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, String> projectData) {
        try {
            // Extraire l'authId depuis le token
            String authId = authClient.extractUserIdFromToken(authorization);

            // Récupérer les paramètres du projet depuis le corps de la requête
            String name = projectData.get("name");
            String description = projectData.get("description");
            String startDateStr = projectData.get("startDate");
            String deadlineStr = projectData.get("deadline");
            String status = projectData.get("status");
            String phase = projectData.get("phase");
            String priority = projectData.get("priority");

            // Validation des champs obligatoires
            if (name == null || description == null || startDateStr == null || deadlineStr == null
                    || status == null || phase == null || priority == null) {
                return ResponseEntity.badRequest().body("Tous les champs du projet sont requis");
            }

            // Convertir les dates depuis String vers LocalDateTime
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate deadline = LocalDate.parse(deadlineStr);

            // Appeler le service pour créer le projet
            projectService.createProject(authId, name, description, startDate, deadline, status, phase, priority);

            return ResponseEntity.ok("Projet créé avec succès");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la création du projet : " + e.getMessage());
        }
    }

    @Operation(summary = "Modifier un projet ",
            description = "Cette méthode permet de modifier un projet associés à un utilisateur authentifié.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "projet  modifiée avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la modification du projet")
    })
    @PutMapping("/modify-project")
    public ResponseEntity<Map<String, String>> updateProject(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, String> requestBody) {
        try {
            // Extraire l'authId depuis le token
            String authId = authClient.extractUserIdFromToken(authorization);

            // Récupérer les paramètres du projet depuis le corps de la requête
            String oldName = requestBody.get("oldName");
            String newName = requestBody.get("newName");
            String description = requestBody.get("description");
            String startDateStr = requestBody.get("startDate");
            String deadlineStr = requestBody.get("deadline");
            String status = requestBody.get("status");
            String phase = requestBody.get("phase");
            String priority = requestBody.get("priority");

            // Validation des champs obligatoires
            if (oldName == null || newName == null || description == null || startDateStr == null || deadlineStr == null
                    || status == null || phase == null || priority == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Tous les champs du projet sont requis");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Convertir les dates depuis String vers LocalDateTime
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate deadline = LocalDate.parse(deadlineStr);

            // Appeler le service pour mettre à jour le projet
            projectService.updateProject(authId, oldName, newName, description, startDate, deadline, status, phase, priority);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Projet modifié avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @Operation(summary = "Supprimer un projet ",
            description = "Cette méthode permet de supprimer un projet associés à un utilisateur authentifié.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "projet  suprrimé avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la supression du projet")
    })
    @DeleteMapping("/delete-project")
    public ResponseEntity<Map<String, String>> deleteProject(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, String> requestBody) {
        try {
            String authId = authClient.extractUserIdFromToken(authorization);
            String name = requestBody.get("name");

            projectService.deleteProject(authId, name);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Projet supprimé avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long projectId) {
        try {
            // Récupérer le projet depuis le service
            Projet project = projectService.getProjectById(projectId);
            Client manager = project.getManager();

            // Créer le ManagerDTO à partir des informations du manager
            ManagerDTO managerDTO = null;
            if (manager != null) {
                managerDTO = new ManagerDTO(
                        manager.getId(),
                        manager.getAuthId(),
                        null, // firstName à récupérer via le service d'auth si nécessaire
                        null, // lastName à récupérer via le service d'auth si nécessaire
                        manager.getRole()
                );
            }

            // Créer le ProjectDTO en remplissant tous les champs
            ProjectDTO projetDTO = new ProjectDTO(
                    project.getId(),
                    project.getName(),
                    project.getDescription(),
                    managerDTO,
                    project.getCreationDate(),
                    project.getStartDate(),
                    project.getDeadline(),
                    project.getStatus().toString(), // Convertir l'énum en String
                    project.getPhase().toString(), // Convertir l'énum en String
                    project.getPriority().toString() // Convertir l'énum en String
            );

            // Retourner la réponse avec le DTO
            return ResponseEntity.ok(projetDTO);
        } catch (Exception e) {
            // En cas d'erreur, retourner une réponse 404 avec un corps vide
            return ResponseEntity.status(404).body(null);
        }
    }

    @GetMapping("/manager/{projectId}")
    public ResponseEntity<ProjectDTO> getProjectDetails(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorizationHeader) {
        // Extraire le token (enlever "Bearer " du header)
        String accessToken = authorizationHeader.replace("Bearer ", "");
        ProjectDTO projectDetails = projectService.getProjectDetails(projectId, accessToken);
        return ResponseEntity.ok(projectDetails);
    }

    @GetMapping("/projects/by-user")
    public ProjectResponseWithRoleDTO getProjectsByUser(@RequestParam String authId) {
        return projectService.getProjectsByUser(authId);
    }

}
