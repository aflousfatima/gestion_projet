package com.project.project_service.DTO;

import java.time.LocalDateTime;

public class UserStoryHistoryDto {
    private String action;
    private LocalDateTime date;
    private String authorFullName; // Ce champ contiendra "Prenom Nom"
    private String description;

    // Constructeurs, getters/setters
    public UserStoryHistoryDto(String action, LocalDateTime date, String authorFullName, String description) {
        this.action = action;
        this.date = date;
        this.authorFullName = authorFullName;
        this.description = description;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getAuthorFullName() {
        return authorFullName;
    }

    public void setAuthorFullName(String authorFullName) {
        this.authorFullName = authorFullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
