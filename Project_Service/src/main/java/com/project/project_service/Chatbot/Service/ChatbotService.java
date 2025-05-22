package com.project.project_service.Chatbot.Service;


import com.project.project_service.Chatbot.DTO.ProjectRemainingTimeResponse;
import com.project.project_service.Chatbot.DTO.SprintSummaryResponse;
import com.project.project_service.Chatbot.DTO.UserStoryResponse;
import com.project.project_service.DTO.TaskStatusResponse;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.WorkItemStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.config.TaskClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TaskClient taskClient;

    public ProjectRemainingTimeResponse getProjectRemainingTime(Long projectId) {
        Optional<Projet> projectOptional = projetRepository.findById(projectId);
        if (projectOptional.isEmpty()) {
            throw new RuntimeException("Projet non trouvé avec l'ID : " + projectId);
        }
        Projet project = projectOptional.get();
        LocalDate today = LocalDate.now();
        LocalDate deadline = project.getDeadline();
        long remainingDays = ChronoUnit.DAYS.between(today, deadline);
        return new ProjectRemainingTimeResponse(project.getName(), remainingDays, deadline);
    }

    public List<UserStoryResponse> getUserStoriesInCurrentSprint(Long projectId) {
        Sprint sprint = sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
        if (sprint == null) {
            throw new RuntimeException("Aucun sprint actif trouvé pour le projet : " + projectId);
        }
        return sprint.getUserStories().stream()
                .map(story -> new UserStoryResponse(story.getId(), story.getTitle()))
                .collect(Collectors.toList());
    }

    public SprintSummaryResponse getCurrentSprintSummary(Long projectId) {
        Sprint sprint = sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
        if (sprint == null) {
            throw new RuntimeException("Aucun sprint actif trouvé pour le projet : " + projectId);
        }
        List<Long> userStoryIds = sprint.getUserStories().stream()
                .map(UserStory::getId)
                .collect(Collectors.toList());

        // Appeler le microservice TASK pour obtenir le statut des tâches
        List<TaskStatusResponse> taskStatus = taskClient.getTaskStatusByProject(projectId);
        long totalTasks = taskStatus.stream().mapToLong(TaskStatusResponse::getCount).sum();
        long completedTasks = taskStatus.stream()
                .filter(status -> status.getStatus() == WorkItemStatus.DONE)
                .mapToLong(TaskStatusResponse::getCount)
                .sum();
        double progress = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;

        return new SprintSummaryResponse(sprint.getName(), progress, totalTasks, completedTasks, sprint.getEndDate());
    }
}