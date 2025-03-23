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
    private String entrepriseId;
    private String project;

    private String token;
    private long expiresAt;
    private boolean used;

    // Constructeurs
    public Invitation() {}

    public Invitation(String email, String role, String entrepriseId,String project, String token, long expiresAt) {
        this.email = email;
        this.role = role;
        this.entrepriseId = entrepriseId;
        this.project=project;
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
    public String getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(String entrepriseId) { this.entrepriseId = entrepriseId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}