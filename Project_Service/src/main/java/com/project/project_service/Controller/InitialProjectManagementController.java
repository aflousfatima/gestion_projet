package com.project.project_service.Controller;

import com.project.project_service.DTO.InitialProjectManagementDTO;
import com.project.project_service.DTO.ProjectResponseDTO;
import com.project.project_service.Service.InitialProjectManagementService;
import com.project.project_service.config.AuthClient;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
public class InitialProjectManagementController {

    @Autowired
    private InitialProjectManagementService initialProjectManagementService;

    @Autowired
    private AuthClient authClient;  // Injecter le client Auth pour extraire l'ID de l'utilisateur


    @Operation(summary = "Créer un projet initial",
            description = "Cette méthode permet de créer une entreprise, une équipe et un projet associés à un utilisateur authentifié.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entreprise, équipe et projet créés avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de la création des entités")
    })
    @PostMapping("/create-initial-project")
    public ResponseEntity<String> CreateInitialProjectManagement(
            @Parameter(description = "Token d'autorisation JWT", required = true)
            @RequestHeader("Authorization") String authorization,  // Récupérer l'access token depuis l'en-tête
            @RequestBody InitialProjectManagementDTO dto) {

        System.out.println("Début de la création du projet avec les données : " + dto);
        try {
            // Récupérer l'ID du créateur à partir du token d'accès
            System.out.println("Extraction du userId à partir du token d'accès...");
            String authId = authClient.extractUserIdFromToken(authorization);
            System.out.println("ID du créateur récupéré : " + authId);

            // Passer l'ID du créateur au service pour l'utiliser lors de la création des entités
            System.out.println("Appel au service pour créer l'entreprise, l'équipe et le projet...");
            initialProjectManagementService.createCompanyTeamProject(dto, authId);

            System.out.println("Entreprise, équipe et projet créés avec succès.");
            return ResponseEntity.ok("Entreprise, équipe et projet créés avec succès.");
        } catch (Exception e) {
            System.out.println("Erreur lors de la création des entités : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la création des entités.");
        }
    }
    @Operation(summary = "Obtenir les projets par manager",
            description = "Récupère tous les projets associés à un manager spécifique en fonction de son ID d'authentification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des projets retournée avec succès"),
            @ApiResponse(responseCode = "400", description = "Erreur lors de la récupération des projets")
    })
    @GetMapping("/projects/by-manager")
    public ResponseEntity<ProjectResponseDTO> getProjectsByManager(
            @Parameter(description = "ID de l'utilisateur authentifié", required = true)
            @RequestParam String authId) {
        try {
            ProjectResponseDTO response = initialProjectManagementService.getProjectsByManager(authId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);
        }
    }


}
