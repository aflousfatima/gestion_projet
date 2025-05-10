package com.project.project_service.unit.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.project_service.Controller.InitialProjectManagementController;
import com.project.project_service.DTO.InitialProjectManagementDTO;
import com.project.project_service.DTO.ManagerDTO;
import com.project.project_service.DTO.ProjectDTO;
import com.project.project_service.DTO.ProjectResponseDTO;
import com.project.project_service.Service.InitialProjectManagementService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class InitialProjectManagementControllerTest {

    @Mock
    private InitialProjectManagementService initialProjectManagementService;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private InitialProjectManagementController controller;

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
    void createInitialProject_shouldReturnSuccess_whenValidInput() throws Exception {
        // Arrange
        InitialProjectManagementDTO dto = new InitialProjectManagementDTO();
        dto.setCompanyName("Test Company");
        dto.setProjectName("Test Project");
        dto.setTeamName("Test Team");

        String authorization = "Bearer valid-token";
        String authId = "user123";

        when(authClient.extractUserIdFromToken(authorization)).thenReturn(authId);
        doNothing().when(initialProjectManagementService).createCompanyTeamProject(any(InitialProjectManagementDTO.class), eq(authId));

        // Act & Assert
        mockMvc.perform(post("/api/create-initial-project")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Entreprise, équipe et projet créés avec succès."));

        // Verify interactions
        verify(authClient).extractUserIdFromToken(authorization);
        verify(initialProjectManagementService).createCompanyTeamProject(any(InitialProjectManagementDTO.class), eq(authId));
    }

    @Test
    void createInitialProject_shouldReturnError_whenServiceThrowsException() throws Exception {
        // Arrange
        InitialProjectManagementDTO dto = new InitialProjectManagementDTO();
        dto.setCompanyName("Test Company");
        dto.setProjectName("Test Project");
        dto.setTeamName("Test Team");

        String authorization = "Bearer valid-token";
        String authId = "user123";

        when(authClient.extractUserIdFromToken(authorization)).thenReturn(authId);
        doThrow(new RuntimeException("Service error")).when(initialProjectManagementService)
                .createCompanyTeamProject(any(InitialProjectManagementDTO.class), eq(authId));

        // Act & Assert
        mockMvc.perform(post("/api/create-initial-project")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur lors de la création des entités."));

        // Verify interactions
        verify(authClient).extractUserIdFromToken(authorization);
        verify(initialProjectManagementService).createCompanyTeamProject(any(InitialProjectManagementDTO.class), eq(authId));
    }


    @Test
    void getProjectsByManager_shouldReturnProjects_whenValidAuthId() throws Exception {
        // Arrange
        String authId = "manager123";
        ManagerDTO manager = new ManagerDTO(2l,"urtw-45","firstname","lastname","DEVELOPER");
        ProjectResponseDTO responseDTO = new ProjectResponseDTO(
                "Test Company",
                List.of(
                        new ProjectDTO(1L, "Project A", "Description A", manager,
                                LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                                "ACTIVE", "PLANNING", "HIGH"),
                        new ProjectDTO(2L, "Project B", "Description B", manager,
                                LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                                "ACTIVE", "PLANNING", "MEDIUM")
                )
        );

        when(initialProjectManagementService.getProjectsByManager(authId)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/projects/by-manager")
                        .param("authId", authId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Test Company"))
                .andExpect(jsonPath("$.projects").isArray())
                .andExpect(jsonPath("$.projects[0].name").value("Project A"))
                .andExpect(jsonPath("$.projects[1].name").value("Project B"));

        // Verify interactions
        verify(initialProjectManagementService).getProjectsByManager(authId);
    }
    @Test
    void getProjectsByManager_shouldReturnBadRequest_whenServiceThrowsException() throws Exception {
        // Arrange
        String authId = "manager123";

        when(initialProjectManagementService.getProjectsByManager(authId))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/api/projects/by-manager")
                        .param("authId", authId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));

        // Verify interactions
        verify(initialProjectManagementService).getProjectsByManager(authId);
    }
}