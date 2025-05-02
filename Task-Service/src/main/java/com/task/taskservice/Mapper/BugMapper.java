package com.task.taskservice.Mapper;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.DTO.*;
import com.task.taskservice.Entity.Comment;
import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Bug;
import com.task.taskservice.Entity.Tag;
import com.task.taskservice.Service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BugMapper {

    @Autowired
    private CloudinaryService cloudinaryService;

    public BugDTO toDTO(Bug bug) {
        if (bug == null) {
            return null;
        }

        BugDTO bugDTO = new BugDTO();
        bugDTO.setId(bug.getId());
        bugDTO.setTitle(bug.getTitle());
        bugDTO.setDescription(bug.getDescription());
        bugDTO.setCreationDate(bug.getCreationDate());
        bugDTO.setStartDate(bug.getStartDate());
        bugDTO.setDueDate(bug.getDueDate());
        bugDTO.setCompletedDate(bug.getCompletedDate());
        bugDTO.setEstimationTime(bug.getEstimationTime());
        bugDTO.setTotalTimeSpent(bug.getTotalTimeSpent());
        bugDTO.setStartTime(bug.getStartTime());
        bugDTO.setStatus(bug.getStatus());
        bugDTO.setPriority(bug.getPriority());
        bugDTO.setSeverity(bug.getSeverity()); // Champ spécifique aux bugs
        bugDTO.setProgress(bug.getProgress());
        bugDTO.setUserStoryId(bug.getUserStory());
        bugDTO.setProjectId(bug.getProjectId());
        bugDTO.setCreatedBy(bug.getCreatedBy());

        if (bug.getAssignedUserIds() != null) {
            bugDTO.setAssignedUserIds(bug.getAssignedUserIds().stream()
                    .collect(Collectors.toList()));
        } else {
            bugDTO.setAssignedUserIds(Collections.emptyList());
        }

        if (bug.getTags() != null) {
            bugDTO.setTagIds(bug.getTags().stream()
                    .map(Tag::getId)
                    .collect(Collectors.toSet()));
            bugDTO.setTags(bug.getTags().stream()
                    .map(Tag::getName)
                    .collect(Collectors.toSet()));
        }

        if (bug.getAttachments() != null) {
            bugDTO.setAttachments(bug.getAttachments().stream()
                    .map(this::toFileAttachmentDTO)
                    .collect(Collectors.toList()));
        }

        return bugDTO;
    }

    public Bug toEntity(BugDTO bugDTO) {
        if (bugDTO == null) {
            return null;
        }

        Bug bug = new Bug();
        bug.setTitle(bugDTO.getTitle());
        bug.setDescription(bugDTO.getDescription());
        bug.setCreationDate(bugDTO.getCreationDate());
        bug.setStartDate(bugDTO.getStartDate());
        bug.setDueDate(bugDTO.getDueDate());
        bug.setCompletedDate(bugDTO.getCompletedDate());
        bug.setEstimationTime(bugDTO.getEstimationTime());
        bug.setTotalTimeSpent(bugDTO.getTotalTimeSpent());
        bug.setStatus(bugDTO.getStatus());
        bug.setPriority(bugDTO.getPriority());
        bug.setSeverity(bugDTO.getSeverity()); // Champ spécifique aux bugs
        bug.setProgress(bugDTO.getProgress());
        bug.setStartTime(bugDTO.getStartTime());
        bug.setUserStory(bugDTO.getUserStoryId());
        bug.setProjectId(bugDTO.getProjectId());
        bug.setCreatedBy(bugDTO.getCreatedBy());
        if (bugDTO.getAssignedUserIds() != null) {
            bug.setAssignedUserIds(new HashSet<>(bugDTO.getAssignedUserIds()));
        }

        return bug;
    }

    private FileAttachmentDTO toFileAttachmentDTO(FileAttachment attachment) {
        FileAttachmentDTO dto = new FileAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setFileType(attachment.getContentType());
        dto.setFileSize(attachment.getFileSize());
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