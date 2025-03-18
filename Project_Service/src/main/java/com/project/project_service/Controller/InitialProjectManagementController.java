package com.project.project_service.Controller;

import com.project.project_service.DTO.InitialProjectManagementDTO;
import com.project.project_service.Service.InitialProjectManagementService;
import com.project.project_service.config.AuthClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class InitialProjectManagementController {

    @Autowired
    private InitialProjectManagementService companyTeamProjectService;

    @Autowired
    private AuthClient authClient;  // Injecter le client Auth pour extraire l'ID de l'utilisateur

    @PostMapping("/create-initial-project")
    public ResponseEntity<String> CreateInitialProjectManagement(
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
            companyTeamProjectService.createCompanyTeamProject(dto, authId);

            System.out.println("Entreprise, équipe et projet créés avec succès.");
            return ResponseEntity.ok("Entreprise, équipe et projet créés avec succès.");
        } catch (Exception e) {
            System.out.println("Erreur lors de la création des entités : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la création des entités.");
        }
    }
}
