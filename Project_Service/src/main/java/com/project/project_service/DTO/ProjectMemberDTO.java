package com.project.project_service.DTO;


public class ProjectMemberDTO {
    private Long projectId;
    private String roleInProject;

    public ProjectMemberDTO(Long projectId, String roleInProject) {
        this.projectId = projectId;
        this.roleInProject = roleInProject;
    }

    // Getters et Setters
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getRoleInProject() { return roleInProject; }
    public void setRoleInProject(String roleInProject) { this.roleInProject = roleInProject; }
}