package com.task.taskservice.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.task.taskservice.Enumeration.BugSeverity;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class BugDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate creationDate;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate completedDate;
    private Long estimationTime;
    private Long totalTimeSpent;
    private WorkItemStatus status;
    private WorkItemPriority priority;
    private BugSeverity severity; // Champ spécifique aux bugs
    private Double progress;
    private Long userStoryId;
    private Long projectId;
    private String createdBy;

    @JsonProperty("assignedUser")
    private List<String> assignedUserIds;
    private LocalDateTime startTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<UserDTO> assignedUsers;

    private Set<Long> tagIds;

    @JsonProperty("tags")
    private Set<String> tags;
    private List<CommentDTO> comments;
    private List<FileAttachmentDTO> attachments;

    public BugDTO() {
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }
    public Long getEstimationTime() { return estimationTime; }
    public void setEstimationTime(Long estimationTime) { this.estimationTime = estimationTime; }
    public Long getTotalTimeSpent() { return totalTimeSpent; }
    public void setTotalTimeSpent(Long totalTimeSpent) { this.totalTimeSpent = totalTimeSpent; }
    public WorkItemStatus getStatus() { return status; }
    public void setStatus(WorkItemStatus status) { this.status = status; }
    public WorkItemPriority getPriority() { return priority; }
    public void setPriority(WorkItemPriority priority) { this.priority = priority; }
    public BugSeverity getSeverity() { return severity; }
    public void setSeverity(BugSeverity severity) { this.severity = severity; }
    public Double getProgress() { return progress; }
    public void setProgress(Double progress) { this.progress = progress; }
    public Long getUserStoryId() { return userStoryId; }
    public void setUserStoryId(Long userStoryId) { this.userStoryId = userStoryId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public List<String> getAssignedUserIds() { return assignedUserIds; }
    public void setAssignedUserIds(List<String> assignedUserIds) { this.assignedUserIds = assignedUserIds; }
    public Set<Long> getTagIds() { return tagIds; }
    public void setTagIds(Set<Long> tagIds) { this.tagIds = tagIds; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public List<CommentDTO> getComments() { return comments; }
    public void setComments(List<CommentDTO> comments) { this.comments = comments; }
    public List<FileAttachmentDTO> getAttachments() { return attachments; }
    public void setAttachments(List<FileAttachmentDTO> attachments) { this.attachments = attachments; }
    public List<UserDTO> getAssignedUsers() { return assignedUsers; }
    public void setAssignedUsers(List<UserDTO> assignedUsers) { this.assignedUsers = assignedUsers; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
}