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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
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
    private static final Logger LOGGER = Logger.getLogger(ProjectController.class.getName());
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

    @Operation(summary = "Récupérer un projet par ID",
            description = "Cette méthode permet de récupérer les détails d'un projet spécifique en utilisant son ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projet récupéré avec succès"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
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

    @Operation(summary = "Récupérer les détails d'un projet",
            description = "Cette méthode permet de récupérer les détails d'un projet spécifique pour un utilisateur authentifié.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Détails du projet récupérés avec succès"),
            @ApiResponse(responseCode = "401", description = "Token invalide"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
    @GetMapping("/manager/{projectId}")
    public ResponseEntity<ProjectDTO> getProjectDetails(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorizationHeader) {
        // Extraire le token (enlever "Bearer " du header)
        String accessToken = authorizationHeader.replace("Bearer ", "");
        ProjectDTO projectDetails = projectService.getProjectDetails(projectId, accessToken);
        return ResponseEntity.ok(projectDetails);
    }

    @Operation(summary = "Récupérer les projets d'un utilisateur",
            description = "Cette méthode permet de récupérer la liste des projets associés à un utilisateur spécifique en fonction de son ID d'authentification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projets récupérés avec succès"),
            @ApiResponse(responseCode = "400", description = "ID d'authentification invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des projets")
    })
    @GetMapping("/projects/by-user")
    public ProjectResponseWithRoleDTO getProjectsByUser(@RequestParam String authId) {
        return projectService.getProjectsByUser(authId);
    }

    @Operation(summary = "Lier un dépôt GitHub à un projet",
            description = "Cette méthode permet de lier un dépôt GitHub à un projet spécifique en utilisant l'URL du dépôt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dépôt GitHub lié avec succès"),
            @ApiResponse(responseCode = "400", description = "URL du dépôt manquante ou invalide"),
            @ApiResponse(responseCode = "401", description = "Token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la liaison du dépôt")
    })
    @PostMapping("/{projectId}/github-link")
    public ResponseEntity<Map<String, String>> linkGitHubRepository(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> requestBody,
            @RequestHeader("Authorization") String authorization) {
        try {
            String repositoryUrl = requestBody.get("repositoryUrl");
            if (repositoryUrl == null || repositoryUrl.trim().isEmpty()) {
                LOGGER.warning("URL du dépôt manquante ou vide");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "L'URL du dépôt est requise"));
            }
            LOGGER.info("Requête pour lier le dépôt GitHub au projet ID: " + projectId + ", URL: " + repositoryUrl);
            projectService.linkGitHubRepositoryToProject(projectId, repositoryUrl, authorization);
            return ResponseEntity.ok(Map.of("message", "Dépôt GitHub lié avec succès"));
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Erreur de validation: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOGGER.severe("Erreur serveur lors de la liaison du dépôt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur serveur: impossible de lier le dépôt."));
        }
    }

    @Operation(summary = "Récupérer l'URL du dépôt GitHub lié à un projet",
            description = "Cette méthode permet de récupérer l'URL du dépôt GitHub associé à un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL du dépôt récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Aucun dépôt GitHub lié au projet"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération de l'URL")
    })
    @GetMapping("/{projectId}/github-link")
    public ResponseEntity<Map<String, String>> getGitHubRepository(
            @PathVariable Long projectId) {
        try {
            String repositoryUrl = projectService.getGitHubRepositoryUrl(projectId);
            if (repositoryUrl == null) {
                LOGGER.info("Aucun dépôt GitHub lié pour le projet ID: " + projectId);
                return ResponseEntity.ok(Map.of("repositoryUrl", ""));
            }
            LOGGER.info("Récupération du dépôt GitHub pour le projet ID: " + projectId + ", URL: " + repositoryUrl);
            return ResponseEntity.ok(Map.of("repositoryUrl", repositoryUrl));
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de la récupération du dépôt GitHub: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération du dépôt."));
        }
    }

    @Operation(summary = "Récupérer l'ID de l'utilisateur GitHub lié à un projet",
            description = "Cette méthode permet de récupérer l'ID de l'utilisateur GitHub associé à un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ID de l'utilisateur GitHub récupéré avec succès"),
            @ApiResponse(responseCode = "404", description = "Aucun utilisateur GitHub lié au projet"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération de l'ID")
    })
    @GetMapping("/{projectId}/github-user")
    public ResponseEntity<Map<String, String>> getGitHubUserId(@PathVariable Long projectId) {
        try {
            Map<String, String> userIdMap = projectService.getGitHubUserId(projectId);
            if (!userIdMap.isEmpty()) {
                LOGGER.info("Retrieved GitHub userId for project ID: " + projectId);
            } else {
                LOGGER.info("No GitHub user linked for project ID: " + projectId);
            }
            return ResponseEntity.ok(userIdMap);
        } catch (Exception e) {
            LOGGER.severe("Error retrieving GitHub userId: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve GitHub userId"));
        }
    }

    @Operation(summary = "Récupérer les IDs des projets actifs",
            description = "Cette méthode permet de récupérer la liste des IDs des projets actuellement actifs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "IDs des projets actifs récupérés avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des IDs")
    })
    @GetMapping("/active")
    public ResponseEntity<List<Long>> getActiveProjectIds() {
        try {
            List<Long> projectIds = projectService.getActiveProjectIds();
            LOGGER.info("Retrieved project IDs: " + projectIds.size() + " projects");
            return ResponseEntity.ok(projectIds);
        } catch (Exception e) {
            LOGGER.severe("Error retrieving project IDs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @GetMapping("/projects/AllProjects")
    public List<ProjectDTO> getAllProjects() {
        return projectService.getAllProjects();
    }
}

