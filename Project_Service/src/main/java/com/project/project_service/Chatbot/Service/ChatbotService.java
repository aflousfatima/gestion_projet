package com.project.project_service.Chatbot.Service;

import com.project.project_service.Chatbot.DTO.ProjectRemainingTimeResponse;
import com.project.project_service.Chatbot.DTO.SprintSummaryResponse;
import com.project.project_service.Chatbot.DTO.UserStoryResponse;
import com.project.project_service.DTO.TaskStatusResponse;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.WorkItemStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.config.TaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TaskClient taskClient;

    public ProjectRemainingTimeResponse getProjectRemainingTime(String projectName) {
        logger.info("Fetching project by name: {}", projectName);
        Projet project = projetRepository.findByName(projectName)
                .orElseThrow(() -> {
                    logger.error("Project not found with name: {}", projectName);
                    return new RuntimeException("Projet non trouvé avec le nom : " + projectName);
                });
        logger.info("Found project: {} with ID: {}", projectName, project.getId());

        LocalDate today = LocalDate.now();
        LocalDate deadline = project.getDeadline();
        logger.debug("Calculating remaining days for project: {}. Today: {}, Deadline: {}", projectName, today, deadline);
        long remainingDays = ChronoUnit.DAYS.between(today, deadline);
        logger.info("Remaining days for project: {} is {}", projectName, remainingDays);

        return new ProjectRemainingTimeResponse(project.getName(), remainingDays, deadline);
    }

    public List<UserStoryResponse> getUserStoriesInCurrentSprint(String projectName) {
        logger.info("Fetching project by name for user stories: {}", projectName);
        Projet project = projetRepository.findByName(projectName)
                .orElseThrow(() -> {
                    logger.error("Project not found with name: {}", projectName);
                    return new RuntimeException("Projet non trouvé avec le nom : " + projectName);
                });
        logger.info("Found project: {} with ID: {}", projectName, project.getId());

        logger.info("Fetching active sprint for project: {}", projectName);
        Sprint sprint = sprintRepository.findByProjectIdAndStatus(project.getId(), SprintStatus.ACTIVE);
        if (sprint == null) {
            logger.error("No active sprint found for project: {}", projectName);
            throw new RuntimeException("Aucun sprint actif trouvé pour le projet : " + projectName);
        }
        logger.info("Found active sprint: {} for project: {}", sprint.getName(), projectName);

        List<UserStoryResponse> userStories = sprint.getUserStories().stream()
                .map(story -> {
                    logger.debug("Processing user story: {} for sprint: {}", story.getTitle(), sprint.getName());
                    return new UserStoryResponse(story.getId(), story.getTitle());
                })
                .collect(Collectors.toList());
        logger.info("Retrieved {} user stories for project: {}", userStories.size(), projectName);

        return userStories;
    }

    public SprintSummaryResponse getCurrentSprintSummary(String projectName) {
        logger.info("Fetching project by name for sprint summary: {}", projectName);
        Projet project = projetRepository.findByName(projectName)
                .orElseThrow(() -> {
                    logger.error("Project not found with name: {}", projectName);
                    return new RuntimeException("Projet non trouvé avec le nom : " + projectName);
                });
        logger.info("Found project: {} with ID: {}", projectName, project.getId());

        logger.info("Fetching active sprint for project: {}", projectName);
        Sprint sprint = sprintRepository.findByProjectIdAndStatus(project.getId(), SprintStatus.ACTIVE);
        if (sprint == null) {
            logger.error("No active sprint found for project: {}", projectName);
            throw new RuntimeException("Aucun sprint actif trouvé pour le projet : " + projectName);
        }
        logger.info("Found active sprint: {} for project: {}", sprint.getName(), projectName);

        List<Long> userStoryIds = sprint.getUserStories().stream()
                .map(userStory -> {
                    logger.debug("Processing user story ID: {} for sprint: {}", userStory.getId(), sprint.getName());
                    return userStory.getId();
                })
                .collect(Collectors.toList());
        logger.info("Collected {} user story IDs for sprint: {}", userStoryIds.size(), sprint.getName());

        logger.info("Calling task microservice to get task status for project: {}", projectName);
        List<TaskStatusResponse> taskStatus = taskClient.getTaskStatusByProject(project.getName());
        logger.info("Received task status response with {} entries for project: {}", taskStatus.size(), projectName);

        long totalTasks = taskStatus.stream()
                .mapToLong(status -> {
                    logger.debug("Processing task status: {} with count: {}", status.getStatus(), status.getCount());
                    return status.getCount();
                })
                .sum();
        long completedTasks = taskStatus.stream()
                .filter(status -> status.getStatus() == WorkItemStatus.DONE)
                .mapToLong(status -> {
                    logger.debug("Counting completed tasks for status: {} with count: {}", status.getStatus(), status.getCount());
                    return status.getCount();
                })
                .sum();
        double progress = totalTasks > 0 ? (completedTasks * 100.0) / totalTasks : 0.0;
        logger.info("Calculated progress for sprint: {}. Total tasks: {}, Completed tasks: {}, Progress: {}%", sprint.getName(), totalTasks, completedTasks, progress);

        return new SprintSummaryResponse(sprint.getName(), progress, totalTasks, completedTasks, sprint.getEndDate());
    }
}