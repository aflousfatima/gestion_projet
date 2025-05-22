package com.project.project_service.Chatbot.Controller;

import com.project.project_service.Chatbot.DTO.ProjectRemainingTimeResponse;
import com.project.project_service.Chatbot.DTO.SprintSummaryResponse;
import com.project.project_service.Chatbot.DTO.UserStoryResponse;
import com.project.project_service.Chatbot.Service.ChatbotService;
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

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping("/remaining-time")
    public ResponseEntity<ProjectRemainingTimeResponse> getProjectRemainingTime(@RequestParam("projectId") Long projectId) {
        try {
            ProjectRemainingTimeResponse response = chatbotService.getProjectRemainingTime(projectId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/sprint/user-stories")
    public ResponseEntity<List<UserStoryResponse>> getUserStoriesInCurrentSprint(@RequestParam("projectId") Long projectId) {
        try {
            List<UserStoryResponse> userStories = chatbotService.getUserStoriesInCurrentSprint(projectId);
            if (userStories.isEmpty()) {
                throw new RuntimeException("Aucune User Story trouvée dans le sprint actif");
            }
            return ResponseEntity.ok(userStories);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/sprint/summary")
    public ResponseEntity<SprintSummaryResponse> getCurrentSprintSummary(@RequestParam("projectId") Long projectId) {
        try {
            SprintSummaryResponse summary = chatbotService.getCurrentSprintSummary(projectId);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}