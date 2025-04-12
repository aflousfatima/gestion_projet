package com.task.taskservice.DTO; // Uppercase DTO

import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate creationDate;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate completedDate;
    private Long estimationTime;
    private Long timeSpent;
    private WorkItemStatus status;
    private WorkItemPriority priority;
    private Double progress;
    private Long userStoryId;
    private Long projectId;
    private String createdBy;
    private List<Long> dependencyIds;
    private Set<Long> assignedUserIds;
    private Set<Long> tagIds;
    private List<CommentDTO> comments;
    private List<FileAttachmentDTO> attachments;

    public TaskDTO() {
    }

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
    public Long getTimeSpent() { return timeSpent; }
    public void setTimeSpent(Long timeSpent) { this.timeSpent = timeSpent; }
    public WorkItemStatus getStatus() { return status; }
    public void setStatus(WorkItemStatus status) { this.status = status; }
    public WorkItemPriority getPriority() { return priority; }
    public void setPriority(WorkItemPriority priority) { this.priority = priority; }
    public Double getProgress() { return progress; }
    public void setProgress(Double progress) { this.progress = progress; }
    public Long getUserStoryId() { return userStoryId; }
    public void setUserStoryId(Long userStoryId) { this.userStoryId = userStoryId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public List<Long> getDependencyIds() { return dependencyIds; }
    public void setDependencyIds(List<Long> dependencyIds) { this.dependencyIds = dependencyIds; }
    public Set<Long> getAssignedUserIds() { return assignedUserIds; }
    public void setAssignedUserIds(Set<Long> assignedUserIds) { this.assignedUserIds = assignedUserIds; }
    public Set<Long> getTagIds() { return tagIds; }
    public void setTagIds(Set<Long> tagIds) { this.tagIds = tagIds; }
    public List<CommentDTO> getComments() { return comments; }
    public void setComments(List<CommentDTO> comments) { this.comments = comments; }
    public List<FileAttachmentDTO> getAttachments() { return attachments; }
    public void setAttachments(List<FileAttachmentDTO> attachments) { this.attachments = attachments; }
}