package com.task.taskservice.Entity;

import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class WorkItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    private String title;
    protected String description;
    protected LocalDate creationDate;
    protected LocalDate lastModifiedDate;
    protected LocalDate startDate;
    protected LocalDate dueDate;
    protected LocalDate completedDate;

    protected Long estimationTime;     // in minutes or hours (depending on convention)
    protected Long timeSpent;

    protected WorkItemStatus status;
    protected WorkItemPriority priority;

    protected Double progress;         // 0.0 to 100.0
    @ElementCollection
    protected List<Long> assignedUser;      // L'ID de l'utilisateur (venant du User MS)
    protected Long userStory;
protected String createdBy;
    protected Long projectId;         // L'ID de la User Story (venant du User Story MS)

    @OneToMany(cascade = CascadeType.ALL)
    protected List<Comment> comments;

    @OneToMany(cascade = CascadeType.ALL)
    protected List<FileAttachment> attachments;

    @ElementCollection
    protected Set<String> tags;        // Optional tagging system


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDate lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public Long getEstimationTime() {
        return estimationTime;
    }

    public void setEstimationTime(Long estimationTime) {
        this.estimationTime = estimationTime;
    }

    public Long getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(Long timeSpent) {
        this.timeSpent = timeSpent;
    }

    public WorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(WorkItemStatus status) {
        this.status = status;
    }

    public WorkItemPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkItemPriority priority) {
        this.priority = priority;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public List<Long> getAssignedUser() {
        return assignedUser;
    }

    public void setAssignedUser(List<Long> assignedUser) {
        this.assignedUser = assignedUser;
    }

    public Long getUserStory() {
        return userStory;
    }

    public void setUserStory(Long userStory) {
        this.userStory = userStory;
    }


    public Long getProjectId() {
        return  projectId;
    }

    public void setProjectId(Long  projectId) {
        this. projectId =  projectId;
    }
    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<FileAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<FileAttachment> attachments) {
        this.attachments = attachments;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }


    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.title = createdBy;
    }
}
