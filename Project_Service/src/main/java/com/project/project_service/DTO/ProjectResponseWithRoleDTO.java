package com.project.project_service.DTO;


import java.util.List;

public class ProjectResponseWithRoleDTO {
    private String companyName;
    private List<ProjectWithRoleDTO> projects;

    public ProjectResponseWithRoleDTO(String companyName, List<ProjectWithRoleDTO> projects) {
        this.companyName = companyName;
        this.projects = projects;
    }

    // Getters et Setters
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public List<ProjectWithRoleDTO> getProjects() { return projects; }
    public void setProjects(List<ProjectWithRoleDTO> projects) { this.projects = projects; }
}