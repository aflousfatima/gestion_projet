package com.task.taskservice.DTO;

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
    private LocalDate lastModifiedDate;
    private LocalDate startDate;
    private LocalDate dueDate;
    private LocalDate completedDate;
    private Long estimationTime; // in minutes or hours
    private Long timeSpent;
    private WorkItemStatus status;
    private WorkItemPriority priority;
    private Double progress; // 0.0 to 100.0
    private String createdBy; // ID of the user who created the task
    private List<Long> assignedUser; // List of user IDs
    private Long userStory; // ID of the User Story
    private List<Long> dependencyIds; // IDs of dependent WorkItems
    private Set<String> tags;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }
    public LocalDate getLastModifiedDate() { return lastModifiedDate; }
    public void setLastModifiedDate(LocalDate lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }
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
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public List<Long> getAssignedUser() { return assignedUser; }
    public void setAssignedUser(List<Long> assignedUser) { this.assignedUser = assignedUser; }
    public Long getUserStory() { return userStory; }
    public void setUserStory(Long userStory) { this.userStory = userStory; }
    public List<Long> getDependencyIds() { return dependencyIds; }
    public void setDependencyIds(List<Long> dependencyIds) { this.dependencyIds = dependencyIds; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
}