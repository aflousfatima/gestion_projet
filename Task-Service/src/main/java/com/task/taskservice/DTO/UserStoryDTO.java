package com.task.taskservice.DTO;



import java.util.List;
import java.util.stream.Collectors;

public class UserStoryDTO {
    private Long id;
    private String title;
    private String description;
    private String priority;
    private int effortPoints;
    private String status;
    private List<Long> dependsOn;
    private Long projectId; // Include only the ID, not the full Projet object
    private Long sprintId; // Ajouté pour refléter l’association avec le sprint
    // Constructors, getters, setters
    private List<String> tags; // Nouveau champ pour les noms des étiquettes


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

    public List<Long> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<Long> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}