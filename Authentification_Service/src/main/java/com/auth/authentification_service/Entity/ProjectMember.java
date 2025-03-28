package com.auth.authentification_service.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id;

    @Column(name = "role_in_project", nullable = false)
    private String roleInProject;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // Constructeurs
    public ProjectMember() {}

    public ProjectMember(Long projectId, String userId, String roleInProject) {
        this.id = new ProjectMemberId(projectId, userId);
        this.roleInProject = roleInProject;
        this.joinedAt = LocalDateTime.now();
    }

    // Getters et Setters
    public ProjectMemberId getId() { return id; }
    public void setId(ProjectMemberId id) { this.id = id; }
    public String getRoleInProject() { return roleInProject; }
    public void setRoleInProject(String roleInProject) { this.roleInProject = roleInProject; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}