package com.project.project_service.unit.Service;
import com.project.project_service.DTO.InitialProjectManagementDTO;
import com.project.project_service.DTO.ProjectDTO;
import com.project.project_service.DTO.ProjectResponseDTO;
import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Team;
import com.project.project_service.Repository.ClientRepository;
import com.project.project_service.Repository.EntrepriseRepository;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.TeamRepository;
import com.project.project_service.Service.InitialProjectManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import com.project.project_service.Enumeration.PhaseProjet;
import com.project.project_service.Enumeration.PriorityProjet;
import com.project.project_service.Enumeration.StatusProjet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialProjectManagementServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EntrepriseRepository companyRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ProjetRepository projectRepository;

    @InjectMocks
    private InitialProjectManagementService service;

    private InitialProjectManagementDTO dto;
    private Client client;
    private Entreprise company;
    private Projet project;
    private String authId;

    @BeforeEach
    void setUp() {
        authId = "auth123";
        dto = new InitialProjectManagementDTO();
        dto.setCompanyName("Test Company");
        dto.setIndustry("Tech");
        dto.setTeamName("Test Team");
        dto.setNumEmployees("10");
        dto.setProjectName("Test Project");
        dto.setProjectDescription("Description");
        dto.setRole("Manager");
        dto.setDepartment("IT");

        client = new Client();
        client.setId(1L);
        client.setAuthId(authId);
        client.setRole("Manager");
        client.setDepartment("IT");

        company = new Entreprise();
        company.setId(1L);
        company.setName("Test Company");
        company.setIndustry("Tech");

        project = new Projet();
        project.setId(1L);
        project.setName("Test Project");
        project.setDescription("Description");
        project.setCreationDate(LocalDate.now());
        project.setStatus(StatusProjet.START);   // Utilisation de l'enum StatusProjet
        project.setPhase(PhaseProjet.PLANIFICATION);     // Utilisation de l'enum PhaseProjet
        project.setPriority(PriorityProjet.MEDIUM);
    }

    @Test
    void testCreateCompanyTeamProject_NewClient() {
        // Arrange
        when(clientRepository.findByAuthId(authId)).thenReturn(null);
        when(clientRepository.saveAndFlush(any(Client.class))).thenReturn(client);
        when(companyRepository.saveAndFlush(any(Entreprise.class))).thenReturn(company);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(teamRepository.save(any(Team.class))).thenReturn(new Team());
        when(projectRepository.save(any(Projet.class))).thenReturn(project);

        // Act
        service.createCompanyTeamProject(dto, authId);

        // Assert
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(clientRepository, times(1)).saveAndFlush(any(Client.class));
        verify(companyRepository, times(1)).saveAndFlush(any(Entreprise.class));
        verify(clientRepository, times(1)).save(any(Client.class));
        verify(teamRepository, times(1)).save(any(Team.class));
        verify(projectRepository, times(1)).save(any(Projet.class));
    }

    @Test
    void testCreateCompanyTeamProject_ExistingClient() {
        // Arrange
        client.setCompany(null); // No company initially
        when(clientRepository.findByAuthId(authId)).thenReturn(client);
        when(companyRepository.saveAndFlush(any(Entreprise.class))).thenReturn(company);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(teamRepository.save(any(Team.class))).thenReturn(new Team());
        when(projectRepository.save(any(Projet.class))).thenReturn(project);

        // Act
        service.createCompanyTeamProject(dto, authId);

        // Assert
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(clientRepository, times(0)).saveAndFlush(any(Client.class)); // No new client creation
        verify(companyRepository, times(1)).saveAndFlush(any(Entreprise.class));
        verify(clientRepository, times(1)).save(any(Client.class));
        verify(teamRepository, times(1)).save(any(Team.class));
        verify(projectRepository, times(1)).save(any(Projet.class));
    }

    @Test
    void testGetProjectsByManager_Success() {
        // Arrange
        client.setCompany(company);
        when(clientRepository.findByAuthId(authId)).thenReturn(client);
        when(projectRepository.findByCompany(company)).thenReturn(Collections.singletonList(project));

        // Act
        ProjectResponseDTO response = service.getProjectsByManager(authId);

        // Assert
        assertNotNull(response);
        assertEquals("Test Company", response.getCompanyName());
        assertEquals(1, response.getProjects().size());
        ProjectDTO projectDTO = response.getProjects().get(0);
        assertEquals(project.getName(), projectDTO.getName());
        assertEquals(project.getDescription(), projectDTO.getDescription());
        assertEquals(project.getStatus().name(), projectDTO.getStatus());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, times(1)).findByCompany(company);
    }

    @Test
    void testGetProjectsByManager_ManagerNotFound() {
        // Arrange
        when(clientRepository.findByAuthId(authId)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.getProjectsByManager(authId);
        });
        assertEquals("Manager non trouvé", exception.getMessage());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, never()).findByCompany(any());
    }

    @Test
    void testGetProjectsByManager_NoCompanyAssociated() {
        // Arrange
        client.setCompany(null);
        when(clientRepository.findByAuthId(authId)).thenReturn(client);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.getProjectsByManager(authId);
        });
        assertEquals("Aucune entreprise associée", exception.getMessage());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, never()).findByCompany(any());
    }
}