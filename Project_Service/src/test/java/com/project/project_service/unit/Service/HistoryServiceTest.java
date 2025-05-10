package com.project.project_service.unit.Service;

import com.project.project_service.DTO.SprintHistoryDto;
import com.project.project_service.DTO.UserStoryHistoryDto;
import com.project.project_service.Entity.SprintHistory;
import com.project.project_service.Entity.UserStoryHistory;
import com.project.project_service.Repository.SprintHistoryRepository;
import com.project.project_service.Repository.UserStoryHistoryRepository;
import com.project.project_service.Service.HistoryService;
import com.project.project_service.config.AuthClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private UserStoryHistoryRepository userStoryHistoryRepo;

    @Mock
    private SprintHistoryRepository sprintHistoryRepo;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private HistoryService historyService;

    private UserStoryHistory userStoryHistory;
    private SprintHistory sprintHistory;
    private Long userStoryId;
    private Long sprintId;
    private String authorId;
    private String userToken;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        userStoryId = 1L;
        sprintId = 2L;
        authorId = "auth123";
        userToken = "testToken";
        now = LocalDateTime.now();

        userStoryHistory = new UserStoryHistory();
        userStoryHistory.setUserStoryId(userStoryId);
        userStoryHistory.setAction("CREATED");
        userStoryHistory.setAuthor(authorId);
        userStoryHistory.setDate(now);
        userStoryHistory.setDescription("User story created");

        sprintHistory = new SprintHistory();
        sprintHistory.setSprintId(sprintId);
        sprintHistory.setAction("STARTED");
        sprintHistory.setAuthor(authorId);
        sprintHistory.setDate(now);
        sprintHistory.setDescription("Sprint started");
    }

    @Test
    void testAddUserStoryHistory_Success() {
        // Arrange
        when(userStoryHistoryRepo.save(any(UserStoryHistory.class))).thenReturn(userStoryHistory);

        // Act
        historyService.addUserStoryHistory(userStoryId, "CREATED", authorId, "User story created");

        // Assert
        verify(userStoryHistoryRepo, times(1)).save(any(UserStoryHistory.class));
    }

    @Test
    void testAddSprintHistory_Success() {
        // Arrange
        when(sprintHistoryRepo.save(any(SprintHistory.class))).thenReturn(sprintHistory);

        // Act
        historyService.addSprintHistory(sprintId, "STARTED", authorId, "Sprint started");

        // Assert
        verify(sprintHistoryRepo, times(1)).save(any(SprintHistory.class));
    }

    @Test
    void testGetUserStoryHistory_Success() {
        // Arrange
        when(userStoryHistoryRepo.findByUserStoryIdOrderByDateDesc(userStoryId))
                .thenReturn(Collections.singletonList(userStoryHistory));

        // Act
        List<UserStoryHistory> result = historyService.getUserStoryHistory(userStoryId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CREATED", result.get(0).getAction());
        assertEquals(authorId, result.get(0).getAuthor());
        verify(userStoryHistoryRepo, times(1)).findByUserStoryIdOrderByDateDesc(userStoryId);
    }

    @Test
    void testGetSprintHistory_Success() {
        // Arrange
        when(sprintHistoryRepo.findBySprintIdOrderByDateDesc(sprintId))
                .thenReturn(Collections.singletonList(sprintHistory));

        // Act
        List<SprintHistory> result = historyService.getSprintHistory(sprintId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("STARTED", result.get(0).getAction());
        assertEquals(authorId, result.get(0).getAuthor());
        verify(sprintHistoryRepo, times(1)).findBySprintIdOrderByDateDesc(sprintId);
    }

    void testGetUserStoryHistoryWithAuthorNames_Success() {
        // Arrange
        when(userStoryHistoryRepo.findByUserStoryIdOrderByDateDesc(userStoryId))
                .thenReturn(Collections.singletonList(userStoryHistory));
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("firstName", "John");
        userDetails.put("lastName", "Doe");
        when(authClient.getUserDetailsByAuthId(eq(authorId), eq("Bearer " + userToken)))
                .thenReturn(userDetails);

        // Act
        List<UserStoryHistoryDto> result = historyService.getUserStoryHistoryWithAuthorNames(userStoryId, userToken);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        UserStoryHistoryDto dto = result.get(0);
        assertEquals("CREATED", dto.getAction());
        assertEquals("John Doe", dto.getAuthorFullName());
        assertEquals("User story created", dto.getDescription());
        verify(userStoryHistoryRepo, times(1)).findByUserStoryIdOrderByDateDesc(userStoryId);
        verify(authClient, times(1)).getUserDetailsByAuthId(authorId, "Bearer " + userToken);
    }


    @Test
    void testGetUserStoryHistoryWithAuthorNames_UnknownAuthor() {
        // Arrange
        ReflectionTestUtils.setField(historyService, "authClient", authClient); // Inject authClient manually
        when(userStoryHistoryRepo.findByUserStoryIdOrderByDateDesc(userStoryId))
                .thenReturn(Collections.singletonList(userStoryHistory));
        when(authClient.getUserDetailsByAuthId(authorId, "Bearer " + userToken))
                .thenReturn(new HashMap<>());

        // Act
        List<UserStoryHistoryDto> result = historyService.getUserStoryHistoryWithAuthorNames(userStoryId, userToken);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        UserStoryHistoryDto dto = result.get(0);
        assertEquals("Inconnu Inconnu", dto.getAuthorFullName());
        verify(userStoryHistoryRepo, times(1)).findByUserStoryIdOrderByDateDesc(userStoryId);
        verify(authClient, times(1)).getUserDetailsByAuthId(authorId, "Bearer " + userToken);
    }

    @Test
    void testGetSprintHistoryWithAuthorNames_Success() {
        // Arrange
        ReflectionTestUtils.setField(historyService, "authClient", authClient); // Inject authClient manually
        when(sprintHistoryRepo.findBySprintIdOrderByDateDesc(sprintId))
                .thenReturn(Collections.singletonList(sprintHistory));
        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("firstName", "Jane");
        userDetails.put("lastName", "Smith");
        when(authClient.getUserDetailsByAuthId(eq(authorId), eq("Bearer " + userToken)))
                .thenReturn(userDetails);

        // Act
        List<SprintHistoryDto> result = historyService.getSprintHistoryWithAuthorNames(sprintId, userToken);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        SprintHistoryDto dto = result.get(0);
        assertEquals("STARTED", dto.getAction());
        assertEquals("Jane Smith", dto.getAuthorFullName());
        assertEquals("Sprint started", dto.getDescription());
        verify(sprintHistoryRepo, times(1)).findBySprintIdOrderByDateDesc(sprintId);
        verify(authClient, times(1)).getUserDetailsByAuthId(authorId, "Bearer " + userToken);
    }

    @Test
    void testGetSprintHistoryWithAuthorNames_UnknownAuthor() {
        // Arrange
        ReflectionTestUtils.setField(historyService, "authClient", authClient); // Inject authClient manually
        when(sprintHistoryRepo.findBySprintIdOrderByDateDesc(sprintId))
                .thenReturn(Collections.singletonList(sprintHistory));
        when(authClient.getUserDetailsByAuthId(authorId, "Bearer " + userToken))
                .thenReturn(new HashMap<>());

        // Act
        List<SprintHistoryDto> result = historyService.getSprintHistoryWithAuthorNames(sprintId, userToken);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        SprintHistoryDto dto = result.get(0);
        assertEquals("Inconnu Inconnu", dto.getAuthorFullName());
        verify(sprintHistoryRepo, times(1)).findBySprintIdOrderByDateDesc(sprintId);
        verify(authClient, times(1)).getUserDetailsByAuthId(authorId, "Bearer " + userToken);
    }
}