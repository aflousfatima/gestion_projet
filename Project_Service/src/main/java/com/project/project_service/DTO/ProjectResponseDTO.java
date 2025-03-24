package com.project.project_service.DTO;

import java.util.List;

public class ProjectResponseDTO {
    private String companyName;
    private List<String> projects;

    // Constructeurs
    public ProjectResponseDTO() {}

    public ProjectResponseDTO(String companyName, List<String> projects) {
        this.companyName = companyName;
        this.projects = projects;
    }

    // Getters et setters
    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void setProjects(List<String> projects) {
        this.projects = projects;
    }
}