package com.task.taskservice.Service;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.DTO.CommentDTO;
import com.task.taskservice.Entity.Comment;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Mapper.TaskMapper;
import com.task.taskservice.Repository.CommentRepository;
import com.task.taskservice.Repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private AuthClient authClient;
    @Autowired
    private TaskMapper taskMapper;

    @Transactional
    public CommentDTO createComment(Comment comment, String token) {
        try {
            logger.info("Creating comment for workItemId: {}", comment.getWorkItem().getId());
            String author = authClient.decodeToken(token);
            Task task = taskRepository.findById(comment.getWorkItem().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));

            // Validate comment
            if (comment.getContent() == null || comment.getContent().isEmpty()) {
                logger.error("Comment content is null or empty");
                throw new IllegalArgumentException("Comment content cannot be empty");
            }

            comment.setWorkItem(task);
            comment.setAuthor(author);
            logger.debug("Saving comment: content={}, author={}", comment.getContent(), author);
            Comment savedComment = commentRepository.save(comment);
            logger.info("Comment saved with id: {}", savedComment.getId());

            // Explicitly flush to ensure the INSERT is executed
            commentRepository.flush();
            logger.info("Comment flushed to database");

            CommentDTO commentDTO = taskMapper.mapToDTO(savedComment);
            messagingTemplate.convertAndSend("/topic/tasks/" + comment.getWorkItem().getId(), commentDTO);
            logger.info("Broadcasted comment to /topic/tasks/{}", comment.getWorkItem().getId());

            return commentDTO;
        } catch (Exception e) {
            logger.error("Failed to create comment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create comment", e);
        }
    }

    public List<CommentDTO> getCommentsByWorkItemId(Long workItemId) {
        logger.info("Fetching comments for workItemId: {}", workItemId);
        List<Comment> comments = commentRepository.findByWorkItemIdOrderByCreatedAtAsc(workItemId);
        return comments.stream()
                .map(taskMapper::mapToDTO)
                .collect(Collectors.toList());
    }
}