package com.project.project_service.DTO;

import java.util.List;

public class ProjectResponseDTO {
    private String companyName;
    private List<ProjectDTO> projects;

    public ProjectResponseDTO(String companyName, List<ProjectDTO> projects) {
        this.companyName = companyName;
        this.projects = projects;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<ProjectDTO> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectDTO> projects) {
        this.projects = projects;
    }
}