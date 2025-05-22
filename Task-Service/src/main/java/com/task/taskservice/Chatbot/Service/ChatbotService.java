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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private IAClient aiServiceClient;

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

    public PredictionResponse predictTaskDuration(Long taskId) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isEmpty()) {
            throw new RuntimeException("Tâche non trouvée avec l'ID : " + taskId);
        }
        Task task = taskOptional.get();

        // Construire TaskInput pour AI-Service
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

        // Appeler AI-Service
        return aiServiceClient.predictTaskDuration(taskInput);
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