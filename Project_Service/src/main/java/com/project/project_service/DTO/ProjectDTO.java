package com.project.project_service.DTO;

public class ProjectDTO {
    private Long id;
    private String name;
    private String description;

    // Constructeur
    public ProjectDTO(Long id , String name, String description) {
        this.name = name;
        this.description = description;
        this.id = id;
    }

    // Getters et setters
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
