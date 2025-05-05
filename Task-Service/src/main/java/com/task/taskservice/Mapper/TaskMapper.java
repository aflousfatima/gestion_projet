package com.task.taskservice.Mapper; // Uppercase Mapper

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.DTO.*;
import com.task.taskservice.Entity.Comment;
import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Entity.Tag;
import com.task.taskservice.Service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class TaskMapper {
    private static final Logger logger = LoggerFactory.getLogger(TaskMapper.class);

    @Autowired
    private CloudinaryService cloudinaryService;
    public TaskDTO toDTO(Task task) {
        if (task == null) {
            return null;
        }

        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(task.getId());
        taskDTO.setTitle(task.getTitle());
        taskDTO.setDescription(task.getDescription());
        taskDTO.setCreationDate(task.getCreationDate());
        taskDTO.setStartDate(task.getStartDate());
        taskDTO.setDueDate(task.getDueDate());
        taskDTO.setCompletedDate(task.getCompletedDate());
        taskDTO.setEstimationTime(task.getEstimationTime());
        taskDTO.setTotalTimeSpent(task.getTotalTimeSpent());
        taskDTO.setStartTime(task.getStartTime()); // Ajouter startTime
        taskDTO.setStatus(task.getStatus());
        taskDTO.setPriority(task.getPriority());
        taskDTO.setProgress(task.getProgress());
        taskDTO.setUserStoryId(task.getUserStory());
        taskDTO.setProjectId(task.getProjectId());
        taskDTO.setCreatedBy(task.getCreatedBy());


        logger.info("Mapping Task {}: status={} (name={}) to TaskDTO status={} (name={})",
                task.getId(), task.getStatus(),
                task.getStatus() != null ? task.getStatus().name() : "null",
                taskDTO.getStatus(),
                taskDTO.getStatus() != null ? taskDTO.getStatus().name() : "null");
        // Mapper les dépendances
        if (task.getDependencies() != null) {
            taskDTO.setDependencyIds(task.getDependencies().stream()
                    .map(Task::getId)
                    .collect(Collectors.toList()));
            taskDTO.setDependencies(task.getDependencies().stream()
                    .map(dep -> new TaskSummaryDTO(
                            dep.getId(),
                            dep.getTitle(),
                            dep.getStatus(),
                            dep.getProjectId(),
                            dep.getUserStory()
                    ))
                    .collect(Collectors.toList()));
        } else {
            taskDTO.setDependencyIds(Collections.emptyList());
            taskDTO.setDependencies(Collections.emptyList());
        }

        if (task.getAssignedUserIds() != null) {
            taskDTO.setAssignedUserIds(task.getAssignedUserIds().stream()
                    .collect(Collectors.toList())); // Set<String> to List<String>
        } else {
            taskDTO.setAssignedUserIds(Collections.emptyList());
        }


        if (task.getTags() != null) {
            // Mapper les IDs des tags
            taskDTO.setTagIds(task.getTags().stream()
                    .map(Tag::getId)
                    .collect(Collectors.toSet()));
            // Mapper les noms des tags
            taskDTO.setTags(task.getTags().stream()
                    .map(Tag::getName)
                    .collect(Collectors.toSet()));
        }

        // Map attachments
        if (task.getAttachments() != null) {
            taskDTO.setAttachments(task.getAttachments().stream()
                    .map(this::toFileAttachmentDTO)
                    .collect(Collectors.toList()));
        }

        return taskDTO;
    }


    public Task toEntity(TaskDTO taskDTO) {
        if (taskDTO == null) {
            return null;
        }

        Task task = new Task();
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setCreationDate(taskDTO.getCreationDate());
        task.setStartDate(taskDTO.getStartDate());
        task.setDueDate(taskDTO.getDueDate());
        task.setCompletedDate(taskDTO.getCompletedDate());
        task.setEstimationTime(taskDTO.getEstimationTime());
        task.setTotalTimeSpent(taskDTO.getTotalTimeSpent());
        task.setStatus(taskDTO.getStatus());
        task.setPriority(taskDTO.getPriority());
        task.setProgress(taskDTO.getProgress());
        task.setStartTime(taskDTO.getStartTime()); // Ajouter startTime
        task.setUserStory(taskDTO.getUserStoryId());
        task.setProjectId(taskDTO.getProjectId());
        task.setCreatedBy(taskDTO.getCreatedBy());
        if (taskDTO.getAssignedUserIds() != null) {
            task.setAssignedUserIds(new HashSet<>(taskDTO.getAssignedUserIds()));
        }
        logger.info("Mapping TaskDTO {}: status={} (name={}) to Task status={} (name={})",
                taskDTO.getId(), taskDTO.getStatus(),
                taskDTO.getStatus() != null ? taskDTO.getStatus().name() : "null",
                task.getStatus(),
                task.getStatus() != null ? task.getStatus().name() : "null");
        return task;
    }


    private FileAttachmentDTO toFileAttachmentDTO(FileAttachment attachment) {
        FileAttachmentDTO dto = new FileAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setFileType(attachment.getContentType());
        dto.setFileSize(attachment.getFileSize());
        // Generate signed URL for PDFs
        if ("application/pdf".equals(attachment.getContentType())) {
            dto.setFileUrl(cloudinaryService.generateSignedUrl(attachment.getPublicId()));
        } else {
            dto.setFileUrl(attachment.getFileUrl());
        }
        dto.setPublicId(attachment.getPublicId());
        dto.setUploadedBy(attachment.getUploadedBy());
        dto.setUploadedAt(attachment.getUploadedAt());
        return dto;
    }


    public CommentDTO mapToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setAuthor(comment.getAuthor());
        dto.setWorkItemId(comment.getWorkItem().getId());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        return dto;
    }

}