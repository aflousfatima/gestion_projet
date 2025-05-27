package com.task.taskservice.Chatbot.Service;


import com.task.taskservice.Chatbot.DTO.PredictionResponse;
import com.task.taskservice.Chatbot.DTO.TaskInput;
import com.task.taskservice.Chatbot.DTO.TaskResponse;
import com.task.taskservice.Chatbot.DTO.TaskStatusResponse;
import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.Configuration.IAClient;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Entity.Tag;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Repository.TaskRepository;
import com.task.taskservice.Service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatbotService {


    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;

    private final AuthClient authClient;

   private final IAClient iaClient;

    @Autowired
    public ChatbotService(TaskRepository taskRepository ,AuthClient authClient , IAClient iaClient) {
        this.taskRepository = taskRepository;
        this.authClient = authClient;
        this.iaClient = iaClient;
    }

    public List<TaskResponse> getLateTasks() {
        List<Task> lateTasks = taskRepository.findByDueDateBeforeAndStatusNot(LocalDate.now(), WorkItemStatus.DONE);
        return mapToTaskResponse(lateTasks);
    }

    public List<TaskStatusResponse> getTaskStatusByProject(Long projectId) {
        List<Object[]> statusCounts = taskRepository.countTasksByStatusForProject(projectId);
        List<TaskStatusResponse> response = new ArrayList<>();
        for (Object[] statusCount : statusCounts) {
            WorkItemStatus status = (WorkItemStatus) statusCount[0];
            Long count = (Long) statusCount[1];
            response.add(new TaskStatusResponse(status, count));
        }
        return response;
    }

    public List<TaskResponse> getTasksByPriority(WorkItemPriority priority) {
        List<Task> priorityTasks = taskRepository.findByPriority(priority);
        return mapToTaskResponse(priorityTasks);
    }

    public List<TaskResponse> getBlockedTasks() {
        List<Task> blockedTasks = taskRepository.findBlockedTasksByDependencies(WorkItemStatus.DONE);
        return mapToTaskResponse(blockedTasks);
    }

    public List<TaskResponse> getAssignedTasks(String userId) {
        List<Task> assignedTasks = taskRepository.findByAssignedUserId(userId);
        return mapToTaskResponse(assignedTasks);
    }

    // New method to fetch tasks by firstName and lastName
    public List<TaskResponse> getAssignedTasksByName(String firstName, String lastName, String token) {
        logger.info("Fetching tasks for user with firstName: {}, lastName: {}", firstName, lastName);

        // Call auth-service to get userId by name
        Map<String, Object> userDetails;
        try {
            userDetails = authClient.searchUserByName(firstName, lastName, token);
        } catch (Exception e) {
            logger.error("User not found for firstName: {}, lastName: {}", firstName, lastName, e);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé");
        }

        String userId = (String) userDetails.get("id");
        if (userId == null) {
            logger.warn("No userId found for firstName: {}, lastName: {}", firstName, lastName);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé");
        }

        // Fetch tasks for the userId
        List<Task> assignedTasks = taskRepository.findByAssignedUserId(userId);
        if (assignedTasks.isEmpty()) {
            logger.warn("No tasks found for userId: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune tâche assignée à cet utilisateur");
        }

        return mapToTaskResponse(assignedTasks);
    }

    public PredictionResponse predictTaskDuration(String title) {
        // Logique existante
        long startTime = System.currentTimeMillis();
        logger.info("Starting predictTaskDuration for title: {}", title);
        TaskInput taskInput = getTaskDetails(title);
        try {
            long iaStartTime = System.currentTimeMillis();
            PredictionResponse prediction = iaClient.predictTaskDuration(taskInput);
            logger.info("IA response received in {}ms: {}", System.currentTimeMillis() - iaStartTime, prediction);
            logger.info("Total predictTaskDuration took {}ms", System.currentTimeMillis() - startTime);
            return prediction;
        } catch (Exception e) {
            logger.error("Failed to get prediction for title {}: {}", title, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Prediction failed", e);
        }
    }

    public TaskInput getTaskDetails(String title) {
        long startTime = System.currentTimeMillis();
        logger.info("Fetching task with title: {}", title);
        Optional<Task> taskOptional = taskRepository.findByTitle(title);
        if (taskOptional.isEmpty()) {
            logger.error("Task not found with title: {}", title);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        Task task = taskOptional.get();

        TaskInput taskInput = new TaskInput(
                task.getTitle(),
                task.getDescription() != null ? task.getDescription() : "",
                task.getEstimationTime() != null ? task.getEstimationTime() : 0.0f,
                task.getProgress() != null ? task.getProgress().floatValue() : 0.0f,
                task.getProjectId() != null ? task.getProjectId().intValue() : 0,
                task.getCreationDate() != null ? task.getCreationDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "",
                task.getStartDate() != null ? task.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null,
                task.getDueDate() != null ? task.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null,
                task.getStatus() != null ? task.getStatus().name() : "",
                task.getPriority() != null ? task.getPriority().name() : "",
                task.getTags() != null ? task.getTags().stream().map(Tag::getName).collect(Collectors.joining(",")) : "",
                task.getAssignedUserIds() != null ? String.join(",", task.getAssignedUserIds()) : ""
        );
        logger.info("TaskInput created in {}ms: {}", System.currentTimeMillis() - startTime, taskInput);
        return taskInput;
    }
    private List<TaskResponse> mapToTaskResponse(List<Task> tasks) {
        List<TaskResponse> response = new ArrayList<>();
        for (Task task : tasks) {
            String userIds = String.join(",", task.getAssignedUserIds());
            Map<String, Map<String, Object>> userDetails = authClient.getUserDetailsByIds(userIds);
            List<String> names = task.getAssignedUserIds().stream()
                    .map(id -> {
                        Map<String, Object> details = userDetails.getOrDefault(id, Map.of());
                        String firstName = (String) details.getOrDefault("firstName", "Inconnu");
                        String lastName = (String) details.getOrDefault("lastName", "Inconnu");
                        return firstName + " " + lastName;
                    })
                    .collect(Collectors.toList());
            List<Long> dependencyIds = task.getDependencies().stream()
                    .map(Task::getId)
                    .collect(Collectors.toList());
            response.add(new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDueDate(),
                    task.getPriority(),
                    names,
                    dependencyIds
            ));
        }
        return response;
    }
}