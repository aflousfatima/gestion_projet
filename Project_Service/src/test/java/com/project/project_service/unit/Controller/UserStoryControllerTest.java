package com.project.project_service.unit.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.project_service.Controller.UserStoryController;
import com.project.project_service.DTO.UserStoryDTO;
import com.project.project_service.DTO.UserStoryHistoryDto;
import com.project.project_service.DTO.UserStoryRequest;
import com.project.project_service.Service.HistoryService;
import com.project.project_service.Service.UserStoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserStoryControllerTest {

    @Mock
    private UserStoryService userStoryService;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private UserStoryController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Set up MockMvc
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Initialize ObjectMapper with JavaTimeModule for LocalDate support
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createUserStory_shouldReturnCreated_whenValidInput() throws Exception {
        // Arrange
        Long projectId = 1L;
        UserStoryRequest request = new UserStoryRequest(
                "New Story",
                "Description for New Story",
                "HIGH",
                "TODO",
                5,
                Collections.singletonList(2L),
                Collections.singletonList("feature")
        );

        UserStoryDTO createdUserStory = new UserStoryDTO();
        createdUserStory.setId(1L);
        createdUserStory.setTitle("New Story");
        createdUserStory.setDescription("Description for New Story");
        createdUserStory.setPriority("HIGH");
        createdUserStory.setEffortPoints(5);
        createdUserStory.setStatus("TODO");
        createdUserStory.setDependsOn(Collections.singletonList(2L));
        createdUserStory.setProjectId(projectId);
        createdUserStory.setTags(Collections.singletonList("feature"));

        String authorization = "Bearer valid-token";

        when(userStoryService.createUserStory(eq(projectId), any(UserStoryRequest.class), eq(authorization)))
                .thenReturn(createdUserStory);

        // Act & Assert
        mockMvc.perform(post("/api/projects/{projectId}/user-stories", projectId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("New Story"))
                .andExpect(jsonPath("$.description").value("Description for New Story"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.effortPoints").value(5))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.projectId").value(projectId));

        // Verify interactions
        ArgumentCaptor<UserStoryRequest> requestCaptor = ArgumentCaptor.forClass(UserStoryRequest.class);
        verify(userStoryService).createUserStory(eq(projectId), requestCaptor.capture(), eq(authorization));
        UserStoryRequest capturedRequest = requestCaptor.getValue();
        assertEquals(request.title(), capturedRequest.title());
        assertEquals(request.description(), capturedRequest.description());
        assertEquals(request.priority(), capturedRequest.priority());
        assertEquals(request.effortPoints(), capturedRequest.effortPoints());
        assertEquals(request.status(), capturedRequest.status());
        assertEquals(request.dependsOn(), capturedRequest.dependsOn());
        assertEquals(request.tags(), capturedRequest.tags());
    }

    @Test
    void createUserStory_shouldReturnUnauthorized_whenInvalidToken() throws Exception {
        // Arrange
        Long projectId = 1L;
        UserStoryRequest request = new UserStoryRequest(
                "New Story",
                "Description for New Story",
                "HIGH",
                "TODO",
                5,
                null,
                null
        );

        String authorization = "Bearer invalid-token";

        when(userStoryService.createUserStory(eq(projectId), any(UserStoryRequest.class), eq(authorization)))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act & Assert
        mockMvc.perform(post("/api/projects/{projectId}/user-stories", projectId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        // Verify interactions
        verify(userStoryService).createUserStory(eq(projectId), any(UserStoryRequest.class), eq(authorization));
    }

    @Test
    void createUserStory_shouldReturnBadRequest_whenInvalidInput() throws Exception {
        // Arrange
        Long projectId = 1L;
        UserStoryRequest request = new UserStoryRequest(
                "", // Invalid title
                "Description",
                "HIGH",
                "TODO",
                5,
                null,
                null
        );

        String authorization = "Bearer valid-token";

        when(userStoryService.createUserStory(eq(projectId), any(UserStoryRequest.class), eq(authorization)))
                .thenThrow(new RuntimeException("Invalid input"));

        // Act & Assert
        mockMvc.perform(post("/api/projects/{projectId}/user-stories", projectId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify interactions
        verify(userStoryService).createUserStory(eq(projectId), any(UserStoryRequest.class), eq(authorization));
    }

    @Test
    void updateUserStory_shouldReturnOk_whenValidInput() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        UserStoryRequest request = new UserStoryRequest(
                "Updated Story",
                "Updated description",
                "MEDIUM",
                "IN_PROGRESS",
                3,
                Collections.singletonList(2L),
                Collections.singletonList("enhancement")
        );

        UserStoryDTO updatedUserStory = new UserStoryDTO();
        updatedUserStory.setId(userStoryId);
        updatedUserStory.setTitle("Updated Story");
        updatedUserStory.setDescription("Updated description");
        updatedUserStory.setPriority("MEDIUM");
        updatedUserStory.setEffortPoints(3);
        updatedUserStory.setStatus("IN_PROGRESS");
        updatedUserStory.setDependsOn(Collections.singletonList(2L));
        updatedUserStory.setProjectId(projectId);
        updatedUserStory.setTags(Collections.singletonList("enhancement"));

        String authorization = "Bearer valid-token";

        when(userStoryService.updateUserStory(eq(projectId), eq(userStoryId), any(UserStoryRequest.class), eq(authorization)))
                .thenReturn(updatedUserStory);

        // Act & Assert
        mockMvc.perform(put("/api/projects/{projectId}/user-stories/{userStoryId}", projectId, userStoryId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userStoryId))
                .andExpect(jsonPath("$.title").value("Updated Story"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.effortPoints").value(3))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Verify interactions
        ArgumentCaptor<UserStoryRequest> requestCaptor = ArgumentCaptor.forClass(UserStoryRequest.class);
        verify(userStoryService).updateUserStory(eq(projectId), eq(userStoryId), requestCaptor.capture(), eq(authorization));
        UserStoryRequest capturedRequest = requestCaptor.getValue();
        assertEquals(request.title(), capturedRequest.title());
        assertEquals(request.description(), capturedRequest.description());
        assertEquals(request.priority(), capturedRequest.priority());
        assertEquals(request.effortPoints(), capturedRequest.effortPoints());
        assertEquals(request.status(), capturedRequest.status());
        assertEquals(request.dependsOn(), capturedRequest.dependsOn());
        assertEquals(request.tags(), capturedRequest.tags());
    }

    @Test
    void updateUserStory_shouldReturnNotFound_whenUserStoryNotFound() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        UserStoryRequest request = new UserStoryRequest(
                "Updated Story",
                "Updated description",
                "MEDIUM",
                "IN_PROGRESS",
                3,
                null,
                null
        );

        String authorization = "Bearer valid-token";

        when(userStoryService.updateUserStory(eq(projectId), eq(userStoryId), any(UserStoryRequest.class), eq(authorization)))
                .thenThrow(new RuntimeException("User story not found"));

        // Act & Assert
        mockMvc.perform(put("/api/projects/{projectId}/user-stories/{userStoryId}", projectId, userStoryId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        // Verify interactions
        verify(userStoryService).updateUserStory(eq(projectId), eq(userStoryId), any(UserStoryRequest.class), eq(authorization));
    }

    @Test
    void getUserStories_shouldReturnOk_whenStoriesExist() throws Exception {
        // Arrange
        Long projectId = 1L;
        UserStoryDTO userStory = new UserStoryDTO();
        userStory.setId(1L);
        userStory.setTitle("Story 1");
        userStory.setDescription("Description");
        userStory.setPriority("HIGH");
        userStory.setEffortPoints(5);
        userStory.setStatus("TODO");
        userStory.setProjectId(projectId);

        List<UserStoryDTO> userStories = Collections.singletonList(userStory);

        when(userStoryService.getUserStoriesByProjectId(projectId)).thenReturn(userStories);

        // Act & Assert
        mockMvc.perform(get("/api/projects/{projectId}/user-stories", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Story 1"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));

        // Verify interactions
        verify(userStoryService).getUserStoriesByProjectId(projectId);
    }

    @Test
    void getUserStories_shouldReturnNotFound_whenNoStories() throws Exception {
        // Arrange
        Long projectId = 1L;

        when(userStoryService.getUserStoriesByProjectId(projectId))
                .thenThrow(new RuntimeException("No user stories found"));

        // Act & Assert
        mockMvc.perform(get("/api/projects/{projectId}/user-stories", projectId))
                .andExpect(status().isNotFound());

        // Verify interactions
        verify(userStoryService).getUserStoriesByProjectId(projectId);
    }

    @Test
    void deleteUserStory_shouldReturnNoContent_whenSuccessful() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        String authorization = "Bearer valid-token";

        doNothing().when(userStoryService).deleteUserStory(projectId, userStoryId);

        // Act & Assert
        mockMvc.perform(delete("/api/projects/{projectId}/user-stories/{userStoryId}", projectId, userStoryId)
                        .header("Authorization", authorization))
                .andExpect(status().isNoContent());

        // Verify interactions
        verify(userStoryService).deleteUserStory(projectId, userStoryId);
    }

    @Test
    void deleteUserStory_shouldReturnNotFound_whenUserStoryNotFound() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        String authorization = "Bearer valid-token";

        doThrow(new RuntimeException("User story not found")).when(userStoryService).deleteUserStory(projectId, userStoryId);

        // Act & Assert
        mockMvc.perform(delete("/api/projects/{projectId}/user-stories/{userStoryId}", projectId, userStoryId)
                        .header("Authorization", authorization))
                .andExpect(status().isNotFound());

        // Verify interactions
        verify(userStoryService).deleteUserStory(projectId, userStoryId);
    }

    @Test
    void assignUserStoryToSprint_shouldReturnOk_whenSuccessful() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        Long sprintId = 1L;
        String authorization = "Bearer valid-token";

        UserStoryDTO updatedUserStory = new UserStoryDTO();
        updatedUserStory.setId(userStoryId);
        updatedUserStory.setTitle("Story 1");
        updatedUserStory.setSprintId(sprintId);
        updatedUserStory.setProjectId(projectId);

        when(userStoryService.assignUserStoryToSprint(eq(projectId), eq(userStoryId), eq(sprintId), eq(authorization)))
                .thenReturn(updatedUserStory);

        // Act & Assert
        mockMvc.perform(put("/api/projects/{projectId}/user-stories/{userStoryId}/assign-sprint/{sprintId}", projectId, userStoryId, sprintId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userStoryId))
                .andExpect(jsonPath("$.sprintId").value(sprintId));

        // Verify interactions
        verify(userStoryService).assignUserStoryToSprint(projectId, userStoryId, sprintId, authorization);
    }

    @Test
    void assignUserStoryToSprint_shouldReturnUnauthorized_whenInvalidToken() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        Long sprintId = 1L;
        String authorization = "Bearer invalid-token";

        when(userStoryService.assignUserStoryToSprint(eq(projectId), eq(userStoryId), eq(sprintId), eq(authorization)))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        // Act & Assert
        mockMvc.perform(put("/api/projects/{projectId}/user-stories/{userStoryId}/assign-sprint/{sprintId}", projectId, userStoryId, sprintId)
                        .header("Authorization", authorization))
                .andExpect(status().isUnauthorized());

        // Verify interactions
        verify(userStoryService).assignUserStoryToSprint(projectId, userStoryId, sprintId, authorization);
    }

    @Test
    void removeUserStoryFromSprint_shouldReturnOk_whenSuccessful() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        String authorization = "Bearer valid-token";

        UserStoryDTO updatedUserStory = new UserStoryDTO();
        updatedUserStory.setId(userStoryId);
        updatedUserStory.setTitle("Story 1");
        updatedUserStory.setSprintId(null);
        updatedUserStory.setProjectId(projectId);

        when(userStoryService.removeUserStoryFromSprint(eq(projectId), eq(userStoryId), eq(authorization)))
                .thenReturn(updatedUserStory);

        // Act & Assert
        mockMvc.perform(put("/api/projects/{projectId}/user-stories/{userStoryId}/remove-sprint", projectId, userStoryId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userStoryId))
                .andExpect(jsonPath("$.sprintId").doesNotExist());

        // Verify interactions
        verify(userStoryService).removeUserStoryFromSprint(projectId, userStoryId, authorization);
    }

    @Test
    void updateDependencies_shouldReturnOk_whenSuccessful() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        Map<String, List<Long>> requestBody = new HashMap<>();
        requestBody.put("dependsOn", Arrays.asList(2L, 3L));

        UserStoryDTO updatedUserStory = new UserStoryDTO();
        updatedUserStory.setId(userStoryId);
        updatedUserStory.setTitle("Story 1");
        updatedUserStory.setDependsOn(Arrays.asList(2L, 3L));
        updatedUserStory.setProjectId(projectId);

        String authorization = "Bearer valid-token";

        when(userStoryService.updateDependencies(eq(projectId), eq(userStoryId), eq(Arrays.asList(2L, 3L)), eq(authorization)))
                .thenReturn(updatedUserStory);

        // Act & Assert
        mockMvc.perform(put("/api/projects/{projectId}/user-stories/{userStoryId}/dependencies", projectId, userStoryId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userStoryId))
                .andExpect(jsonPath("$.dependsOn[0]").value(2L))
                .andExpect(jsonPath("$.dependsOn[1]").value(3L));

        // Verify interactions
        verify(userStoryService).updateDependencies(eq(projectId), eq(userStoryId), eq(Arrays.asList(2L, 3L)), eq(authorization));
    }

    @Test
    void updateTags_shouldReturnOk_whenSuccessful() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        Map<String, List<String>> requestBody = new HashMap<>();
        requestBody.put("tags", Arrays.asList("feature", "enhancement"));

        UserStoryDTO updatedUserStory = new UserStoryDTO();
        updatedUserStory.setId(userStoryId);
        updatedUserStory.setTitle("Story 1");
        updatedUserStory.setTags(Arrays.asList("feature", "enhancement"));
        updatedUserStory.setProjectId(projectId);

        String authorization = "Bearer valid-token";

        when(userStoryService.updateTags(eq(projectId), eq(userStoryId), eq(Arrays.asList("feature", "enhancement")), eq(authorization)))
                .thenReturn(updatedUserStory);

        // Act & Assert
        mockMvc.perform(put("/api/projects/{projectId}/user-stories/{userStoryId}/tags", projectId, userStoryId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userStoryId))
                .andExpect(jsonPath("$.tags[0]").value("feature"))
                .andExpect(jsonPath("$.tags[1]").value("enhancement"));

        // Verify interactions
        verify(userStoryService).updateTags(eq(projectId), eq(userStoryId), eq(Arrays.asList("feature", "enhancement")), eq(authorization));
    }


    @Test
    void getUserStoryHistory_shouldReturnNoContent_whenNoHistory() throws Exception {
        // Arrange
        Long userStoryId = 1L;
        String authorization = "Bearer valid-token";

        when(historyService.getUserStoryHistoryWithAuthorNames(eq(userStoryId), eq("valid-token")))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/projects/user-story/{userStoryId}/history", userStoryId)
                        .header("Authorization", authorization))
                .andExpect(status().isNoContent());

        // Verify interactions
        verify(historyService).getUserStoryHistoryWithAuthorNames(eq(userStoryId), eq("valid-token"));
    }

    @Test
    void getUserStoryIdsOfActiveSprint_shouldReturnIds_whenActiveSprintExists() throws Exception {
        // Arrange
        Long projectId = 1L;
        List<Long> userStoryIds = Arrays.asList(1L, 2L);

        when(userStoryService.getUserStoryIdsOfActiveSprint(projectId)).thenReturn(userStoryIds);

        // Act & Assert
        mockMvc.perform(get("/api/projects/{projectId}/sprint/actif/user_stories", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(1L))
                .andExpect(jsonPath("$[1]").value(2L));

        // Verify interactions
        verify(userStoryService).getUserStoryIdsOfActiveSprint(projectId);
    }

    @Test
    void checkAndUpdateUserStoryStatus_shouldReturnOk_whenSuccessful() throws Exception {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 1L;
        String authorization = "Bearer valid-token";

        UserStoryDTO updatedUserStory = new UserStoryDTO();
        updatedUserStory.setId(userStoryId);
        updatedUserStory.setTitle("Story 1");
        updatedUserStory.setStatus("DONE");
        updatedUserStory.setProjectId(projectId);

        when(userStoryService.checkAndUpdateUserStoryStatus(eq(projectId), eq(userStoryId), eq(authorization)))
                .thenReturn(updatedUserStory);

        // Act & Assert
        mockMvc.perform(post("/api/projects/{projectId}/{userStoryId}/check-status", projectId, userStoryId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userStoryId))
                .andExpect(jsonPath("$.status").value("DONE"));

        // Verify interactions
        verify(userStoryService).checkAndUpdateUserStoryStatus(projectId, userStoryId, authorization);
    }
}