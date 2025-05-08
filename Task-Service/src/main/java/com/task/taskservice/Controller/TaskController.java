package com.task.taskservice.Controller;

import com.task.taskservice.DTO.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.task.taskservice.Entity.Task;
import com.task.taskservice.Mapper.TaskMapper;
import com.task.taskservice.Service.TaskService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/project/tasks")
@OpenAPIDefinition(info = @Info(
        title = "API de Gestion des Tâches",
        version = "1.0",
        description = "Cette API permet de gérer les tâches associées aux projets et user stories."
),
        servers = @Server(
                url = "http://localhost:8085/"
        ))
public class TaskController {


    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskMapper taskMapper;

    @Operation(summary = "Créer une tâche",
            description = "Cette méthode permet de créer une nouvelle tâche pour une user story spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tâche créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide")
    })
    @PostMapping("/{projectId}/{userStoryId}/createTask")
    public ResponseEntity<TaskDTO> createTask(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestBody TaskDTO taskDTO,
            @RequestHeader("Authorization") String token) {
        TaskDTO createdTask = taskService.createTask(projectId ,userStoryId ,taskDTO, token);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    @Operation(summary = "Mettre à jour une tâche",
            description = "Cette méthode permet de modifier une tâche existante.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tâche mise à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée")
    })
    @PutMapping("/{taskId}/updateTask")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskDTO taskDTO,
            @RequestHeader("Authorization") String token) {
        TaskDTO updatedTask = taskService.updateTask(taskId, taskDTO, token);
        return new ResponseEntity<>(updatedTask, HttpStatus.OK);
    }
    @Operation(summary = "Supprimer une tâche",
            description = "Cette méthode permet de supprimer une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tâche supprimée avec succès"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée")
    })
    @DeleteMapping("/{taskId}/deleteTask")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Récupérer une tâche",
            description = "Cette méthode permet de récupérer les détails d'une tâche spécifique dans une user story et un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tâche récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche, user story ou projet non trouvé")
    })
    @GetMapping("/{projectId}/{userStoryId}/{taskId}")
    public ResponseEntity<TaskDTO> getTask(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        TaskDTO taskDTO = taskService.getTaskById(projectId, userStoryId, taskId, token);
        return new ResponseEntity<>(taskDTO, HttpStatus.OK);
    }

    @Operation(summary = "Récupérer les tâches d'une user story",
            description = "Cette méthode permet de récupérer la liste des tâches associées à une user story spécifique dans un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des tâches récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
    @GetMapping("/{projectId}/{userStoryId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProjectAndUserStory(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestHeader("Authorization") String token) {
        List<TaskDTO> tasks = taskService.getTasksByProjectAndUserStory(projectId, userStoryId, token);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @Operation(summary = "Récupérer les tâches d'un projet",
            description = "Cette méthode permet de récupérer la liste des tâches associées à un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des tâches récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<TaskDTO> tasks = taskService.getTasksByProjectId(projectId, token);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @Operation(summary = "Récupérer les tâches du sprint actif",
            description = "Cette méthode permet de récupérer la liste des tâches associées au sprint actif d'un projet.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des tâches récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Projet ou sprint actif non trouvé")
    })
    @GetMapping("/active_sprint/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksOfActiveSprint(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {

        List<TaskDTO> tasks = taskService.getTasksOfActiveSprint(projectId, token);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @Operation(summary = "Récupérer les tâches d'un utilisateur dans les sprints actifs",
            description = "Cette méthode permet de récupérer la liste des tâches assignées à un utilisateur dans les sprints actifs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des tâches récupérée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de la récupération des tâches")
    })
    @GetMapping("/user/active-sprints")
    public ResponseEntity<List<TaskDTO>> getTasksByUserAndActiveSprints(
            @RequestHeader("Authorization") String token) {
        logger.info("Received request to get tasks for user in active sprints");
        try {
            List<TaskDTO> tasks = taskService.getTasksByUserAndActiveSprints(token);
            logger.info("Returning {} tasks for user", tasks.size());
            return ResponseEntity.ok(tasks);
        } catch (IllegalArgumentException e) {
            logger.error("Error fetching tasks: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            logger.error("Unexpected error fetching tasks: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Joindre un fichier à une tâche",
            description = "Cette méthode permet de joindre un fichier à une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fichier joint avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée"),
            @ApiResponse(responseCode = "500", description = "Erreur serveur lors de l'upload du fichier")
    })
    @PostMapping("/{taskId}/attachments")
    public ResponseEntity<TaskDTO> uploadFileToTask(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            Task updatedTask = taskService.attachFileToTask(taskId, file, token);
            TaskDTO taskDTO = taskMapper.toDTO(updatedTask); // Convert to DTO with signed URLs
            return new ResponseEntity<>(taskDTO, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Operation(summary = "Supprimer un fichier d'une tâche",
            description = "Cette méthode permet de supprimer un fichier joint à une tâche en utilisant son publicId.")
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
            taskService.deleteFileFromTask(publicId, token);
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
        DashboardStatsDTO stats = taskService.getDashboardStats(projectId, token);
        return new ResponseEntity<>(stats, HttpStatus.OK);
    }
    @Operation(summary = "Récupérer les tâches pour le calendrier",
            description = "Cette méthode permet de récupérer la liste des tâches d'un projet formatée pour un affichage dans un calendrier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tâches pour le calendrier récupérées avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
    @GetMapping("/calendar/{projectId}")
    public ResponseEntity<List<TaskCalendarDTO>> getTasksForCalendar(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<TaskCalendarDTO> tasks = taskService.getTasksForCalendar(projectId, token);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }
    @Operation(summary = "Ajouter une entrée de temps manuelle à une tâche",
            description = "Cette méthode permet d'ajouter une entrée de temps manuelle à une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entrée de temps ajoutée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée")
    })
    @PostMapping("/{taskId}/time-entry")
    public ResponseEntity<Void> addManualTimeEntry(
            @PathVariable Long taskId,
            @RequestParam Long duration,
            @RequestParam String type,
            @RequestHeader("Authorization") String token) {
        taskService.addManualTimeEntry(taskId, duration, type, token);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Récupérer les entrées de temps d'une tâche",
            description = "Cette méthode permet de récupérer la liste des entrées de temps associées à une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Entrées de temps récupérées avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée")
    })
    @GetMapping("/{taskId}/time-entries")
    public ResponseEntity<List<TimeEntryDTO>> getTimeEntries(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        List<TimeEntryDTO> timeEntryDTOs = taskService.getTimeEntries(taskId, token);
        return new ResponseEntity<>(timeEntryDTOs, HttpStatus.OK);
    }
    @Operation(summary = "Ajouter une dépendance à une tâche",
            description = "Cette méthode permet d'ajouter une dépendance à une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dépendance ajoutée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche ou dépendance non trouvée")
    })
    @PostMapping("/{taskId}/dependencies/{dependencyId}/add-dependancy")
    public ResponseEntity<TaskDTO> addDependency(
            @PathVariable Long taskId,
            @PathVariable Long dependencyId,
            @RequestHeader("Authorization") String token) {
        TaskDTO updatedTask = taskService.addDependency(taskId, dependencyId, token);
        return new ResponseEntity<>(updatedTask, HttpStatus.OK);
    }
    @Operation(summary = "Supprimer une dépendance d'une tâche",
            description = "Cette méthode permet de supprimer une dépendance d'une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dépendance supprimée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche ou dépendance non trouvée")
    })
    @DeleteMapping("/{taskId}/dependencies/{dependencyId}")
    public ResponseEntity<TaskDTO> removeDependency(
            @PathVariable Long taskId,
            @PathVariable Long dependencyId,
            @RequestHeader("Authorization") String token) {
        TaskDTO updatedTask = taskService.removeDependency(taskId, dependencyId, token);
        return new ResponseEntity<>(updatedTask, HttpStatus.OK);
    }
    @Operation(summary = "Récupérer les dépendances potentielles d'une tâche",
            description = "Cette méthode permet de récupérer la liste des tâches pouvant être ajoutées comme dépendances pour une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dépendances potentielles récupérées avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée")
    })
    @GetMapping("/{taskId}/potential-dependencies")
    public ResponseEntity<List<TaskDTO>> getPotentialDependencies(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        List<TaskDTO> potentialDependencies = taskService.getPotentialDependencies(taskId, token);
        return new ResponseEntity<>(potentialDependencies, HttpStatus.OK);
    }
    @Operation(summary = "Récupérer l'historique d'une tâche",
            description = "Cette méthode permet de récupérer l'historique des modifications d'une tâche spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historique de la tâche récupéré avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée")
    })
    @GetMapping("/{taskId}/history")
    public ResponseEntity<List<WorkItemHistoryDTO>> getTaskHistory(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        List<WorkItemHistoryDTO> history = taskService.getTaskHistory(taskId, token);
        return ResponseEntity.ok(history);
    }
    @Operation(summary = "Récupérer l'historique d'une tâche avec les noms des auteurs",
            description = "Cette méthode permet de récupérer l'historique des modifications d'une tâche spécifique, incluant les noms des auteurs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historique de la tâche récupéré avec succès"),
            @ApiResponse(responseCode = "401", description = "Non autorisé, token invalide"),
            @ApiResponse(responseCode = "404", description = "Tâche non trouvée")
    })
    @GetMapping("/{taskId}/history-with-author-names")
    public ResponseEntity<List<WorkItemHistoryDTO>> getTaskHistoryWithAuthorNames(
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        List<WorkItemHistoryDTO> history = taskService.getTaskHistoryWithAuthorNames(taskId, token);
        return ResponseEntity.ok(history);
    }


    @Operation(summary = "Récupérer toutes les tâches d'un projet",
            description = "Cette méthode permet de récupérer toutes les tâches associées à un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des tâches récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
    @GetMapping("/{projectId}/fetch_tasks")
    public ResponseEntity<List<Task>> getTasksByProjectId(@PathVariable Long projectId) {
        List<Task> tasks = taskService.getTasksByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }
    @Operation(summary = "Compter les tâches d'un projet",
            description = "Cette méthode permet de compter le nombre total de tâches associées à un projet spécifique.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nombre de tâches récupéré avec succès"),
            @ApiResponse(responseCode = "404", description = "Projet non trouvé")
    })
    @GetMapping("/{projectId}/count")
    public ResponseEntity<Long> countTasksByProjectId(@PathVariable Long projectId) {
        long count = taskService.countTasksByProjectId(projectId);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Récupérer les tâches d'une user story (interne)",
            description = "Cette méthode permet de récupérer la liste des tâches associées à une user story spécifique dans un projet, pour un usage interne.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des tâches récupérée avec succès"),
            @ApiResponse(responseCode = "404", description = "User story ou projet non trouvé")
    })
    @GetMapping("/internal/{projectId}/{userStoryId}")
    public List<TaskDTO> getTasksByProjectAndUserStoryInternal(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId) {
        return taskService.getTasksByProjectAndUserStoryInternal(projectId, userStoryId);
    }
}
