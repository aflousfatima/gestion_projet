package com.project.project_service.Controller;

import com.project.project_service.DTO.SprintDTO;
import com.project.project_service.DTO.SprintHistoryDto;
import com.project.project_service.DTO.UserStoryHistoryDto;
import com.project.project_service.Entity.SprintHistory;
import com.project.project_service.Repository.SprintHistoryRepository;
import com.project.project_service.Service.HistoryService;
import com.project.project_service.Service.SprintService;
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

@RestController
@RequestMapping("/api/projects")
@OpenAPIDefinition(info = @Info(
        title = "API de Gestion des Sprints",
        version = "1.0",
        description = "Cette API permet de gérer les sprints associés aux projets."
),
        servers = @Server(
                url = "http://localhost:8085/"
        ))
public class SprintController {

    @Autowired
    private SprintService sprintService;
  @Autowired
  private HistoryService historyService;

    @Operation(summary = "Créer un sprint",
            description = "Cette méthode permet de créer un nouveau sprint pour un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sprint créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
    @PostMapping("/{projectId}/sprints")
    public ResponseEntity<SprintDTO> createSprint(
            @PathVariable Long projectId,
            @RequestBody SprintDTO request,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            SprintDTO createdSprint = sprintService.createSprint(projectId, request, authorizationHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSprint);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Récupérer les sprints d'un projet",
            description = "Cette méthode permet de récupérer la liste des sprints associés à un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des sprints récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Projet ou sprints non trouvés")
    })
    @GetMapping("/{projectId}/sprints")
    public ResponseEntity<List<SprintDTO>> getSprints(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            List<SprintDTO> sprints = sprintService.getSprintsByProjectId(projectId);
            return ResponseEntity.ok(sprints);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Mettre à jour un sprint",
            description = "Cette méthode permet de modifier un sprint existant pour un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sprint mis à jour avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Sprint ou projet non trouvé")
    })
    @PutMapping("/{projectId}/sprints/{sprintId}")
    public ResponseEntity<SprintDTO> updateSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestBody SprintDTO request,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            SprintDTO updatedSprint = sprintService.updateSprint(projectId, sprintId, request, authorizationHeader);
            return ResponseEntity.ok(updatedSprint);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Supprimer un sprint",
            description = "Cette méthode permet de supprimer un sprint spécifique d'un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Sprint supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Sprint ou projet non trouvé")
    })
    @DeleteMapping("/{projectId}/sprints/{sprintId}")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            sprintService.deleteSprint(projectId, sprintId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Annuler un sprint",
            description = "Cette méthode permet d'annuler un sprint spécifique pour un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sprint annulé avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Sprint ou projet non trouvé")
    })
    @PostMapping("/{projectId}/sprints/{sprintId}/cancel")
    public ResponseEntity<SprintDTO> cancelSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String token) {
        SprintDTO updatedSprint = sprintService.cancelSprint(projectId, sprintId, token);
        return ResponseEntity.ok(updatedSprint);
    }


    @Operation(summary = "Archiver un sprint",
            description = "Cette méthode permet d'archiver un sprint spécifique pour un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sprint archivé avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Sprint ou projet non trouvé")
    })
    @PostMapping("/{projectId}/sprints/{sprintId}/archive")
    public ResponseEntity<SprintDTO> archiveSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String token) {
        SprintDTO updatedSprint = sprintService.archiveSprint(projectId, sprintId, token);
        return ResponseEntity.ok(updatedSprint);
    }

    @Operation(summary = "Activer un sprint",
            description = "Cette méthode permet d'activer un sprint spécifique pour un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sprint activé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
    @PostMapping("/{projectId}/sprints/{sprintId}/activate")
    public ResponseEntity<SprintDTO> activateSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            SprintDTO activatedSprint = sprintService.activateSprint(projectId, sprintId, authorizationHeader);
            return ResponseEntity.status(HttpStatus.OK).body(activatedSprint);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


    @Operation(summary = "Mettre à jour le statut d'un sprint",
            description = "Cette méthode permet de mettre à jour le statut d'un sprint spécifique pour un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statut du sprint mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
    @PostMapping("/{projectId}/sprints/{sprintId}/update-status")
    public ResponseEntity<SprintDTO> updateSprintStatus(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            SprintDTO updatedSprint = sprintService.updateSprintStatus(projectId, sprintId, authorizationHeader);
            return ResponseEntity.ok(updatedSprint); // 200 OK avec SprintDTO
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // 400
        }
    }

    @Operation(summary = "Récupérer l'historique d'un sprint",
            description = "Cette méthode permet de récupérer l'historique des modifications d'un sprint spécifique, incluant les noms des auteurs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historique du sprint récupéré avec succès"),
            @ApiResponse(responseCode = "204", description = "Aucun historique trouvé pour le sprint"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
    @GetMapping("/sprint/{sprintId}/history")
    public ResponseEntity<List<SprintHistoryDto>> getSprintHistory(
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String authHeader) {

        String userToken = authHeader.replace("Bearer ", "");
        List<SprintHistoryDto> enrichedHistory = historyService.getSprintHistoryWithAuthorNames(sprintId, userToken);

        if (enrichedHistory.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(enrichedHistory);
    }

}