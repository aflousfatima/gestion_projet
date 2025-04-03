package com.project.project_service.DTO;

public class ProjectDetailsDTO {
    private Long id;
    private String name;
    private String description;
    private ManagerDTO manager;

    public ProjectDetailsDTO(Long id, String name, String description, ManagerDTO manager) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.manager = manager;
    }

    // Getters and setters
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
}



