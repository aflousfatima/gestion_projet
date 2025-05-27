package com.project.project_service.Chatbot.Controller;

import com.project.project_service.Chatbot.DTO.ProjectRemainingTimeResponse;
import com.project.project_service.Chatbot.DTO.SprintSummaryResponse;
import com.project.project_service.Chatbot.DTO.UserStoryResponse;
import com.project.project_service.Chatbot.Service.ChatbotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/chatbot/projects")
public class ChatbotController {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping("/remaining-time")
    public ResponseEntity<ProjectRemainingTimeResponse> getProjectRemainingTime(@RequestParam("projectName") String projectName) {
        logger.info("Received request to get remaining time for project: {}", projectName);
        try {
            ProjectRemainingTimeResponse response = chatbotService.getProjectRemainingTime(projectName);
            logger.info("Successfully retrieved remaining time for project: {}", projectName);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Failed to get remaining time for project: {}. Error: {}", projectName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/sprint/user-stories")
    public ResponseEntity<List<UserStoryResponse>> getUserStoriesInCurrentSprint(@RequestParam("projectName") String projectName) {
        logger.info("Received request to get user stories for project: {}", projectName);
        try {
            List<UserStoryResponse> userStories = chatbotService.getUserStoriesInCurrentSprint(projectName);
            if (userStories.isEmpty()) {
                logger.warn("No user stories found in active sprint for project: {}", projectName);
                throw new RuntimeException("Aucune User Story trouvée dans le sprint actif");
            }
            logger.info("Successfully retrieved {} user stories for project: {}", userStories.size(), projectName);
            return ResponseEntity.ok(userStories);
        } catch (RuntimeException e) {
            logger.error("Failed to get user stories for project: {}. Error: {}", projectName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/sprint/summary")
    public ResponseEntity<SprintSummaryResponse> getCurrentSprintSummary(@RequestParam("projectName") String projectName) {
        logger.info("Received request to get sprint summary for project: {}", projectName);
        try {
            SprintSummaryResponse summary = chatbotService.getCurrentSprintSummary(projectName);
            logger.info("Successfully retrieved sprint summary for project: {}", projectName);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            logger.error("Failed to get sprint summary for project: {}. Error: {}", projectName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}