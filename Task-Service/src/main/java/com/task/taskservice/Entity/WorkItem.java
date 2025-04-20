package com.task.taskservice.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
    @Enumerated(EnumType.STRING)

    protected WorkItemStatus status;
    @Enumerated(EnumType.STRING)

    protected WorkItemPriority priority;

    protected Double progress;         // 0.0 to 100.0

    protected String updatedBy;


    @OneToMany(mappedBy = "workItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<WorkItemHistory> history = new HashSet<>();  // Historique des tâches

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "workitem_tags",
            joinColumns = @JoinColumn(name = "workitem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> itemtags = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "workitem_assigned_user",
            joinColumns = @JoinColumn(name = "workitem_id")
    )
    @Column(name = "user_id")
    private Set<String> assignedUserIds = new HashSet<>();
    protected Long userStory;
protected String createdBy;
    protected Long projectId;         // L'ID de la User Story (venant du User Story MS)

    @OneToMany(cascade = CascadeType.ALL)
    @JsonIgnore
    protected List<Comment> comments;

    @OneToMany(cascade = CascadeType.ALL)
    protected List<FileAttachment> attachments;



    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "total_time_spent")
    private Long totalTimeSpent = 0L; // En minutes

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeEntry> timeEntries = new ArrayList<>();

    // Getters et setters
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public Long getTotalTimeSpent() { return totalTimeSpent; }
    public void setTotalTimeSpent(Long totalTimeSpent) { this.totalTimeSpent = totalTimeSpent; }
    public List<TimeEntry> getTimeEntries() { return timeEntries; }
    public void setTimeEntries(List<TimeEntry> timeEntries) { this.timeEntries = timeEntries; }

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




    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }



    public Set<WorkItemHistory> getHistory() {
        return history;
    }

    public void setHistory(Set<WorkItemHistory> history) {
        this.history = history;
    }

    public Set<Tag> getTags() {
        return itemtags;
    }

    public void setTags(Set<Tag> workitemtags) {
        this.itemtags = workitemtags;
    }

    // Getters and setters
    public Set<String> getAssignedUserIds() {
        return assignedUserIds;
    }

    public void setAssignedUserIds(Set<String> assignedUserIds) {
        this.assignedUserIds = assignedUserIds;
    }



    public void addTag(Tag tag) {
        itemtags.add(tag);
        tag.getWorkItems().add(this);
    }

    // Getters et setters
    public Set<Tag> getItemtags() {
        return itemtags;
    }

    public void setItemtags(Set<Tag> itemtags) {
        this.itemtags = itemtags;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void updateStatus(WorkItemStatus newStatus, String userId) {
        this.status = newStatus;
        this.lastModifiedDate = LocalDate.now();
        this.updatedBy = userId;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", attachments=" + attachments +
                '}';
    }
}
