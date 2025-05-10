package com.project.project_service.unit.Service;

import com.project.project_service.DTO.TaskDTO;
import com.project.project_service.DTO.UserStoryDTO;
import com.project.project_service.DTO.UserStoryRequest;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.Tag;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.Priority;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.UserStoryStatus;
import com.project.project_service.Enumeration.WorkItemStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.Repository.TagRepository;
import com.project.project_service.Repository.UserStoryRepository;
import com.project.project_service.Service.HistoryService;
import com.project.project_service.Service.SprintService;
import com.project.project_service.Service.UserStoryService;
import com.project.project_service.config.AuthClient;
import com.project.project_service.config.TaskClient;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStoryServiceTest {

    @Mock
    private UserStoryRepository userStoryRepository;

    @Mock
    private ProjetRepository projetRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private AuthClient authClient;

    @Mock
    private TaskClient taskClient;

    @Mock
    private HistoryService historyService;

    @Mock
    private SprintService sprintService;

    @InjectMocks
    private UserStoryService userStoryService;

    private Projet projet;
    private Sprint sprint;
    private UserStory userStory;
    private UserStoryRequest userStoryRequest;
    private Tag tag;
    private TaskDTO taskDTO;
    private Long projectId;
    private Long sprintId;
    private Long userStoryId;
    private String token;
    private String userId;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        projectId = 1L;
        sprintId = 2L;
        userStoryId = 3L;
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
        sprint.setUserStories(Collections.emptyList());

        userStory = new UserStory();
        userStory.setId(userStoryId);
        userStory.setProject(projet);
        userStory.setTitle("Test User Story");
        userStory.setDescription("Test Description");
        userStory.setPriority(Priority.MEDIUM);
        userStory.setEffortPoints(3);
        userStory.setCreatedBy(userId);
        userStory.setStatus(UserStoryStatus.BACKLOG);
        userStory.setDependsOn(Collections.emptyList());
        userStory.setTags(Collections.emptyList());

        userStoryRequest = new UserStoryRequest(
                "Test User Story",
                "Test Description",
                "MEDIUM",
                "IN_PROGRESS",
                10,
                Collections.emptyList(),
                Collections.singletonList("Feature")
        );

        tag = new Tag("Feature");
        tag.setId(1L);

        taskDTO = new TaskDTO();
        taskDTO.setId(4L);
        taskDTO.setStatus(WorkItemStatus.DONE);

        // Manually inject dependencies
        userStoryService = new UserStoryService();
        ReflectionTestUtils.setField(userStoryService, "userStoryRepository", userStoryRepository);
        ReflectionTestUtils.setField(userStoryService, "projetRepository", projetRepository);
        ReflectionTestUtils.setField(userStoryService, "sprintRepository", sprintRepository);
        ReflectionTestUtils.setField(userStoryService, "tagRepository", tagRepository);
        ReflectionTestUtils.setField(userStoryService, "authClient", authClient);
        ReflectionTestUtils.setField(userStoryService, "taskClient", taskClient);
        ReflectionTestUtils.setField(userStoryService, "historyService", historyService);
        ReflectionTestUtils.setField(userStoryService, "sprintService", sprintService);
    }

    @Test
    void testCreateUserStory_Success() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(tagRepository.findByName("Feature")).thenReturn(Optional.of(tag));
        when(userStoryRepository.save(any(UserStory.class))).thenReturn(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        UserStoryDTO result = userStoryService.createUserStory(projectId, userStoryRequest, token);

        // Assert
        assertNotNull(result);
        assertEquals("Test User Story", result.getTitle());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(tagRepository, times(1)).findByName("Feature");
        verify(userStoryRepository, times(1)).save(any(UserStory.class));
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("CREATE"), eq(userId), anyString());
    }

    @Test
    void testCreateUserStory_InvalidToken() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userStoryService.createUserStory(projectId, userStoryRequest, token);
        });
        assertEquals("Token invalide ou utilisateur non identifié", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, never()).findById(any());
    }

    @Test
    void testCreateUserStory_ProjectNotFound() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.createUserStory(projectId, userStoryRequest, token);
        });
        assertEquals("Projet non trouvé avec l'ID: " + projectId, exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, never()).save(any());
    }

    @Test
    void testUpdateUserStory_Success() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(tagRepository.findByName("Feature")).thenReturn(Optional.of(tag));
        when(userStoryRepository.save(any(UserStory.class))).thenReturn(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        UserStoryDTO result = userStoryService.updateUserStory(projectId, userStoryId, userStoryRequest, token);

        // Assert
        assertNotNull(result);
        assertEquals("Test User Story", result.getTitle());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(tagRepository, times(1)).findByName("Feature");
        verify(userStoryRepository, times(1)).save(any(UserStory.class));
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("UPDATE"), eq(userId), anyString());
    }

    @Test
    void testUpdateUserStory_OptimisticLockException() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(tagRepository.findByName("Feature")).thenReturn(Optional.of(tag));
        when(userStoryRepository.save(any(UserStory.class))).thenThrow(new OptimisticLockException("Conflict"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.updateUserStory(projectId, userStoryId, userStoryRequest, token);
        });
        assertEquals("Conflit de mise à jour détecté. Veuillez réessayer.", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(userStoryRepository, times(1)).save(any(UserStory.class));
        verify(historyService, never()).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void testUpdateUserStory_UserStoryNotFound() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.updateUserStory(projectId, userStoryId, userStoryRequest, token);
        });
        assertEquals("User Story non trouvée avec l'ID: " + userStoryId, exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(userStoryRepository, never()).save(any());
    }

    @Test
    void testGetUserStoriesByProjectId_Success() {
        // Arrange
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findByProject(projet)).thenReturn(Collections.singletonList(userStory));

        // Act
        List<UserStoryDTO> result = userStoryService.getUserStoriesByProjectId(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test User Story", result.get(0).getTitle());
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findByProject(projet);
    }

    @Test
    void testGetUserStoriesByProjectId_ProjectNotFound() {
        // Arrange
        when(projetRepository.findById(projectId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.getUserStoriesByProjectId(projectId);
        });
        assertEquals("Projet non trouvé avec l'ID: " + projectId, exception.getMessage());
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, never()).findByProject(any());
    }

    @Test
    void testDeleteUserStory_Success() {
        // Arrange
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        doNothing().when(userStoryRepository).delete(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        userStoryService.deleteUserStory(projectId, userStoryId);

        // Assert
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(userStoryRepository, times(1)).delete(userStory);
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("DELETE"), eq(userId), anyString());
    }

    @Test
    void testDeleteUserStory_UserStoryNotInProject() {
        // Arrange
        Projet differentProject = new Projet();
        differentProject.setId(999L);
        userStory.setProject(differentProject);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.deleteUserStory(projectId, userStoryId);
        });
        assertEquals("La User Story n'appartient pas à ce projet", exception.getMessage());
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(userStoryRepository, never()).delete(any());
    }

    @Test
    void testAssignUserStoryToSprint_Success() {
        // Arrange
        userStory.setSprint(null);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        UserStoryDTO result = userStoryService.assignUserStoryToSprint(projectId, userStoryId, sprintId, token);

        // Assert
        assertNotNull(result);
        assertEquals(sprintId, result.getSprintId());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(userStoryRepository, times(1)).save(userStory);
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("ASSIGN_TO_SPRINT"), eq(userId), anyString());
    }

    @Test
    void testAssignUserStoryToSprint_CapacityExceeded() {
        // Arrange
        userStory.setEffortPoints(101); // Exceeds sprint capacity of 100
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(sprintRepository.findById(sprintId)).thenReturn(Optional.of(sprint));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.assignUserStoryToSprint(projectId, userStoryId, sprintId, token);
        });
        assertEquals("La capacité du sprint est insuffisante pour cette User Story", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(sprintRepository, times(1)).findById(sprintId);
        verify(userStoryRepository, never()).save(any());
    }

    @Test
    void testRemoveUserStoryFromSprint_Success() {
        // Arrange
        userStory.setSprint(sprint);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        UserStoryDTO result = userStoryService.removeUserStoryFromSprint(projectId, userStoryId, token);

        // Assert
        assertNotNull(result);
        assertNull(result.getSprintId());
        assertEquals("BACKLOG", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(userStoryRepository, times(1)).save(userStory);
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("DELETE"), eq(userId), anyString());
    }

    @Test
    void testRemoveUserStoryFromSprint_UserStoryNotFound() {
        // Arrange
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(projetRepository.findById(projectId)).thenReturn(Optional.of(projet));
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.removeUserStoryFromSprint(projectId, userStoryId, token);
        });
        assertEquals("User Story non trouvée avec l'ID: " + userStoryId, exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(projetRepository, times(1)).findById(projectId);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(userStoryRepository, never()).save(any());
    }


    @Test
    void testUpdateUserStoryStatus_Success() {
        // Arrange
        userStory.setSprint(sprint);
        sprint.setStatus(SprintStatus.ACTIVE);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        UserStoryDTO result = userStoryService.updateUserStoryStatus(projectId, userStoryId, "IN_PROGRESS", token);

        // Assert
        assertNotNull(result);
        assertEquals("IN_PROGRESS", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(userStoryRepository, times(1)).save(userStory);
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("UPDATE_USER_STORY_STATUS"), eq(userId), anyString());
    }

    @Test
    void testUpdateUserStoryStatus_BlockedWithoutDependencies() {
        // Arrange
        userStory.setDependsOn(Collections.emptyList());
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.updateUserStoryStatus(projectId, userStoryId, "BLOCKED", token);
        });
        assertEquals("Une US ne peut être BLOCKED sans dépendances non terminées", exception.getMessage());
        verify(authClient, times(1)).decodeToken(token);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(userStoryRepository, never()).save(any());
    }

    @Test
    void testUpdateTags_Success() {
        // Arrange
        List<String> tagNames = Collections.singletonList("Feature");
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(tagRepository.findByName("Feature")).thenReturn(Optional.of(tag));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());

        // Act
        UserStoryDTO result = userStoryService.updateTags(projectId, userStoryId, tagNames, token);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTags().size());
        assertEquals("Feature", result.getTags().get(0));
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(tagRepository, times(1)).findByName("Feature");
        verify(userStoryRepository, times(1)).save(userStory);
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("UPDATE_TAG"), eq(userId), anyString());
    }

    @Test
    void testUpdateTags_UserStoryNotFound() {
        // Arrange
        List<String> tagNames = Collections.singletonList("Feature");
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userStoryService.updateTags(projectId, userStoryId, tagNames, token);
        });
        assertEquals("User story not found", exception.getMessage());
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(tagRepository, never()).findByName(anyString());
        verify(userStoryRepository, never()).save(any());
    }

    @Test
    void testGetUserStoryIdsOfActiveSprint_Success() {
        // Arrange
        sprint.setStatus(SprintStatus.ACTIVE);
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(sprint);
        when(userStoryRepository.findBySprintId(sprintId)).thenReturn(Collections.singletonList(userStory));

        // Act
        List<Long> result = userStoryService.getUserStoryIdsOfActiveSprint(projectId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userStoryId, result.get(0));
        verify(sprintRepository, times(1)).findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
        verify(userStoryRepository, times(1)).findBySprintId(sprintId);
    }

    @Test
    void testGetUserStoryIdsOfActiveSprint_NoActiveSprint() {
        // Arrange
        when(sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)).thenReturn(null);

        // Act
        List<Long> result = userStoryService.getUserStoryIdsOfActiveSprint(projectId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(sprintRepository, times(1)).findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
        verify(userStoryRepository, never()).findBySprintId(anyLong());
    }

    @Test
    void testCheckAndUpdateUserStoryStatus_AllTasksDone() {
        // Arrange
        userStory.setStatus(UserStoryStatus.IN_PROGRESS);
        userStory.setSprint(sprint);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(taskClient.getTasksByProjectAndUserStory(projectId, userStoryId, token)).thenReturn(Collections.singletonList(taskDTO));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());
        when(sprintService.checkAndUpdateSprintStatus(projectId, sprintId, token)).thenReturn(new com.project.project_service.DTO.SprintDTO());

        // Act
        UserStoryDTO result = userStoryService.checkAndUpdateUserStoryStatus(projectId, userStoryId, token);

        // Assert
        assertNotNull(result);
        assertEquals("DONE", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(taskClient, times(1)).getTasksByProjectAndUserStory(projectId, userStoryId, token);
        verify(userStoryRepository, times(1)).save(userStory);
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("UPDATE_STATUS"), eq(userId), anyString());
        verify(sprintService, times(1)).checkAndUpdateSprintStatus(projectId, sprintId, token);
    }

    @Test
    void testCheckAndUpdateUserStoryStatus_NoTasksDone() {
        // Arrange
        taskDTO.setStatus(WorkItemStatus.IN_PROGRESS);
        when(authClient.decodeToken(token)).thenReturn(userId);
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(taskClient.getTasksByProjectAndUserStory(projectId, userStoryId, token)).thenReturn(Collections.singletonList(taskDTO));

        // Act
        UserStoryDTO result = userStoryService.checkAndUpdateUserStoryStatus(projectId, userStoryId, token);

        // Assert
        assertNotNull(result);
        assertEquals("BACKLOG", result.getStatus());
        verify(authClient, times(1)).decodeToken(token);
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(taskClient, times(1)).getTasksByProjectAndUserStory(projectId, userStoryId, token);
        verify(userStoryRepository, never()).save(any());
        verify(historyService, never()).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());
        verify(sprintService, never()).checkAndUpdateSprintStatus(anyLong(), anyLong(), anyString());
    }

    @Test
    void testCheckAndUpdateUserStoryStatus_SystemTask() {
        // Arrange
        userStory.setStatus(UserStoryStatus.IN_PROGRESS);
        userStory.setSprint(sprint);
        when(userStoryRepository.findById(userStoryId)).thenReturn(Optional.of(userStory));
        when(taskClient.getTasksByProjectAndUserStoryInternal(projectId, userStoryId)).thenReturn(Collections.singletonList(taskDTO));
        when(userStoryRepository.save(userStory)).thenReturn(userStory);
        doNothing().when(historyService).addUserStoryHistory(anyLong(), anyString(), anyString(), anyString());
        when(sprintService.checkAndUpdateSprintStatus(projectId, sprintId, null)).thenReturn(new com.project.project_service.DTO.SprintDTO());

        // Act
        UserStoryDTO result = userStoryService.checkAndUpdateUserStoryStatus(projectId, userStoryId, null);

        // Assert
        assertNotNull(result);
        assertEquals("DONE", result.getStatus());
        verify(authClient, never()).decodeToken(anyString());
        verify(userStoryRepository, times(1)).findById(userStoryId);
        verify(taskClient, times(1)).getTasksByProjectAndUserStoryInternal(projectId, userStoryId);
        verify(userStoryRepository, times(1)).save(userStory);
        verify(historyService, times(1)).addUserStoryHistory(eq(userStoryId), eq("UPDATE_STATUS"), eq("system-task"), anyString());
        verify(sprintService, times(1)).checkAndUpdateSprintStatus(projectId, sprintId, null);
    }
}