package com.task.taskservice.unit.Service;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.Configuration.ProjectClient;
import com.task.taskservice.DTO.*;
import com.task.taskservice.Entity.*;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Mapper.BugMapper;
import com.task.taskservice.Repository.BugRepository;
import com.task.taskservice.Repository.FileAttachmentRepository;
import com.task.taskservice.Repository.TagRepository;
import com.task.taskservice.Service.BugService;
import com.task.taskservice.Service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BugServiceTest {

    @Mock
    private BugRepository bugRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private BugMapper bugMapper;

    @Mock
    private AuthClient authClient;

    @Mock
    private ProjectClient projectClient;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;

    @InjectMocks
    private BugService bugService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createBug_shouldCreateBugWithTagsAndUsers_whenValidInput() {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 2L;
        String token = "Bearer valid-token";
        String createdBy = "user1";
        BugDTO bugDTO = new BugDTO();
        bugDTO.setTitle("Test Bug");
        bugDTO.setDescription("Test Description");
        bugDTO.setAssignedUserIds(Arrays.asList("user2", "user3"));
        bugDTO.setTags(new HashSet<>(Arrays.asList("tag1", "tag2")));

        Bug bug = new Bug();
        bug.setId(1L);
        bug.setTitle("Test Bug");
        bug.setDescription("Test Description");
        bug.setProjectId(projectId);
        bug.setUserStory(userStoryId);
        bug.setCreatedBy(createdBy);
        bug.setHistory(new HashSet<>());

        Tag tag1 = new Tag();
        tag1.setName("tag1");
        tag1.setWorkItems(new HashSet<>());
        Tag tag2 = new Tag();
        tag2.setName("tag2");
        tag2.setWorkItems(new HashSet<>());

        WorkItemHistory history = new WorkItemHistory();
        history.setAction("CREATION");
        history.setDescription("Bug created: Test Bug");
        history.setAuthorId(createdBy);
        history.setTimestamp(LocalDateTime.now());

        Bug savedBug = new Bug();
        savedBug.setId(1L);
        savedBug.setTitle("Test Bug");
        savedBug.setDescription("Test Description");
        savedBug.setProjectId(projectId);
        savedBug.setUserStory(userStoryId);
        savedBug.setCreatedBy(createdBy);
        savedBug.setAssignedUserIds(new HashSet<>(Arrays.asList("user2", "user3")));
        savedBug.setTags(new HashSet<>(Arrays.asList(tag1, tag2)));
        savedBug.setHistory(new HashSet<>(Collections.singletonList(history)));

        BugDTO savedBugDTO = new BugDTO();
        savedBugDTO.setId(1L);
        savedBugDTO.setTitle("Test Bug");
        savedBugDTO.setDescription("Test Description");
        savedBugDTO.setProjectId(projectId);
        savedBugDTO.setUserStoryId(userStoryId);
        savedBugDTO.setCreatedBy(createdBy);
        savedBugDTO.setAssignedUserIds(Arrays.asList("user2", "user3"));
        savedBugDTO.setTags(new HashSet<>(Arrays.asList("tag1", "tag2")));

        when(authClient.decodeToken(token)).thenReturn(createdBy);
        when(bugMapper.toEntity(bugDTO)).thenReturn(bug);
        when(tagRepository.findByName("tag1")).thenReturn(Optional.of(tag1));
        when(tagRepository.findByName("tag2")).thenReturn(Optional.of(tag2));
        when(bugRepository.save(any(Bug.class))).thenReturn(savedBug);
        when(bugMapper.toDTO(savedBug)).thenReturn(savedBugDTO);

        // Act
        BugDTO result = bugService.createBug(projectId, userStoryId, bugDTO, token);

        // Assert
        assertNotNull(result);
        assertEquals(savedBugDTO.getId(), result.getId());
        assertEquals(savedBugDTO.getTitle(), result.getTitle());
        assertEquals(savedBugDTO.getProjectId(), result.getProjectId());
        assertEquals(savedBugDTO.getUserStoryId(), result.getUserStoryId());
        assertEquals(savedBugDTO.getCreatedBy(), result.getCreatedBy());
        assertEquals(savedBugDTO.getAssignedUserIds(), result.getAssignedUserIds());
        assertEquals(savedBugDTO.getTags(), result.getTags());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugMapper).toEntity(bugDTO);
        verify(tagRepository).findByName("tag1");
        verify(tagRepository).findByName("tag2");
        verify(bugRepository).save(any(Bug.class));
        verify(bugMapper).toDTO(savedBug);
    }

    @Test
    void updateBug_shouldUpdateBugFieldsAndLogTime_whenValidInput() {
        // Arrange
        Long bugId = 1L;
        String token = "Bearer valid-token";
        String updatedBy = "user1";
        BugDTO bugDTO = new BugDTO();
        bugDTO.setTitle("Updated Title");
        bugDTO.setDescription("Updated Description");
        bugDTO.setStatus(WorkItemStatus.IN_PROGRESS);
        bugDTO.setAssignedUserIds(Arrays.asList("user2", "user3"));
        bugDTO.setTags(new HashSet<>(Arrays.asList("tag1")));

        Bug existingBug = new Bug();
        existingBug.setId(bugId);
        existingBug.setTitle("Old Title");
        existingBug.setDescription("Old Description");
        existingBug.setStatus(WorkItemStatus.TO_DO);
        existingBug.setAssignedUserIds(new HashSet<>(Arrays.asList("user4")));
        existingBug.setTags(new HashSet<>());
        existingBug.setHistory(new HashSet<>());
        existingBug.setTimeEntries(new ArrayList<>());

        WorkItemHistory history = new WorkItemHistory();
        history.setAction("MISE_A_JOUR");
        history.setDescription("Titre changé, Description modifiée, Status Changed");
        history.setAuthorId(updatedBy);
        history.setTimestamp(LocalDateTime.now());

        Bug updatedBug = new Bug();
        updatedBug.setId(bugId);
        updatedBug.setTitle("Updated Title");
        updatedBug.setDescription("Updated Description");
        updatedBug.setStatus(WorkItemStatus.IN_PROGRESS);
        updatedBug.setAssignedUserIds(new HashSet<>(Arrays.asList("user2", "user3")));
        updatedBug.setTags(new HashSet<>(Arrays.asList(new Tag())));
        updatedBug.setHistory(new HashSet<>(Collections.singletonList(history)));
        updatedBug.setTimeEntries(new ArrayList<>());
        updatedBug.setStartTime(LocalDateTime.now());

        BugDTO updatedBugDTO = new BugDTO();
        updatedBugDTO.setId(bugId);
        updatedBugDTO.setTitle("Updated Title");
        updatedBugDTO.setDescription("Updated Description");
        updatedBugDTO.setStatus(WorkItemStatus.IN_PROGRESS);
        updatedBugDTO.setAssignedUserIds(Arrays.asList("user2", "user3"));
        updatedBugDTO.setTags(new HashSet<>(Arrays.asList("tag1")));
        updatedBugDTO.setAssignedUsers(Arrays.asList(new UserDTO(), new UserDTO()));

        when(authClient.decodeToken(token)).thenReturn(updatedBy);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(existingBug));
        when(tagRepository.findByName("tag1")).thenReturn(Optional.of(new Tag()));
        when(bugRepository.save(any(Bug.class))).thenReturn(updatedBug);
        when(bugMapper.toDTO(updatedBug)).thenReturn(updatedBugDTO);
        when(authClient.getUsersByIds(anyString(), eq(Arrays.asList("user2", "user3"))))
                .thenReturn(Arrays.asList(new UserDTO(), new UserDTO()));

        // Act
        BugDTO result = bugService.updateBug(bugId, bugDTO, token);

        // Assert
        assertNotNull(result);
        assertEquals(updatedBugDTO.getId(), result.getId());
        assertEquals(updatedBugDTO.getTitle(), result.getTitle());
        assertEquals(updatedBugDTO.getDescription(), result.getDescription());
        assertEquals(updatedBugDTO.getStatus(), result.getStatus());
        assertEquals(updatedBugDTO.getAssignedUserIds(), result.getAssignedUserIds());
        assertEquals(updatedBugDTO.getTags(), result.getTags());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verify(tagRepository).findByName("tag1");
        verify(bugRepository).save(any(Bug.class));
        verify(bugMapper).toDTO(updatedBug);
        verify(authClient).getUsersByIds(anyString(), eq(Arrays.asList("user2", "user3")));

        // Verify history logging
        assertFalse(updatedBug.getHistory().isEmpty());
        WorkItemHistory actualHistory = updatedBug.getHistory().iterator().next();
        assertEquals("MISE_A_JOUR", actualHistory.getAction());
        assertTrue(actualHistory.getDescription().contains("Titre changé"));
        assertEquals(updatedBy, actualHistory.getAuthorId());
    }

    @Test
    void updateBug_shouldThrowNoSuchElementException_whenBugNotFound() {
        // Arrange
        Long bugId = 1L;
        String token = "Bearer valid-token";
        BugDTO bugDTO = new BugDTO();
        when(authClient.decodeToken(token)).thenReturn("user1");
        when(bugRepository.findById(bugId)).thenReturn(Optional.empty());

        // Act & Assert
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () ->
                bugService.updateBug(bugId, bugDTO, token));
        assertEquals("Bug not found with ID: " + bugId, exception.getMessage());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verifyNoMoreInteractions(bugRepository, bugMapper, tagRepository);
    }

    @Test
    void getBugById_shouldReturnBugDTO_whenValidInput() {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 2L;
        Long bugId = 3L;
        String token = "Bearer valid-token";
        String userId = "user1";

        Bug bug = new Bug();
        bug.setId(bugId);
        bug.setProjectId(projectId);
        bug.setUserStory(userStoryId);
        bug.setTitle("Test Bug");

        BugDTO bugDTO = new BugDTO();
        bugDTO.setId(bugId);
        bugDTO.setProjectId(projectId);
        bugDTO.setUserStoryId(userStoryId);
        bugDTO.setTitle("Test Bug");

        when(authClient.decodeToken(token)).thenReturn(userId);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));
        when(bugMapper.toDTO(bug)).thenReturn(bugDTO);

        // Act
        BugDTO result = bugService.getBugById(projectId, userStoryId, bugId, token);

        // Assert
        assertNotNull(result);
        assertEquals(bugDTO.getId(), result.getId());
        assertEquals(bugDTO.getProjectId(), result.getProjectId());
        assertEquals(bugDTO.getUserStoryId(), result.getUserStoryId());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verify(bugMapper).toDTO(bug);
    }

    @Test
    void getBugById_shouldThrowIllegalArgumentException_whenBugNotInProjectOrStory() {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 2L;
        Long bugId = 3L;
        String token = "Bearer valid-token";
        String userId = "user1";

        Bug bug = new Bug();
        bug.setId(bugId);
        bug.setProjectId(999L); // Different project
        bug.setUserStory(999L); // Different user story

        when(authClient.decodeToken(token)).thenReturn(userId);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bugService.getBugById(projectId, userStoryId, bugId, token));
        assertEquals("Bug does not belong to the specified project or user story", exception.getMessage());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verifyNoInteractions(bugMapper);
    }

    @Test
    void getBugByBugId_shouldReturnBugDTO_whenValidInput() {
        // Arrange
        Long bugId = 1L;
        String token = "Bearer valid-token";
        String userId = "user1";

        Bug bug = new Bug();
        bug.setId(bugId);
        bug.setTitle("Test Bug");

        BugDTO bugDTO = new BugDTO();
        bugDTO.setId(bugId);
        bugDTO.setTitle("Test Bug");

        when(authClient.decodeToken(token)).thenReturn(userId);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));
        when(bugMapper.toDTO(bug)).thenReturn(bugDTO);

        // Act
        BugDTO result = bugService.getBugByBugId(bugId, token);

        // Assert
        assertNotNull(result);
        assertEquals(bugDTO.getId(), result.getId());
        assertEquals(bugDTO.getTitle(), result.getTitle());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verify(bugMapper).toDTO(bug);
    }

    @Test
    void deleteBug_shouldDeleteBug_whenBugExists() {
        // Arrange
        Long bugId = 1L;
        Bug bug = new Bug();
        bug.setId(bugId);

        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));

        // Act
        bugService.deleteBug(bugId);

        // Assert
        verify(bugRepository).findById(bugId);
        verify(bugRepository).delete(bug);
    }

    @Test
    void getBugsByProjectAndUserStory_shouldReturnBugDTOs_whenValidInput() {
        // Arrange
        Long projectId = 1L;
        Long userStoryId = 2L;
        String token = "Bearer valid-token";
        String userId = "user1";

        Bug bug = new Bug();
        bug.setId(1L);
        bug.setProjectId(projectId);
        bug.setUserStory(userStoryId);

        BugDTO bugDTO = new BugDTO();
        bugDTO.setId(1L);
        bugDTO.setProjectId(projectId);
        bugDTO.setUserStoryId(userStoryId);

        when(authClient.decodeToken(token)).thenReturn(userId);
        when(bugRepository.findByProjectIdAndUserStory(projectId, userStoryId)).thenReturn(Arrays.asList(bug));
        when(bugMapper.toDTO(bug)).thenReturn(bugDTO);

        // Act
        List<BugDTO> result = bugService.getBugsByProjectAndUserStory(projectId, userStoryId, token);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(bugDTO.getId(), result.get(0).getId());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findByProjectIdAndUserStory(projectId, userStoryId);
        verify(bugMapper).toDTO(bug);
    }

    @Test
    void getBugsByProjectId_shouldReturnBugDTOs_whenValidInput() {
        // Arrange
        Long projectId = 1L;
        String token = "Bearer valid-token";
        String userId = "user1";

        Bug bug = new Bug();
        bug.setId(1L);
        bug.setProjectId(projectId);

        BugDTO bugDTO = new BugDTO();
        bugDTO.setId(1L);
        bugDTO.setProjectId(projectId);

        when(authClient.decodeToken(token)).thenReturn(userId);
        when(bugRepository.findByProjectId(projectId)).thenReturn(Arrays.asList(bug));
        when(bugMapper.toDTO(bug)).thenReturn(bugDTO);

        // Act
        List<BugDTO> result = bugService.getBugsByProjectId(projectId, token);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(bugDTO.getId(), result.get(0).getId());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findByProjectId(projectId);
        verify(bugMapper).toDTO(bug);
    }

    @Test
    void getBugsOfActiveSprint_shouldReturnBugDTOsWithUsers_whenValidInput() {
        // Arrange
        Long projectId = 1L;
        String token = "Bearer valid-token";
        Long userStoryId = 2L;

        Bug bug = new Bug();
        bug.setId(1L);
        bug.setUserStory(userStoryId);
        bug.setAssignedUserIds(new HashSet<>(Arrays.asList("user1", "user2")));

        BugDTO bugDTO = new BugDTO();
        bugDTO.setId(1L);
        bugDTO.setUserStoryId(userStoryId);
        bugDTO.setAssignedUserIds(Arrays.asList("user1", "user2"));

        UserDTO user1 = new UserDTO();
        user1.setId("user1");
        UserDTO user2 = new UserDTO();
        user2.setId("user2");

        when(projectClient.getUserStoriesOfActiveSprint(projectId)).thenReturn(Arrays.asList(userStoryId));
        when(bugRepository.findByUserStoryIn(Arrays.asList(userStoryId))).thenReturn(Arrays.asList(bug));
        when(bugMapper.toDTO(bug)).thenReturn(bugDTO);
        when(authClient.getUsersByIds(token, Arrays.asList("user1", "user2")))
                .thenReturn(Arrays.asList(user1, user2));

        // Act
        List<BugDTO> result = bugService.getBugsOfActiveSprint(projectId, token);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(bugDTO.getId(), result.get(0).getId());
        assertEquals(2, result.get(0).getAssignedUsers().size());

        // Verify interactions
        verify(projectClient).getUserStoriesOfActiveSprint(projectId);
        verify(bugRepository).findByUserStoryIn(Arrays.asList(userStoryId));
        verify(bugMapper).toDTO(bug);
        verify(authClient).getUsersByIds(token, Arrays.asList("user1", "user2"));
    }

    @Test
    void attachFileToBug_shouldAttachFile_whenValidInput() throws IOException {
        // Arrange
        Long bugId = 1L;
        String token = "Bearer valid-token";
        String uploadedBy = "user1";
        MultipartFile file = mock(MultipartFile.class);
        Bug bug = new Bug();
        bug.setId(bugId);
        bug.setAttachments(new ArrayList<>());

        Map<String, String> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "http://cloudinary.com/file");
        uploadResult.put("public_id", "file123");

        when(authClient.decodeToken(token)).thenReturn(uploadedBy);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        when(cloudinaryService.uploadFile(file)).thenReturn(uploadResult);
        when(bugRepository.saveAndFlush(bug)).thenReturn(bug);

        // Act
        Bug result = bugService.attachFileToBug(bugId, file, token);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getAttachments().size());
        FileAttachment attachment = result.getAttachments().get(0);
        assertEquals("test.pdf", attachment.getFileName());
        assertEquals("http://cloudinary.com/file", attachment.getFileUrl());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verify(cloudinaryService).uploadFile(file);
        verify(bugRepository).saveAndFlush(bug);
    }

    @Test
    void attachFileToBug_shouldThrowIllegalArgumentException_whenFileIsEmpty() throws IOException {
        // Arrange
        Long bugId = 1L;
        String token = "Bearer valid-token";
        String uploadedBy = "user1";
        MultipartFile file = mock(MultipartFile.class);
        Bug bug = new Bug();
        bug.setId(bugId);

        when(authClient.decodeToken(token)).thenReturn(uploadedBy);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));
        when(file.isEmpty()).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bugService.attachFileToBug(bugId, file, token));
        assertEquals("File cannot be empty", exception.getMessage());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verifyNoInteractions(cloudinaryService, fileAttachmentRepository);
    }

    @Test
    void deleteFileFromBug_shouldDeleteFile_whenValidInput() throws IOException {
        // Arrange
        String publicId = "file123";
        String token = "Bearer valid-token";
        FileAttachment attachment = new FileAttachment();
        attachment.setPublicId(publicId);
        Bug bug = new Bug();
        bug.setId(1L);
        bug.setAttachments(new ArrayList<>(Collections.singletonList(attachment)));

        when(fileAttachmentRepository.findByPublicId(publicId)).thenReturn(Optional.of(attachment));
        when(bugRepository.findByAttachmentsContaining(attachment)).thenReturn(Arrays.asList(bug));
        when(bugRepository.save(bug)).thenReturn(bug);

        // Act
        bugService.deleteFileFromBug(publicId, token);

        // Assert
        verify(cloudinaryService).deleteFile(publicId);
        verify(bugRepository).save(bug);
        verify(fileAttachmentRepository).delete(attachment);
    }

    @Test
    void getDashboardStats_shouldReturnStats_whenActiveSprintExists() {
        // Arrange
        Long projectId = 1L;
        String token = "Bearer valid-token";
        Long userStoryId = 2L;

        when(projectClient.getUserStoriesOfActiveSprint(projectId)).thenReturn(Arrays.asList(userStoryId));
        when(bugRepository.countByUserStoryInAndStatus(Arrays.asList(userStoryId), WorkItemStatus.DONE)).thenReturn(5L);
        when(bugRepository.countByUserStoryInAndStatusNot(Arrays.asList(userStoryId), WorkItemStatus.DONE)).thenReturn(3L);
        when(bugRepository.countOverdueByUserStoryIn(Arrays.asList(userStoryId), WorkItemStatus.DONE, LocalDate.now())).thenReturn(1L);
        when(bugRepository.countByUserStoryIn(Arrays.asList(userStoryId))).thenReturn(8L);
        when(bugRepository.countBugsByStatus(Arrays.asList(userStoryId)))
                .thenReturn(Arrays.asList(new Object[]{WorkItemStatus.DONE, 5L}, new Object[]{WorkItemStatus.IN_PROGRESS, 3L}));
        when(bugRepository.countBugsByPriority(Arrays.asList(userStoryId)))
                .thenReturn(Arrays.asList(new Object[]{WorkItemPriority.HIGH, 4L}, new Object[]{WorkItemPriority.MEDIUM, 4L}));

        // Act
        DashboardStatsDTO result = bugService.getDashboardStats(projectId, token);

        // Assert
        assertNotNull(result);

        // Verify interactions
        verify(projectClient).getUserStoriesOfActiveSprint(projectId);
        verify(bugRepository).countByUserStoryInAndStatus(Arrays.asList(userStoryId), WorkItemStatus.DONE);
        verify(bugRepository).countByUserStoryInAndStatusNot(Arrays.asList(userStoryId), WorkItemStatus.DONE);
        verify(bugRepository).countOverdueByUserStoryIn(Arrays.asList(userStoryId), WorkItemStatus.DONE, LocalDate.now());
        verify(bugRepository).countByUserStoryIn(Arrays.asList(userStoryId));
        verify(bugRepository).countBugsByStatus(Arrays.asList(userStoryId));
        verify(bugRepository).countBugsByPriority(Arrays.asList(userStoryId));
    }

    @Test
    void getBugsForCalendar_shouldReturnBugCalendarDTOs_whenActiveSprintExists() {
        // Arrange
        Long projectId = 1L;
        String token = "Bearer valid-token";
        Long userStoryId = 2L;

        Bug bug = new Bug();
        bug.setId(1L);
        bug.setTitle("Test Bug");
        bug.setStartDate(LocalDate.now());
        bug.setDueDate(LocalDate.now().plusDays(1));

        when(projectClient.getUserStoriesOfActiveSprint(projectId)).thenReturn(Arrays.asList(userStoryId));
        when(bugRepository.findByUserStoryIn(Arrays.asList(userStoryId))).thenReturn(Arrays.asList(bug));

        // Act
        List<BugCalendarDTO> result = bugService.getBugsForCalendar(projectId, token);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(bug.getId(), result.get(0).getId());
        assertEquals(bug.getTitle(), result.get(0).getTitle());
        assertEquals(bug.getStartDate(), result.get(0).getStartDate());
        assertEquals(bug.getDueDate(), result.get(0).getDueDate());

        // Verify interactions
        verify(projectClient).getUserStoriesOfActiveSprint(projectId);
        verify(bugRepository).findByUserStoryIn(Arrays.asList(userStoryId));
    }

    @Test
    void getTimeEntries_shouldReturnTimeEntryDTOs_whenValidInput() {
        // Arrange
        Long bugId = 1L;
        String token = "Bearer valid-token";
        String userId = "user1";

        Bug bug = new Bug();
        bug.setId(bugId);
        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setId(1L);
        timeEntry.setDuration(60L);
        timeEntry.setAddedBy(userId);
        timeEntry.setAddedAt(LocalDateTime.now());
        timeEntry.setType("travail");
        bug.setTimeEntries(new ArrayList<>(Collections.singletonList(timeEntry)));

        BugDTO bugDTO = new BugDTO();
        bugDTO.setId(bugId);

        when(authClient.decodeToken(token)).thenReturn(userId);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));
        when(bugMapper.toDTO(bug)).thenReturn(bugDTO);

        // Act
        List<TimeEntryDTO> result = bugService.getTimeEntries(bugId, token);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(timeEntry.getId(), result.get(0).getId());
        assertEquals(timeEntry.getDuration(), result.get(0).getDuration());
        assertEquals(timeEntry.getType(), result.get(0).getType());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository, times(2)).findById(bugId); // Called by getBugByBugId and getTimeEntries
        verify(bugMapper).toDTO(bug);
    }

    @Test
    void addManualTimeEntry_shouldAddTimeEntry_whenValidInput() {
        // Arrange
        Long bugId = 1L;
        Long duration = 60L;
        String type = "travail";
        String token = "Bearer valid-token";
        String addedBy = "user1";

        Bug bug = new Bug();
        bug.setId(bugId);
        bug.setTimeEntries(new ArrayList<>());
        bug.setTotalTimeSpent(0L);

        when(authClient.decodeToken(token)).thenReturn(addedBy);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));
        when(bugRepository.save(bug)).thenReturn(bug);

        // Act
        bugService.addManualTimeEntry(bugId, duration, type, token);

        // Assert
        assertEquals(1, bug.getTimeEntries().size());
        TimeEntry timeEntry = bug.getTimeEntries().get(0);
        assertEquals(duration, timeEntry.getDuration());
        assertEquals(type, timeEntry.getType());
        assertEquals(addedBy, timeEntry.getAddedBy());
        assertEquals(60L, bug.getTotalTimeSpent());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verify(bugRepository).save(bug);
    }

    @Test
    void addManualTimeEntry_shouldThrowIllegalArgumentException_whenInvalidDuration() {
        // Arrange
        Long bugId = 1L;
        Long duration = 0L;
        String type = "travail";
        String token = "Bearer valid-token";
        String addedBy = "user1";

        Bug bug = new Bug();
        bug.setId(bugId);

        when(authClient.decodeToken(token)).thenReturn(addedBy);
        when(bugRepository.findById(bugId)).thenReturn(Optional.of(bug));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bugService.addManualTimeEntry(bugId, duration, type, token));
        assertEquals("Duration must be positive", exception.getMessage());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(bugRepository).findById(bugId);
        verifyNoMoreInteractions(bugRepository);
    }
}