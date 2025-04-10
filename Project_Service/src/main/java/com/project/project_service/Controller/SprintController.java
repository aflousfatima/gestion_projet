package com.project.project_service.Controller;

import com.project.project_service.DTO.SprintDTO;
import com.project.project_service.DTO.SprintHistoryDto;
import com.project.project_service.DTO.UserStoryHistoryDto;
import com.project.project_service.Entity.SprintHistory;
import com.project.project_service.Repository.SprintHistoryRepository;
import com.project.project_service.Service.HistoryService;
import com.project.project_service.Service.SprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class SprintController {

    @Autowired
    private SprintService sprintService;
  @Autowired
  private HistoryService historyService;
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

    // Endpoint pour annuler un sprint
    @PostMapping("/{projectId}/sprints/{sprintId}/cancel")
    public ResponseEntity<SprintDTO> cancelSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String token) {
        SprintDTO updatedSprint = sprintService.cancelSprint(projectId, sprintId, token);
        return ResponseEntity.ok(updatedSprint);
    }

    // Endpoint pour archiver un sprint
    @PostMapping("/{projectId}/sprints/{sprintId}/archive")
    public ResponseEntity<SprintDTO> archiveSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestHeader("Authorization") String token) {
        SprintDTO updatedSprint = sprintService.archiveSprint(projectId, sprintId, token);
        return ResponseEntity.ok(updatedSprint);
    }

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