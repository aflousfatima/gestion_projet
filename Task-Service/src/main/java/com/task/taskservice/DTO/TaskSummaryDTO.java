package com.task.taskservice.DTO;
import com.task.taskservice.Enumeration.WorkItemStatus;

public class TaskSummaryDTO {
    private Long id;
    private String title;
    private WorkItemStatus status;
    private Long projectId;
    private Long userStoryId;

    // Constructeur
    public TaskSummaryDTO() {}

    public TaskSummaryDTO(Long id, String title, WorkItemStatus status, Long projectId, Long userStoryId) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.projectId = projectId;
        this.userStoryId = userStoryId;
    }

    // Getters et setters
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

    public WorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(WorkItemStatus status) {
        this.status = status;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getUserStoryId() {
        return userStoryId;
    }

    public void setUserStoryId(Long userStoryId) {
        this.userStoryId = userStoryId;
    }
}
