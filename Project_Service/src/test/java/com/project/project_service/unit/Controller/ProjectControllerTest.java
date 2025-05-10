package com.project.project_service.unit.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.project_service.Controller.ProjectController;
import com.project.project_service.DTO.ManagerDTO;
import com.project.project_service.DTO.ProjectDTO;
import com.project.project_service.DTO.ProjectResponseWithRoleDTO;
import com.project.project_service.DTO.ProjectWithRoleDTO;
import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Enumeration.PhaseProjet;
import com.project.project_service.Enumeration.PriorityProjet;
import com.project.project_service.Enumeration.StatusProjet;
import com.project.project_service.Service.ProjectService;
import com.project.project_service.config.AuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private ProjectController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Set up MockMvc
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Initialize ObjectMapper for JSON serialization
        objectMapper = new ObjectMapper();
    }

    @Test
    void createProject_shouldReturnSuccess_whenValidInput() throws Exception {
        // Arrange
        Map<String, String> projectData = new HashMap<>();
        projectData.put("name", "Test Project");
        projectData.put("description", "Test Description");
        projectData.put("startDate", "2025-05-10");
        projectData.put("deadline", "2025-06-10");
        projectData.put("status", "ACTIVE");
        projectData.put("phase", "PLANNING");
        projectData.put("priority", "HIGH");

        String authorization = "Bearer valid-token";
        String authId = "user123";

        when(authClient.extractUserIdFromToken(authorization)).thenReturn(authId);
        doNothing().when(projectService).createProject(eq(authId), eq("Test Project"), eq("Test Description"),
                eq(LocalDate.parse("2025-05-10")), eq(LocalDate.parse("2025-06-10")), eq("ACTIVE"), eq("PLANNING"), eq("HIGH"));

        // Act & Assert
        mockMvc.perform(post("/api/create-project")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectData)))
                .andExpect(status().isOk())
                .andExpect(content().string("Projet créé avec succès"));

        // Verify interactions
        verify(authClient).extractUserIdFromToken(authorization);
        verify(projectService).createProject(eq(authId), eq("Test Project"), eq("Test Description"),
                eq(LocalDate.parse("2025-05-10")), eq(LocalDate.parse("2025-06-10")), eq("ACTIVE"), eq("PLANNING"), eq("HIGH"));
    }

    @Test
    void createProject_shouldReturnBadRequest_whenMissingFields() throws Exception {
        // Arrange
        Map<String, String> projectData = new HashMap<>();
        projectData.put("name", "Test Project"); // Missing other fields

        String authorization = "Bearer valid-token";
        String authId = "user123";

        when(authClient.extractUserIdFromToken(authorization)).thenReturn(authId);

        // Act & Assert
        mockMvc.perform(post("/api/create-project")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectData)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tous les champs du projet sont requis"));

        // Verify interactions
        verify(authClient).extractUserIdFromToken(authorization);
        verifyNoInteractions(projectService);
    }

    @Test
    void updateProject_shouldReturnSuccess_whenValidInput() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("oldName", "Old Project");
        requestBody.put("newName", "New Project");
        requestBody.put("description", "New Description");
        requestBody.put("startDate", "2025-05-10");
        requestBody.put("deadline", "2025-06-10");
        requestBody.put("status", "ACTIVE");
        requestBody.put("phase", "PLANNING");
        requestBody.put("priority", "HIGH");

        String authorization = "Bearer valid-token";
        String authId = "user123";

        when(authClient.extractUserIdFromToken(authorization)).thenReturn(authId);
        doNothing().when(projectService).updateProject(eq(authId), eq("Old Project"), eq("New Project"),
                eq("New Description"), eq(LocalDate.parse("2025-05-10")), eq(LocalDate.parse("2025-06-10")),
                eq("ACTIVE"), eq("PLANNING"), eq("HIGH"));

        // Act & Assert
        mockMvc.perform(put("/api/modify-project")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Projet modifié avec succès"));

        // Verify interactions
        verify(authClient).extractUserIdFromToken(authorization);
        verify(projectService).updateProject(eq(authId), eq("Old Project"), eq("New Project"),
                eq("New Description"), eq(LocalDate.parse("2025-05-10")), eq(LocalDate.parse("2025-06-10")),
                eq("ACTIVE"), eq("PLANNING"), eq("HIGH"));
    }

    @Test
    void updateProject_shouldReturnBadRequest_whenMissingFields() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("oldName", "Old Project"); // Missing other fields

        String authorization = "Bearer valid-token";
        String authId = "user123";

        when(authClient.extractUserIdFromToken(authorization)).thenReturn(authId);

        // Act & Assert
        mockMvc.perform(put("/api/modify-project")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tous les champs du projet sont requis"));

        // Verify interactions
        verify(authClient).extractUserIdFromToken(authorization);
        verifyNoInteractions(projectService);
    }

    @Test
    void deleteProject_shouldReturnSuccess_whenValidInput() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "Test Project");

        String authorization = "Bearer valid-token";
        String authId = "user123";

        when(authClient.extractUserIdFromToken(authorization)).thenReturn(authId);
        doNothing().when(projectService).deleteProject(authId, "Test Project");

        // Act & Assert
        mockMvc.perform(delete("/api/delete-project")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Projet supprimé avec succès"));

        // Verify interactions
        verify(authClient).extractUserIdFromToken(authorization);
        verify(projectService).deleteProject(authId, "Test Project");
    }

    @Test
    void deleteProject_shouldReturnBadRequest_whenServiceThrowsException() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "Test Project");

        String authorization = "Bearer valid-token";
        String authId = "user123";

        when(authClient.extractUserIdFromToken(authorization)).thenReturn(authId);
        doThrow(new RuntimeException("Project not found")).when(projectService).deleteProject(authId, "Test Project");

        // Act & Assert
        mockMvc.perform(delete("/api/delete-project")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Project not found"));

        // Verify interactions
        verify(authClient).extractUserIdFromToken(authorization);
        verify(projectService).deleteProject(authId, "Test Project");
    }

    @Test
    void getProjectById_shouldReturnProject_whenFound() throws Exception {
        // Arrange
        Long projectId = 1L;
        Client client = new Client();
        client.setId(1L);
        client.setAuthId("manager123");
        client.setRole("MANAGER");

        Projet projet = new Projet();
        projet.setId(projectId);
        projet.setName("Test Project");
        projet.setDescription("Test Description");
        projet.setManager(client);
        projet.setCreationDate(LocalDate.now());
        projet.setStartDate(LocalDate.now());
        projet.setDeadline(LocalDate.now().plusDays(30));
        projet.setStatus(StatusProjet.IN_PROGRESS);
        projet.setPhase(PhaseProjet.DEVELOPPEMENT);
        projet.setPriority(PriorityProjet.HIGH);

        when(projectService.getProjectById(projectId)).thenReturn(projet);

        // Act & Assert
        mockMvc.perform(get("/api/projects/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.manager.authId").value("manager123"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.phase").value("DEVELOPPEMENT"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        // Verify interactions
        verify(projectService).getProjectById(projectId);
    }

    @Test
    void getProjectById_shouldReturnNotFound_whenProjectNotFound() throws Exception {
        // Arrange
        Long projectId = 1L;
        when(projectService.getProjectById(projectId)).thenThrow(new RuntimeException("Project not found"));

        // Act & Assert
        mockMvc.perform(get("/api/projects/{projectId}", projectId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        // Verify interactions
        verify(projectService).getProjectById(projectId);
    }

    @Test
    void getProjectDetails_shouldReturnProject_whenValidInput() throws Exception {
        // Arrange
        Long projectId = 1L;
        String authorization = "Bearer valid-token";
        ProjectDTO projectDTO = new ProjectDTO(
                1L, "Test Project", "Test Description",
                new ManagerDTO(1L, "manager123", null, null, "MANAGER"),
                LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                "ACTIVE", "PLANNING", "HIGH"
        );

        when(projectService.getProjectDetails(projectId, authorization.replace("Bearer ", ""))).thenReturn(projectDTO);

        // Act & Assert
        mockMvc.perform(get("/api/manager/{projectId}", projectId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.manager.authId").value("manager123"));

        // Verify interactions
        verify(projectService).getProjectDetails(projectId, authorization.replace("Bearer ", ""));
    }



    @Test
    void linkGitHubRepository_shouldReturnSuccess_whenValidInput() throws Exception {
        // Arrange
        Long projectId = 1L;
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("repositoryUrl", "https://github.com/test/repo");

        String authorization = "Bearer valid-token";

        doNothing().when(projectService).linkGitHubRepositoryToProject(projectId, "https://github.com/test/repo", authorization);

        // Act & Assert
        mockMvc.perform(post("/api/{projectId}/github-link", projectId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Dépôt GitHub lié avec succès"));

        // Verify interactions
        verify(projectService).linkGitHubRepositoryToProject(projectId, "https://github.com/test/repo", authorization);
    }

    @Test
    void linkGitHubRepository_shouldReturnBadRequest_whenMissingUrl() throws Exception {
        // Arrange
        Long projectId = 1L;
        Map<String, String> requestBody = new HashMap<>();
        // repositoryUrl is missing

        String authorization = "Bearer valid-token";

        // Act & Assert
        mockMvc.perform(post("/api/{projectId}/github-link", projectId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("L'URL du dépôt est requise"));

        // Verify interactions
        verifyNoInteractions(projectService);
    }

    @Test
    void getGitHubRepository_shouldReturnUrl_whenLinked() throws Exception {
        // Arrange
        Long projectId = 1L;
        when(projectService.getGitHubRepositoryUrl(projectId)).thenReturn("https://github.com/test/repo");

        // Act & Assert
        mockMvc.perform(get("/api/{projectId}/github-link", projectId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryUrl").value("https://github.com/test/repo"));

        // Verify interactions
        verify(projectService).getGitHubRepositoryUrl(projectId);
    }

    @Test
    void getGitHubRepository_shouldReturnEmpty_whenNotLinked() throws Exception {
        // Arrange
        Long projectId = 1L;
        when(projectService.getGitHubRepositoryUrl(projectId)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/{projectId}/github-link", projectId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositoryUrl").value(""));

        // Verify interactions
        verify(projectService).getGitHubRepositoryUrl(projectId);
    }

    @Test
    void getGitHubUserId_shouldReturnUserId_whenLinked() throws Exception {
        // Arrange
        Long projectId = 1L;
        Map<String, String> userIdMap = new HashMap<>();
        userIdMap.put("userId", "githubUser123");

        when(projectService.getGitHubUserId(projectId)).thenReturn(userIdMap);

        // Act & Assert
        mockMvc.perform(get("/api/{projectId}/github-user", projectId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("githubUser123"));

        // Verify interactions
        verify(projectService).getGitHubUserId(projectId);
    }

    @Test
    void getGitHubUserId_shouldReturnEmpty_whenNotLinked() throws Exception {
        // Arrange
        Long projectId = 1L;
        when(projectService.getGitHubUserId(projectId)).thenReturn(new HashMap<>());

        // Act & Assert
        mockMvc.perform(get("/api/{projectId}/github-user", projectId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.userId").doesNotExist());

        // Verify interactions
        verify(projectService).getGitHubUserId(projectId);
    }

    @Test
    void getActiveProjectIds_shouldReturnIds_whenProjectsExist() throws Exception {
        // Arrange
        when(projectService.getActiveProjectIds()).thenReturn(List.of(1L, 2L));

        // Act & Assert
        mockMvc.perform(get("/api/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value(1))
                .andExpect(jsonPath("$[1]").value(2));

        // Verify interactions
        verify(projectService).getActiveProjectIds();
    }

    @Test
    void getActiveProjectIds_shouldReturnEmpty_whenNoActiveProjects() throws Exception {
        // Arrange
        when(projectService.getActiveProjectIds()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        // Verify interactions
        verify(projectService).getActiveProjectIds();
    }
}