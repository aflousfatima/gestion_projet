package com.project.project_service.Entity;

import jakarta.persistence.*;

@Entity
@Table(name="githublink")
public class GitHubLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String repositoryUrl;

    @OneToOne
    private Projet projet;

    private String userId;

    // Constructeurs, getters et setters
    public GitHubLink() {}

    public GitHubLink(String repositoryUrl, Projet projet , String userId) {
        this.repositoryUrl = repositoryUrl;
        this.projet = projet;
        this.userId=userId;
    }

    public Long getId() {
        return id;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public Projet getProjet() {
        return projet;
    }

    public void setProjet(Projet projet) {
        this.projet = projet;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}