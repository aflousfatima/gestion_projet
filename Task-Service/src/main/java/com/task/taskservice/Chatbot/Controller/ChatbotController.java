package com.task.taskservice.Chatbot.Controller;

import com.task.taskservice.Chatbot.DTO.PredictionResponse;
import com.task.taskservice.Chatbot.DTO.TaskResponse;
import com.task.taskservice.Chatbot.DTO.TaskStatusResponse;
import com.task.taskservice.Chatbot.Service.ChatbotService;
import com.task.taskservice.Enumeration.WorkItemPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot/tasks")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping("/late")
    public ResponseEntity<List<TaskResponse>> getLateTasks() {
        List<TaskResponse> lateTasks = chatbotService.getLateTasks();
        if (lateTasks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune tâche en retard trouvée");
        }
        return ResponseEntity.ok(lateTasks);
    }

    @GetMapping("/status")
    public ResponseEntity<List<TaskStatusResponse>> getTaskStatusByProject(@RequestParam("projectId") Long projectId) {
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
        List<TaskResponse> assignedTasks = chatbotService.getAssignedTasks(userId);
        if (assignedTasks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune tâche assignée à cet utilisateur");
        }
        return ResponseEntity.ok(assignedTasks);
    }

    @GetMapping("/predict")
    public ResponseEntity<PredictionResponse> predictTaskDuration(@RequestParam("taskId") Long taskId) {
        try {
            PredictionResponse prediction = chatbotService.predictTaskDuration(taskId);
            return ResponseEntity.ok(prediction);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}