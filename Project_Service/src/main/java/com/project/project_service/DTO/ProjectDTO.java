package com.project.project_service.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProjectDTO {
    private Long id;
    private String name;
    private String description;
    private ManagerDTO manager;

    private LocalDate creationDate;
    private LocalDate startDate;
    private LocalDate deadline;
    private String status;
    private String phase;
    private String priority;

    // Constructeur complet
    public ProjectDTO(Long id, String name, String description, ManagerDTO manager,
                      LocalDate creationDate, LocalDate startDate, LocalDate deadline,
                      String status, String phase, String priority) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.manager = manager;
        this.creationDate = creationDate;
        this.startDate = startDate;
        this.deadline = deadline;
        this.status = status;
        this.phase = phase;
        this.priority = priority;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ManagerDTO getManager() {
        return manager;
    }

    public void setManager(ManagerDTO manager) {
        this.manager = manager;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
