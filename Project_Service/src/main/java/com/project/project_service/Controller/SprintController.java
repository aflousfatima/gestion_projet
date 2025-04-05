package com.project.project_service.Controller;

import com.project.project_service.DTO.SprintDTO;
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
}