package com.auth.authentification_service.Entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String role;
    private String entreprise;
    @Column(name = "project_id", nullable = false)
    private Long projectId; // Remplacer "project" par "projectId"

    private String token;
    private long expiresAt;
    private boolean used;

    // Constructeurs
    public Invitation() {}

    public Invitation(String email, String role, String entreprise,Long projectId, String token, long expiresAt) {
        this.email = email;
        this.role = role;
        this.entreprise = entreprise;
        this.projectId=projectId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.used = false;
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEntreprise() { return entreprise; }
    public void setEntreprise(String entreprise) { this.entreprise = entreprise; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }


    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}