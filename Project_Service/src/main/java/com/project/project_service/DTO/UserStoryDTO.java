package com.project.project_service.DTO;

import com.project.project_service.Entity.UserStory;

public class UserStoryDTO {
    private Long id;
    private String title;
    private String description;
    private String priority;
    private int effortPoints;
    private String status;
    private Long projectId; // Include only the ID, not the full Projet object
    private Long sprintId; // Ajouté pour refléter l’association avec le sprint
    // Constructors, getters, setters
    public UserStoryDTO(UserStory userStory) {
        this.id = userStory.getId();
        this.title = userStory.getTitle();
        this.description = userStory.getDescription();
        this.priority = userStory.getPriority().name();
        this.effortPoints = userStory.getEffortPoints();
        this.status = userStory.getStatus().name();
        this.projectId = userStory.getProject().getId();
        this.sprintId = userStory.getSprint() != null ? userStory.getSprint().getId() : null; // Vérification explicite
    }

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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getEffortPoints() {
        return effortPoints;
    }

    public void setEffortPoints(int effortPoints) {
        this.effortPoints = effortPoints;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }
}