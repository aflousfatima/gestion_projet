package com.task.taskservice.Chatbot.Controller;

import com.task.taskservice.Chatbot.DTO.PredictionResponse;
import com.task.taskservice.Chatbot.DTO.TaskInput;
import com.task.taskservice.Chatbot.DTO.TaskResponse;
import com.task.taskservice.Chatbot.DTO.TaskStatusResponse;
import com.task.taskservice.Chatbot.Service.ChatbotService;
import com.task.taskservice.Configuration.ProjectClient;
import com.task.taskservice.Configuration.SecurityConfig;
import com.task.taskservice.DTO.ProjectDTO;
import com.task.taskservice.DTO.ProjectResponceChatbotDTO;
import com.task.taskservice.Enumeration.WorkItemPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot/tasks")
public class ChatbotController {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ProjectClient projectClient;
    @GetMapping("/late")
    public ResponseEntity<List<TaskResponse>> getLateTasks() {
        List<TaskResponse> lateTasks = chatbotService.getLateTasks();
        if (lateTasks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune tâche en retard trouvée");
        }
        return ResponseEntity.ok(lateTasks);
    }

    @GetMapping("/status")
    public ResponseEntity<List<TaskStatusResponse>> getTaskStatusByProject(@RequestParam("title") String title) {
        // Resolve project title to projectId using project-service
        ProjectResponceChatbotDTO project = projectClient.getProjectByName(title);
        if (project == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet non trouvé avec le titre : " + title);
        }
        Long projectId = project.getId();

        List<TaskStatusResponse> statusCounts = chatbotService.getTaskStatusByProject(projectId);
        if (statusCounts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune tâche trouvée pour ce projet");
        }
        return ResponseEntity.ok(statusCounts);
    }

    @GetMapping("/priority")
    public ResponseEntity<List<TaskResponse>> getTasksByPriority(@RequestParam("priority") WorkItemPriority priority) {
        List<TaskResponse> priorityTasks = chatbotService.getTasksByPriority(priority);
        if (priorityTasks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune tâche trouvée pour cette priorité");
        }
        return ResponseEntity.ok(priorityTasks);
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<TaskResponse>> getBlockedTasks() {
        List<TaskResponse> blockedTasks = chatbotService.getBlockedTasks();
        if (blockedTasks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune tâche bloquée trouvée");
        }
        return ResponseEntity.ok(blockedTasks);
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<TaskResponse>> getAssignedTasks(@RequestParam("userId") String userId) {
        logger.info("Received request for assigned tasks for userId: {}", userId);
        List<TaskResponse> assignedTasks = chatbotService.getAssignedTasks(userId);
        if (assignedTasks.isEmpty()) {
            logger.warn("No tasks found for userId: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune tâche assignée à cet utilisateur");
        }
        logger.info("Returning {} tasks for userId: {}", assignedTasks.size(), userId);
        return ResponseEntity.ok(assignedTasks);
    }

    // New endpoint for fetching tasks by name
    @GetMapping("/assigned/by-name")
    public ResponseEntity<List<TaskResponse>> getAssignedTasksByName(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestHeader("Authorization") String authorization) {
        logger.info("Received request for assigned tasks for firstName: {}, lastName: {}", firstName, lastName);
        List<TaskResponse> assignedTasks = chatbotService.getAssignedTasksByName(firstName, lastName, authorization);
        logger.info("Returning {} tasks for firstName: {}, lastName: {}", assignedTasks.size(), firstName, lastName);
        return ResponseEntity.ok(assignedTasks);
    }

    @GetMapping("/predict")
    public PredictionResponse predictTaskDuration(@RequestParam String title) {
        return chatbotService.predictTaskDuration(title);
    }


    @GetMapping("/details")
    public TaskInput getTaskDetails(@RequestParam String title) {
        logger.info("Fetching task details for title: {}", title);
        try {
            return chatbotService.getTaskDetails(title);
        } catch (Exception e) {
            logger.error("Error fetching task details for title {}: {}", title, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch task details", e);
        }
    }
}