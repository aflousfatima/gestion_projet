package com.project.project_service.Controller;

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

import java.util.HashMap;
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
            String authId = authClient.extractUserIdFromToken(authorization);
            String name = projectData.get("name");
            String description = projectData.get("description");

            if (name == null || description == null) {
                return ResponseEntity.badRequest().body("Nom et description du projet requis");
            }
            projectService.createProject(authId, name, description);
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
            String authId = authClient.extractUserIdFromToken(authorization);
            String oldName = requestBody.get("oldName");
            String newName = requestBody.get("newName");
            String description = requestBody.get("description");

            projectService.updateProject(authId, oldName, newName, description);

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
}
