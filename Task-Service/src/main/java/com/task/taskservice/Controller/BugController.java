package com.task.taskservice.Controller;

import com.task.taskservice.DTO.BugDTO;
import com.task.taskservice.DTO.DashboardStatsDTO;
import com.task.taskservice.DTO.BugCalendarDTO;
import com.task.taskservice.DTO.TimeEntryDTO;
import com.task.taskservice.Entity.Bug;
import com.task.taskservice.Mapper.BugMapper;
import com.task.taskservice.Service.BugService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/project/bugs")
@OpenAPIDefinition(info = @Info(
        title = "API de Gestion des Bugs",
        version = "1.0",
        description = "Cette API permet de gérer les bugs associés aux projets et user stories."
),
        servers = @Server(
                url = "http://localhost:8086/"
        ))
public class BugController {

    private static final Logger logger = LoggerFactory.getLogger(BugService.class);

    @Autowired
    private BugService bugService;
    @Autowired
    private BugMapper bugMapper;
    @Operation(summary = "Créer un bug",
            description = "Cette méthode permet de créer un nouveau bug pour une user story spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bug créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
    @PostMapping("/{projectId}/{userStoryId}/createBug")
    public ResponseEntity<BugDTO> createBug(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestBody BugDTO bugDTO,
            @RequestHeader("Authorization") String token) {
        try {
            BugDTO createdBug = bugService.createBug(projectId, userStoryId, bugDTO, token);
            return new ResponseEntity<>(createdBug, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid token for createBug: {}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
    }
    @Operation(summary = "Mettre à jour un bug",
            description = "Cette méthode permet de modifier un bug existant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bug mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Bug non trouvé")
    })
    @PutMapping("/{bugId}/updateBug")
    public ResponseEntity<BugDTO> updateBug(
            @PathVariable Long bugId,
            @RequestBody BugDTO bugDTO,
            @RequestHeader("Authorization") String token) {
        try {
            BugDTO updatedBug = bugService.updateBug(bugId, bugDTO, token);
            return new ResponseEntity<>(updatedBug, HttpStatus.OK);
        } catch (NoSuchElementException e) {
            logger.warn("Bug not found for ID: {}", bugId);
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid token for updateBug: {}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
    }
    @Operation(summary = "Supprimer un bug",
            description = "Cette méthode permet de supprimer un bug spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bug supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Bug non trouvé")
    })
    @DeleteMapping("/{bugId}/deleteBug")
    public ResponseEntity<Void> deleteBug(@PathVariable Long bugId) {
        try {
            bugService.deleteBug(bugId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NoSuchElementException e) {
            logger.warn("Bug not found for ID: {}", bugId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @Operation(summary = "Récupérer un bug",
            description = "Cette méthode permet de récupérer les détails d'un bug spécifique dans une user story et un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bug récupéré avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Bug, user story ou projet non trouvé")
    })
    @GetMapping("/{projectId}/{userStoryId}/{bugId}")
    public ResponseEntity<BugDTO> getBug(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @PathVariable Long bugId,
            @RequestHeader("Authorization") String token) {
        BugDTO bugDTO = bugService.getBugById(projectId, userStoryId, bugId, token);
        return new ResponseEntity<>(bugDTO, HttpStatus.OK);
    }
    @Operation(summary = "Récupérer les bugs d'une user story",
            description = "Cette méthode permet de récupérer la liste des bugs associés à une user story spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des bugs récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
    @GetMapping("/{projectId}/{userStoryId}")
    public ResponseEntity<List<BugDTO>> getBugsByProjectAndUserStory(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestHeader("Authorization") String token) {
        List<BugDTO> bugs = bugService.getBugsByProjectAndUserStory(projectId, userStoryId, token);
        return new ResponseEntity<>(bugs, HttpStatus.OK);
    }
    @Operation(summary = "Récupérer les bugs d'un projet",
            description = "Cette méthode permet de récupérer la liste des bugs associés à un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des bugs récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<List<BugDTO>> getBugsByProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<BugDTO> bugs = bugService.getBugsByProjectId(projectId, token);
        return new ResponseEntity<>(bugs, HttpStatus.OK);
    }
    @Operation(summary = "Récupérer les bugs du sprint actif",
            description = "Cette méthode permet de récupérer la liste des bugs associés au sprint actif d'un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des bugs récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Projet ou sprint actif non trouvé")
    })
    @GetMapping("/active_sprint/{projectId}")
    public ResponseEntity<List<BugDTO>> getBugsOfActiveSprint(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<BugDTO> bugs = bugService.getBugsOfActiveSprint(projectId, token);
        return new ResponseEntity<>(bugs, HttpStatus.OK);
    }

    @Operation(summary = "Joindre un fichier à un bug",
            description = "Cette méthode permet de joindre un fichier à un bug spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fichier joint avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Bug non trouvé"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de l'upload du fichier")
    })
    @PostMapping("/{bugId}/attachments")
    public ResponseEntity<BugDTO> uploadFileToBug(
            @PathVariable Long bugId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            Bug updatedBug = bugService.attachFileToBug(bugId, file, token);
            BugDTO bugDTO = bugMapper.toDTO(updatedBug);
            return new ResponseEntity<>(bugDTO, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Operation(summary = "Supprimer un fichier d'un bug",
            description = "Cette méthode permet de supprimer un fichier joint à un bug en utilisant son publicId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fichier supprimé avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Fichier non trouvé"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la suppression du fichier")
    })
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(
            @RequestParam String publicId,
            @RequestHeader("Authorization") String token) {
        logger.info("Received DELETE request for publicId: {}", publicId);
        try {
            bugService.deleteFileFromBug(publicId, token);
            return new ResponseEntity<>("File deleted successfully.", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.warn("File not found for publicId: {}", publicId);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            logger.error("Failed to delete file from Cloudinary for publicId: {}", publicId, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("Failed to delete file for publicId: {}", publicId, e);
            return new ResponseEntity<>("Failed to delete file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @Operation(summary = "Récupérer les statistiques du tableau de bord",
            description = "Cette méthode permet de récupérer les statistiques du tableau de bord pour un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
    @GetMapping("/dashboard/{projectId}")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        DashboardStatsDTO stats = bugService.getDashboardStats(projectId, token);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }

    @Operation(summary = "Récupérer les bugs pour le calendrier",
            description = "Cette méthode permet de récupérer la liste des bugs d'un projet formatée pour un affichage dans un calendrier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bugs pour le calendrier récupérés avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
    @GetMapping("/calendar/{projectId}")
    public ResponseEntity<List<BugCalendarDTO>> getBugsForCalendar(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<BugCalendarDTO> bugs = bugService.getBugsForCalendar(projectId, token);
        return new ResponseEntity<>(bugs, HttpStatus.OK);
    }
    @Operation(summary = "Ajouter une entrée de temps manuelle à un bug",
            description = "Cette méthode permet d'ajouter une entrée de temps manuelle à un bug spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entrée de temps ajoutée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Bug non trouvé")
    })
    @PostMapping("/{bugId}/time-entry")
    public ResponseEntity<Void> addManualTimeEntry(
            @PathVariable Long bugId,
            @RequestParam Long duration,
            @RequestParam String type,
            @RequestHeader("Authorization") String token) {
        bugService.addManualTimeEntry(bugId, duration, type, token);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Récupérer les entrées de temps d'un bug",
            description = "Cette méthode permet de récupérer la liste des entrées de temps associées à un bug spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entrées de temps récupérées avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Bug non trouvé")
    })
    @GetMapping("/{bugId}/time-entries")
    public ResponseEntity<List<TimeEntryDTO>> getTimeEntries(
            @PathVariable Long bugId,
            @RequestHeader("Authorization") String token) {
        List<TimeEntryDTO> timeEntryDTOs = bugService.getTimeEntries(bugId, token);
        return new ResponseEntity<>(timeEntryDTOs, HttpStatus.OK);
    }
}