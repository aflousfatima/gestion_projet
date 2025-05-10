package com.task.taskservice.unit.Service;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.DTO.CommentDTO;
import com.task.taskservice.Entity.Comment;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Entity.WorkItem;
import com.task.taskservice.Entity.WorkItemHistory;
import com.task.taskservice.Mapper.TaskMapper;
import com.task.taskservice.Repository.CommentRepository;
import com.task.taskservice.Repository.TaskRepository;
import com.task.taskservice.Service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AuthClient authClient;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createComment_shouldThrowRuntimeException_dueToNullContentInLogHistory() {
        // Arrange
        Long workItemId = 1L;
        String token = "Bearer valid-token";
        String author = "user1";
        String content = "This is a test comment";

        Task task = new Task();
        task.setId(workItemId);
        task.setHistory(new HashSet<>());

        Comment comment = new Comment();
        comment.setWorkItem(task);
        comment.setContent(content);

        Comment savedComment = new Comment();
        savedComment.setId(1L);
        savedComment.setWorkItem(task);
        savedComment.setContent(content);
        savedComment.setAuthor(author);
        savedComment.setCreatedAt(LocalDateTime.now());

        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setId(1L);
        commentDTO.setWorkItemId(workItemId);
        commentDTO.setContent(content);
        commentDTO.setAuthor(author);
        commentDTO.setCreatedAt(savedComment.getCreatedAt());

        when(authClient.decodeToken(token)).thenReturn(author);
        when(taskRepository.findById(workItemId)).thenReturn(Optional.of(task));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(taskMapper.mapToDTO(savedComment)).thenReturn(commentDTO);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                commentService.createComment(comment, token), "Failed to create comment");
        assertEquals("Failed to create comment", exception.getMessage());
        assertTrue(exception.getCause() instanceof NullPointerException);

        // Verify interactions up to the exception
        verify(authClient).decodeToken(token);
        verify(taskRepository).findById(workItemId);
        verify(commentRepository).save(any(Comment.class));
        verifyNoMoreInteractions(commentRepository, taskMapper, messagingTemplate);
    }

    @Test
    void createComment_shouldThrowIllegalArgumentException_whenTaskNotFound() {
        // Arrange
        Long workItemId = 1L;
        String token = "Bearer valid-token";
        Comment comment = new Comment();
        Task task = new Task();
        task.setId(workItemId);
        comment.setWorkItem(task);

        when(taskRepository.findById(workItemId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                commentService.createComment(comment, token), "Task not found");
        assertEquals("Failed to create comment", exception.getMessage());

        // Verify interactions
        verify(taskRepository).findById(workItemId);
        verify(authClient).decodeToken(token);
        verifyNoInteractions(commentRepository, messagingTemplate, taskMapper);
    }

    @Test
    void createComment_shouldThrowIllegalArgumentException_whenContentIsEmpty() {
        // Arrange
        Long workItemId = 1L;
        String token = "Bearer valid-token";
        String author = "user1";

        Task task = new Task();
        task.setId(workItemId);

        Comment comment = new Comment();
        comment.setWorkItem(task);
        comment.setContent("");

        when(authClient.decodeToken(token)).thenReturn(author);
        when(taskRepository.findById(workItemId)).thenReturn(Optional.of(task));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                commentService.createComment(comment, token), "Comment content cannot be empty");
        assertEquals("Failed to create comment", exception.getMessage());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(taskRepository).findById(workItemId);
        verifyNoInteractions(commentRepository, messagingTemplate, taskMapper);
    }

    @Test
    void createComment_shouldThrowRuntimeException_whenExceptionOccurs() {
        // Arrange
        Long workItemId = 1L;
        String token = "Bearer valid-token";
        String author = "user1";
        String content = "This is a test comment";

        Task task = new Task();
        task.setId(workItemId);

        Comment comment = new Comment();
        comment.setWorkItem(task);
        comment.setContent(content);

        when(authClient.decodeToken(token)).thenReturn(author);
        when(taskRepository.findById(workItemId)).thenReturn(Optional.of(task));
        when(commentRepository.save(any(Comment.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                commentService.createComment(comment, token), "Failed to create comment");
        assertEquals("Failed to create comment", exception.getMessage());
        assertEquals("Database error", exception.getCause().getMessage());

        // Verify interactions
        verify(authClient).decodeToken(token);
        verify(taskRepository).findById(workItemId);
        verify(commentRepository).save(any(Comment.class));
        verifyNoInteractions(messagingTemplate, taskMapper);
    }

    @Test
    void getCommentsByWorkItemId_shouldReturnComments_whenCommentsExist() {
        // Arrange
        Long workItemId = 1L;

        Task task = new Task();
        task.setId(workItemId);

        Comment comment1 = new Comment();
        comment1.setId(1L);
        comment1.setWorkItem(task);
        comment1.setContent("Comment 1");
        comment1.setAuthor("user1");
        comment1.setCreatedAt(LocalDateTime.now().minusHours(1));

        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setWorkItem(task);
        comment2.setContent("Comment 2");
        comment2.setAuthor("user2");
        comment2.setCreatedAt(LocalDateTime.now());

        List<Comment> comments = Arrays.asList(comment1, comment2);

        CommentDTO commentDTO1 = new CommentDTO();
        commentDTO1.setId(1L);
        commentDTO1.setWorkItemId(workItemId);
        commentDTO1.setContent("Comment 1");
        commentDTO1.setAuthor("user1");
        commentDTO1.setCreatedAt(comment1.getCreatedAt());

        CommentDTO commentDTO2 = new CommentDTO();
        commentDTO2.setId(2L);
        commentDTO2.setWorkItemId(workItemId);
        commentDTO2.setContent("Comment 2");
        commentDTO2.setAuthor("user2");
        commentDTO2.setCreatedAt(comment2.getCreatedAt());

        when(commentRepository.findByWorkItemIdOrderByCreatedAtAsc(workItemId)).thenReturn(comments);
        when(taskMapper.mapToDTO(comment1)).thenReturn(commentDTO1);
        when(taskMapper.mapToDTO(comment2)).thenReturn(commentDTO2);

        // Act
        List<CommentDTO> result = commentService.getCommentsByWorkItemId(workItemId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(commentDTO1.getId(), result.get(0).getId());
        assertEquals(commentDTO1.getContent(), result.get(0).getContent());
        assertEquals(commentDTO2.getId(), result.get(1).getId());
        assertEquals(commentDTO2.getContent(), result.get(1).getContent());

        // Verify interactions
        verify(commentRepository).findByWorkItemIdOrderByCreatedAtAsc(workItemId);
        verify(taskMapper, times(2)).mapToDTO(any(Comment.class));
        verifyNoInteractions(taskRepository, authClient, messagingTemplate);
    }

    @Test
    void getCommentsByWorkItemId_shouldReturnEmptyList_whenNoCommentsExist() {
        // Arrange
        Long workItemId = 1L;

        when(commentRepository.findByWorkItemIdOrderByCreatedAtAsc(workItemId)).thenReturn(Collections.emptyList());

        // Act
        List<CommentDTO> result = commentService.getCommentsByWorkItemId(workItemId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify interactions
        verify(commentRepository).findByWorkItemIdOrderByCreatedAtAsc(workItemId);
        verifyNoInteractions(taskMapper, taskRepository, authClient, messagingTemplate);
    }
}