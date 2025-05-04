package com.task.taskservice.DTO;


import java.time.LocalDateTime;

public class ProjectWithRoleDTO {
    private Long id;
    private String name;
    private String description;
    private String roleInProject; // Champ pour le rôle
    private LocalDateTime creationDate;
    private LocalDateTime startDate;
    private LocalDateTime deadline;
    private String status;
    private String phase;
    private String priority;

    public ProjectWithRoleDTO(Long id, String name, String description, String roleInProject,
                              LocalDateTime creationDate, LocalDateTime startDate, LocalDateTime deadline,
                              String status, String phase, String priority) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.roleInProject = roleInProject;
        this.creationDate = creationDate;
        this.startDate = startDate;
        this.deadline = deadline;
        this.status = status;
        this.phase = phase;
        this.priority = priority;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRoleInProject() { return roleInProject; }
    public void setRoleInProject(String roleInProject) { this.roleInProject = roleInProject; }
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}