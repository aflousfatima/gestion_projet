package com.project.project_service.unit.Service;


import com.project.project_service.DTO.ManagerDTO;
import com.project.project_service.DTO.ProjectDTO;
import com.project.project_service.DTO.ProjectMemberDTO;
import com.project.project_service.DTO.ProjectResponseWithRoleDTO;
import com.project.project_service.DTO.ProjectWithRoleDTO;
import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.GitHubLink;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Enumeration.PhaseProjet;
import com.project.project_service.Enumeration.PriorityProjet;
import com.project.project_service.Enumeration.StatusProjet;
import com.project.project_service.Repository.ClientRepository;
import com.project.project_service.Repository.GitHubLinkRepository;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.config.AuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.project.project_service.Service.ProjectService;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ProjetRepository projectRepository;

    @Mock
    private AuthClient authClient;

    @Mock
    private GitHubLinkRepository gitHubLinkRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProjectService projectService;

    private Client manager;
    private Entreprise company;
    private Projet project;
    private String authId;
    private LocalDate startDate;
    private LocalDate deadline;

    @BeforeEach
    void setUp() {
        authId = "auth123";
        manager = new Client();
        manager.setId(1L);
        manager.setAuthId(authId);
        manager.setRole("Manager");

        company = new Entreprise();
        company.setId(1L);
        company.setName("Test Company");
        manager.setCompany(company);

        project = new Projet();
        project.setId(1L);
        project.setName("Test Project");
        project.setDescription("Description");
        project.setCompany(company);
        project.setManager(manager);
        project.setCreationDate(LocalDate.now());
        project.setStatus(StatusProjet.START);
        project.setPhase(PhaseProjet.PLANIFICATION);
        project.setPriority(PriorityProjet.MEDIUM);

        startDate = LocalDate.now().plusDays(1);
        deadline = LocalDate.now().plusDays(10);
    }

    @Test
    void testCreateProject_Success() {
        // Arrange
        when(clientRepository.findByAuthId(authId)).thenReturn(manager);
        when(projectRepository.save(any(Projet.class))).thenReturn(project);

        // Act
        projectService.createProject(authId, "Test Project", "Description", startDate, deadline,
                "IN_PROGRESS", "PLANIFICATION", "MEDIUM");

        // Assert
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, times(1)).save(any(Projet.class));
    }
    @Test
    void testCreateProject_ManagerNotFound() {
        // Arrange
        when(clientRepository.findByAuthId(authId)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.createProject(authId, "Test Project", "Description", startDate, deadline,
                    "NOT_STARTED", "INITIATION", "MEDIUM");
        });
        assertEquals("Manager non trouvé", exception.getMessage());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testCreateProject_NoCompanyAssociated() {
        // Arrange
        manager.setCompany(null);
        when(clientRepository.findByAuthId(authId)).thenReturn(manager);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.createProject(authId, "Test Project", "Description", startDate, deadline,
                    "NOT_STARTED", "INITIATION", "MEDIUM");
        });
        assertEquals("Aucune entreprise associée au manager", exception.getMessage());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, never()).save(any());
    }



    @Test
    void testUpdateProject_ProjectNotFound() {
        // Arrange
        when(clientRepository.findByAuthId(authId)).thenReturn(manager);
        when(projectRepository.findByNameAndCompany("Test Project", company)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.updateProject(authId, "Test Project", "New Project", "New Description",
                    startDate, deadline, "IN_PROGRESS", "PLANNING", "HIGH");
        });
        assertEquals("Projet non trouvé ou vous n'avez pas les droits pour le modifier", exception.getMessage());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, times(1)).findByNameAndCompany("Test Project", company);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void testDeleteProject_Success() {
        // Arrange
        when(clientRepository.findByAuthId(authId)).thenReturn(manager);
        when(projectRepository.findByNameAndCompany("Test Project", company)).thenReturn(project);

        // Act
        projectService.deleteProject(authId, "Test Project");

        // Assert
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, times(1)).findByNameAndCompany("Test Project", company);
        verify(projectRepository, times(1)).delete(project);
    }

    @Test
    void testDeleteProject_ProjectNotFound() {
        // Arrange
        when(clientRepository.findByAuthId(authId)).thenReturn(manager);
        when(projectRepository.findByNameAndCompany("Test Project", company)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.deleteProject(authId, "Test Project");
        });
        assertEquals("Projet non trouvé ou vous n'avez pas les droits pour le supprimer", exception.getMessage());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, times(1)).findByNameAndCompany("Test Project", company);
        verify(projectRepository, never()).delete(any());
    }

    @Test
    void testGetProjectById_Success() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // Act
        Projet result = projectService.getProjectById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        verify(projectRepository, times(1)).findById(1L);
    }

    @Test
    void testGetProjectById_NotFound() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            projectService.getProjectById(1L);
        });
        assertEquals("Projet non trouvé avec l'ID : 1", exception.getMessage());
        verify(projectRepository, times(1)).findById(1L);
    }

    @Test
    void testGetManagerByProject_Success() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("firstName", "John");
        userInfo.put("lastName", "Doe");
        userInfo.put("avatar", "avatar.png");
        when(authClient.getUserDetailsByAuthId(authId, "Bearer token")).thenReturn(userInfo);

        // Act
        Map<String, Object> result = projectService.getManagerByProject("token", 1L);

        // Assert
        assertNotNull(result);
        assertEquals("1", result.get("id"));
        assertEquals(authId, result.get("authId"));
        assertEquals("John", result.get("firstName"));
        verify(projectRepository, times(1)).findById(1L);
        verify(authClient, times(1)).getUserDetailsByAuthId(authId, "Bearer token");
    }

    @Test
    void testGetManagerByProject_NoManager() {
        // Arrange
        project.setManager(null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        // Act
        Map<String, Object> result = projectService.getManagerByProject("token", 1L);

        // Assert
        assertNull(result);
        verify(projectRepository, times(1)).findById(1L);
        verify(authClient, never()).getUserDetailsByAuthId(anyString(), anyString());
    }

    @Test
    void testGetProjectDetails_Success() {
        // Arrange
        project.setStartDate(startDate); // Ensure startDate is set
        project.setDeadline(deadline);   // Ensure deadline is set
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", "1");
        userInfo.put("authId", authId);
        userInfo.put("firstName", "John");
        userInfo.put("lastName", "Doe");
        userInfo.put("role", "Manager");
        when(authClient.getUserDetailsByAuthId(authId, "Bearer token")).thenReturn(userInfo);

        // Act
        ProjectDTO result = projectService.getProjectDetails(1L, "token");

        // Assert
        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        assertNotNull(result.getManager());
        assertEquals("John", result.getManager().getFirstName());
        verify(projectRepository, times(2)).findById(1L); // Expect 2 calls due to getManagerByProject
        verify(authClient, times(1)).getUserDetailsByAuthId(authId, "Bearer token");
    }

    @Test
    void testUpdateProject_Success() {
        // Arrange
        when(clientRepository.findByAuthId(authId)).thenReturn(manager);
        when(projectRepository.findByNameAndCompany("Test Project", company)).thenReturn(project);
        when(projectRepository.save(any(Projet.class))).thenReturn(project);

        // Act
        projectService.updateProject(authId, "Test Project", "New Project", "New Description",
                startDate, deadline, "IN_PROGRESS", "DEVELOPPEMENT", "HIGH");

        // Assert
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, times(1)).findByNameAndCompany("Test Project", company);
        verify(projectRepository, times(1)).save(any(Projet.class));
    }

    @Test
    void testGetProjectsByUser_MemberOnly() {
        // Arrange
        project.setStartDate(startDate); // Ensure startDate is set
        project.setDeadline(deadline);   // Ensure deadline is set
        when(clientRepository.findByAuthId(authId)).thenReturn(null);
        ProjectMemberDTO memberDTO = new ProjectMemberDTO(1L, "Developer");
        when(authClient.getProjectMembersByUserId(authId)).thenReturn(Collections.singletonList(memberDTO));
        when(projectRepository.findAllById(Collections.singletonList(1L))).thenReturn(Collections.singletonList(project));

        // Act
        ProjectResponseWithRoleDTO result = projectService.getProjectsByUser(authId);

        // Assert
        assertNotNull(result);
        assertEquals("N/A", result.getCompanyName());
        assertEquals(1, result.getProjects().size());
        assertEquals("Developer", result.getProjects().get(0).getRoleInProject());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(authClient, times(1)).getProjectMembersByUserId(authId);
        verify(projectRepository, times(1)).findAllById(Collections.singletonList(1L));
    }

    @Test
    void testGetProjectsByUser_ManagerOnly() {
        // Arrange
        project.setStartDate(startDate); // Ensure startDate is set
        project.setDeadline(deadline);   // Ensure deadline is set
        when(clientRepository.findByAuthId(authId)).thenReturn(manager);
        when(projectRepository.findByCompany(company)).thenReturn(Collections.singletonList(project));
        when(authClient.getProjectMembersByUserId(authId)).thenReturn(Collections.emptyList());

        // Act
        ProjectResponseWithRoleDTO result = projectService.getProjectsByUser(authId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Company", result.getCompanyName());
        assertEquals(1, result.getProjects().size());
        assertEquals("Manager", result.getProjects().get(0).getRoleInProject());
        verify(clientRepository, times(1)).findByAuthId(authId);
        verify(projectRepository, times(1)).findByCompany(company);
        verify(authClient, times(1)).getProjectMembersByUserId(authId);
    }
    @Test
    void testLinkGitHubRepositoryToProject_NewLink() {
        // Arrange
        String repoUrl = "https://github.com/owner/repo";
        when(authClient.decodeToken("Bearer token")).thenReturn(authId);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(gitHubLinkRepository.findByProjetId(1L)).thenReturn(null);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Collections.singletonMap("exists", true), HttpStatus.OK));
        when(gitHubLinkRepository.save(any(GitHubLink.class))).thenReturn(new GitHubLink());

        // Act
        projectService.linkGitHubRepositoryToProject(1L, repoUrl, "Bearer token");

        // Assert
        verify(authClient, times(1)).decodeToken("Bearer token");
        verify(projectRepository, times(1)).findById(1L);
        verify(gitHubLinkRepository, times(1)).findByProjetId(1L);
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(Map.class));
        verify(gitHubLinkRepository, times(1)).save(any(GitHubLink.class));
    }

    @Test
    void testLinkGitHubRepositoryToProject_UpdateLink() {
        // Arrange
        String repoUrl = "https://github.com/owner/repo";
        GitHubLink existingLink = new GitHubLink(repoUrl, project, authId);
        when(authClient.decodeToken("Bearer token")).thenReturn(authId);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(gitHubLinkRepository.findByProjetId(1L)).thenReturn(existingLink);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Collections.singletonMap("exists", true), HttpStatus.OK));
        when(gitHubLinkRepository.save(any(GitHubLink.class))).thenReturn(existingLink);

        // Act
        projectService.linkGitHubRepositoryToProject(1L, repoUrl, "Bearer token");

        // Assert
        verify(authClient, times(1)).decodeToken("Bearer token");
        verify(projectRepository, times(1)).findById(1L);
        verify(gitHubLinkRepository, times(1)).findByProjetId(1L);
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(Map.class));
        verify(gitHubLinkRepository, times(1)).save(existingLink);
    }

    @Test
    void testLinkGitHubRepositoryToProject_InvalidUrl() {
        // Arrange
        String invalidUrl = "https://invalid.com/owner/repo";
        when(authClient.decodeToken("Bearer token")).thenReturn(authId);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            projectService.linkGitHubRepositoryToProject(1L, invalidUrl, "Bearer token");
        });
        assertEquals("L'URL du dépôt GitHub est invalide", exception.getMessage());
        verify(authClient, times(1)).decodeToken("Bearer token");
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void testGetGitHubRepositoryUrl_Success() {
        // Arrange
        GitHubLink link = new GitHubLink("https://github.com/owner/repo", project, authId);
        when(gitHubLinkRepository.findByProjetId(1L)).thenReturn(link);

        // Act
        String result = projectService.getGitHubRepositoryUrl(1L);

        // Assert
        assertEquals("https://github.com/owner/repo", result);
        verify(gitHubLinkRepository, times(1)).findByProjetId(1L);
    }

    @Test
    void testGetGitHubRepositoryUrl_NotFound() {
        // Arrange
        when(gitHubLinkRepository.findByProjetId(1L)).thenReturn(null);

        // Act
        String result = projectService.getGitHubRepositoryUrl(1L);

        // Assert
        assertNull(result);
        verify(gitHubLinkRepository, times(1)).findByProjetId(1L);
    }

    @Test
    void testGetGitHubUserId_Success() {
        // Arrange
        GitHubLink link = new GitHubLink("https://github.com/owner/repo", project, authId);
        when(gitHubLinkRepository.findByProjectId(1L)).thenReturn(Optional.of(link));

        // Act
        Map<String, String> result = projectService.getGitHubUserId(1L);

        // Assert
        assertEquals(authId, result.get("userId"));
        verify(gitHubLinkRepository, times(1)).findByProjectId(1L);
    }

    @Test
    void testGetGitHubUserId_NotFound() {
        // Arrange
        when(gitHubLinkRepository.findByProjectId(1L)).thenReturn(Optional.empty());

        // Act
        Map<String, String> result = projectService.getGitHubUserId(1L);

        // Assert
        assertTrue(result.isEmpty());
        verify(gitHubLinkRepository, times(1)).findByProjectId(1L);
    }

    @Test
    void testGetActiveProjectIds_Success() {
        // Arrange
        when(projectRepository.findAll()).thenReturn(Collections.singletonList(project));

        // Act
        List<Long> result = projectService.getActiveProjectIds();

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));
        verify(projectRepository, times(1)).findAll();
    }
}