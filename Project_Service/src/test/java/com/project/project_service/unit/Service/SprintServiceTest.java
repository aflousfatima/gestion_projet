package com.project.project_service.unit.Service;

import com.project.project_service.DTO.SprintDTO;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.Priority;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.UserStoryStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.Repository.UserStoryRepository;
import com.project.project_service.Service.HistoryService;
import com.project.project_service.Service.SprintService;
import com.project.project_service.config.AuthClient;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private AuthClient authClient;

    @Mock
    private UserStoryRepository userStoryRepository;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private SprintService sprintService;

    private Projet projet;
    private Sprint sprint;
    private UserStory userStory;
    private SprintDTO sprintDTO;
    private Long projectId;
    private Long sprintId;
    private String token;
    private String userId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        projectId = 1L;
        sprintId = 2L;
        token = "testToken";
        userId = "auth123";
        now = LocalDateTime.now();

        projet = new Projet();
        projet.setId(projectId);

        sprint = new Sprint();
        sprint.setId(sprintId);
        sprint.setProject(projet);
        sprint.setName("Test Sprint");
        sprint.setStartDate(LocalDate.now());
        sprint.setEndDate(LocalDate.now().plusDays(7));
        sprint.setGoal("Test Goal");
        sprint.setCapacity(100);
        sprint.setCreatedBy(userId);
        sprint.setCreatedAt(now);
        sprint.setStatus(SprintStatus.PLANNED);

        userStory = new UserStory();
        userStory.setId(3L);
        userStory.setStatus(UserStoryStatus.BACKLOG);
        userStory.setSprint(sprint);
        userStory.setPriority(Priority.MEDIUM); // Avoid NullPointerException on getPriority
        userStory.setEffortPoints(3); // Avoid NullPointerException on getEffortPoints
        userStory.setProject(projet); // Avoid NullPointerException on getProject
        sprint.setUserStories(Collections.singletonList(userStory));

        sprintDTO = new SprintDTO();
        sprintDTO.setName("Test Sprint");
        sprintDTO.setStartDate(LocalDate.now());
        sprintDTO.setEndDate(LocalDate.now().plusDays(7));
        sprintDTO.setGoal("Test Goal");
        sprintDTO.setCapacity(100);
        sprintDTO.setStatus("PLANNED");

        // Manually inject dependencies to avoid NullPointerException
        sprintService = new SprintService();
        ReflectionTestUtils.setField(sprintService, "sprintRepository", sprintRepository);
        ReflectionTestUtils.setField(sprintService, "projetRepository", projetRepository);
        ReflectionTestUtils.setField(sprintService, "authClient", authClient);
        ReflectionTestUtils.setField(sprintService, "userStoryRepository", userStoryRepository);
        ReflectionTestUtils.setField(sprintService, "historyService", historyService);
    }

    @Test
    void testCreateSprint_Success() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(sprintRepository.save(any(Sprint.class))).thenReturn(sprint);
        doNothing().when(historyService).addSprintHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        SprintDTO result = sprintService.createSprint(projectId, sprintDTO, token);

        // Assert
        assertNotNull(result);
        assertEquals("Test Sprint", result.getName());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, times(1)).save(any(Sprint.class));
        verify(historyService, times(1)).addSprintHistory(eq(sprintId), eq("CREATE"), eq(userId), anyString());
    }

    @Test
    void testCreateSprint_InvalidToken() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sprintService.createSprint(projectId, sprintDTO, token);
        });
        assertEquals("Token invalide ou utilisateur non identifié", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, never()).findById(any());
    }

    @Test
    void testGetSprintsByProjectId_Success() {
        // Arrange
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(sprintRepository.findByProject(projet)).thenReturn(Collections.singletonList(sprint));

        // Act
        List<SprintDTO> result = sprintService.getSprintsByProjectId(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Sprint", result.get(0).getName());
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, times(1)).findByProject(projet);
    }

    @Test
    void testGetSprintsByProjectId_ProjectNotFound() {
        // Arrange
        when(projetRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sprintService.getSprintsByProjectId(projectId);
        });
        assertEquals("Projet non trouvé avec l'ID: " + projectId, exception.getMessage());
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, never()).findByProject(any());
    }

    @Test
    void testUpdateSprint_Success() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(any(Sprint.class))).thenReturn(sprint);
        doNothing().when(historyService).addSprintHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        SprintDTO result = sprintService.updateSprint(projectId, sprintId, sprintDTO, token);

        // Assert
        assertNotNull(result);
        assertEquals("Test Sprint", result.getName());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, times(1)).save(any(Sprint.class));
        verify(historyService, times(1)).addSprintHistory(eq(sprintId), eq("UPDATE"), eq(userId), anyString());
    }

    @Test
    void testUpdateSprint_OptimisticLockException() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(any(Sprint.class))).thenThrow(new OptimisticLockException("Conflict"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sprintService.updateSprint(projectId, sprintId, sprintDTO, token);
        });
        assertEquals("Conflit de mise à jour détecté. Veuillez réessayer.", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, times(1)).save(any(Sprint.class));
        verify(historyService, never()).addSprintHistory(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void testDeleteSprint_Success() {
        // Arrange
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        doNothing().when(sprintRepository).delete(sprint);
        doNothing().when(historyService).addSprintHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        sprintService.deleteSprint(projectId, sprintId);

        // Assert
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, times(1)).delete(sprint);
        verify(historyService, times(1)).addSprintHistory(eq(sprintId), eq("DELETE"), eq(userId), anyString());
    }

    @Test
    void testDeleteSprint_SprintNotInProject() {
        // Arrange
        Projet differentProject = new Projet();
        differentProject.setId(999L);
        sprint.setProject(differentProject);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sprintService.deleteSprint(projectId, sprintId);
        });
        assertEquals("Le Sprint n'appartient pas à ce projet", exception.getMessage());
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, never()).delete(any());
    }

    @Test
    void testUpdateSprintStatus_Success_Completed() {
        // Arrange
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setEndDate(LocalDate.now().minusDays(1)); // End date in the past
        userStory.setStatus(UserStoryStatus.BACKLOG);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        doNothing().when(historyService).addSprintHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        SprintDTO result = sprintService.updateSprintStatus(projectId, sprintId, token);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(UserStoryStatus.BACKLOG, userStory.getStatus());
        assertNull(userStory.getSprint());
        verify(authClient, times(1)).decodeToken(token);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(userStoryRepository, times(1)).save(userStory);
        verify(sprintRepository, times(1)).save(sprint);
        verify(historyService, times(1)).addSprintHistory(eq(sprintId), eq("UPDATE_STATUS"), eq(userId), anyString());
    }

    @Test
    void testUpdateSprintStatus_NoChange() {
        // Arrange
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setEndDate(LocalDate.now().plusDays(1)); // End date in the future
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        // Act
        SprintDTO result = sprintService.updateSprintStatus(projectId, sprintId, token);

        // Assert
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(userStoryRepository, never()).save(any());
        verify(sprintRepository, never()).save(any());
        verify(historyService, never()).addSprintHistory(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void testCancelSprint_Success() {
        // Arrange
        sprint.setStatus(SprintStatus.ACTIVE);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        doNothing().when(historyService).addSprintHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        SprintDTO result = sprintService.cancelSprint(projectId, sprintId, token);

        // Assert
        assertNotNull(result);
        assertEquals("CANCELED", result.getStatus());
        assertEquals(UserStoryStatus.BACKLOG, userStory.getStatus());
        assertNull(userStory.getSprint());
        verify(authClient, times(1)).decodeToken(token);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(userStoryRepository, times(1)).save(userStory);
        verify(sprintRepository, times(1)).save(sprint);
        verify(historyService, times(1)).addSprintHistory(eq(sprintId), eq("CANCEL"), eq(userId), anyString());
    }

    @Test
    void testCancelSprint_AlreadyCompleted() {
        // Arrange
        sprint.setStatus(SprintStatus.COMPLETED);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sprintService.cancelSprint(projectId, sprintId, token);
        });
        assertEquals("Impossible d'annuler un sprint déjà terminé ou archivé", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(userStoryRepository, never()).save(any());
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void testArchiveSprint_Success() {
        // Arrange
        sprint.setStatus(SprintStatus.COMPLETED);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        doNothing().when(historyService).addSprintHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        SprintDTO result = sprintService.archiveSprint(projectId, sprintId, token);

        // Assert
        assertNotNull(result);
        assertEquals("ARCHIVED", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, times(1)).save(sprint);
        verify(historyService, times(1)).addSprintHistory(eq(sprintId), eq("ARCHIVE"), eq(userId), anyString());
    }

    @Test
    void testArchiveSprint_NotCompletedOrCanceled() {
        // Arrange
        sprint.setStatus(SprintStatus.ACTIVE);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sprintService.archiveSprint(projectId, sprintId, token);
        });
        assertEquals("Seuls les sprints terminés ou annulés peuvent être archivés", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void testActivateSprint_Success() {
        // Arrange
        sprint.setStatus(SprintStatus.PLANNED);
        userStory.setStatus(UserStoryStatus.BACKLOG);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        doNothing().when(historyService).addSprintHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        SprintDTO result = sprintService.activateSprint(projectId, sprintId, token);

        // Assert
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(userStoryRepository, times(1)).save(userStory);
        verify(sprintRepository, times(1)).save(sprint);
        verify(historyService, times(1)).addSprintHistory(eq(sprintId), eq("ACTIVATE"), eq(userId), anyString());
    }

    @Test
    void testActivateSprint_NotPlanned() {
        // Arrange
        sprint.setStatus(SprintStatus.ACTIVE);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sprintService.activateSprint(projectId, sprintId, token);
        });
        assertEquals("Seul un sprint planifié (PLANNED) peut être activé", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void testUpdateStatusBasedOnDependencies_NoDependencies() {
        // Arrange
        userStory.setDependsOn(Collections.emptyList());
        userStory.setStatus(UserStoryStatus.BACKLOG);
        userStory.setSprint(sprint);
        sprint.setStatus(SprintStatus.ACTIVE);

        // Act
        sprintService.updateStatusBasedOnDependencies(userStory);

        // Assert
        assertEquals(UserStoryStatus.IN_PROGRESS, userStory.getStatus());
        verify(userStoryRepository, never()).findById(anyLong());
    }

    @Test
    void testUpdateStatusBasedOnDependencies_AllDependenciesDone() {
        // Arrange
        UserStory dependency = new UserStory();
        dependency.setId(4L);
        dependency.setStatus(UserStoryStatus.DONE);
        dependency.setPriority(Priority.MEDIUM);
        dependency.setEffortPoints(3);
        dependency.setProject(projet); // Ensure project is set for dependency
        userStory.setDependsOn(Collections.singletonList(4L));
        userStory.setSprint(sprint);
        sprint.setStatus(SprintStatus.ACTIVE);
        when(userStoryRepository.findById(4L)).thenReturn(Optional.of(dependency));

        // Act
        sprintService.updateStatusBasedOnDependencies(userStory);

        // Assert
        assertEquals(UserStoryStatus.IN_PROGRESS, userStory.getStatus());
        verify(userStoryRepository, times(1)).findById(4L);
    }

    @Test
    void testUpdateStatusBasedOnDependencies_Blocked() {
        // Arrange
        UserStory dependency = new UserStory();
        dependency.setId(4L);
        dependency.setStatus(UserStoryStatus.IN_PROGRESS);
        dependency.setSprint(sprint);
        dependency.setPriority(Priority.MEDIUM);
        dependency.setEffortPoints(3);
        dependency.setProject(projet); // Ensure project is set for dependency
        userStory.setDependsOn(Collections.singletonList(4L));
        userStory.setSprint(sprint);
        sprint.setStatus(SprintStatus.ACTIVE);
        when(userStoryRepository.findById(4L)).thenReturn(Optional.of(dependency));

        // Act
        sprintService.updateStatusBasedOnDependencies(userStory);

        // Assert
        assertEquals(UserStoryStatus.BLOCKED, userStory.getStatus());
        verify(userStoryRepository, times(1)).findById(4L);
    }

    @Test
    void testCheckAndUpdateSprintStatus_AllUserStoriesDone() {
        // Arrange
        userStory.setStatus(UserStoryStatus.DONE);
        sprint.setStatus(SprintStatus.ACTIVE);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(sprintRepository.save(sprint)).thenReturn(sprint);
        doNothing().when(historyService).addSprintHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        SprintDTO result = sprintService.checkAndUpdateSprintStatus(projectId, sprintId, token);

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, times(1)).save(sprint);
        verify(historyService, times(1)).addSprintHistory(eq(sprintId), eq("UPDATE_STATUS"), eq(userId), anyString());
    }

    @Test
    void testCheckAndUpdateSprintStatus_NotAllUserStoriesDone() {
        // Arrange
        userStory.setStatus(UserStoryStatus.IN_PROGRESS);
        sprint.setStatus(SprintStatus.ACTIVE);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        // Act
        SprintDTO result = sprintService.checkAndUpdateSprintStatus(projectId, sprintId, token);

        // Assert
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(sprintRepository, never()).save(any());
        verify(historyService, never()).addSprintHistory(anyLong(), anyString(), anyString(), anyString());
    }
}