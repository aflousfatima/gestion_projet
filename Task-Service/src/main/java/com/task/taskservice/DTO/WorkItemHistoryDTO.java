package com.task.taskservice.DTO;

import java.time.LocalDateTime;

public class WorkItemHistoryDTO {
    private Long id;
    private String action;
    private String authorName; // Nouveau champ pour le nom complet
    private String description;
    private LocalDateTime timestamp;

    public WorkItemHistoryDTO(Long id, String action, String description, String authorName, LocalDateTime timestamp) {
        this.id = id;
        this.action = action;
        this.description = description;
        this.authorName = authorName;
        this.timestamp = timestamp;
    }

    public WorkItemHistoryDTO(){}
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    // Getters et setters
}