package com.project.project_service.DTO;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.project.project_service.Enumeration.WorkItemStatus;

public class TaskDTO {
    private Long id;
    private Long projectId;
    private Long userStoryId;
    private WorkItemStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public WorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(WorkItemStatus status) {
        this.status = status;
    }

    @JsonSetter("status")
    public void setStatusFromString(String statusString) {
        try {
            this.status = statusString != null ? WorkItemStatus.valueOf(statusString) : null;
        } catch (IllegalArgumentException e) {
            this.status = null;
        }
    }
}