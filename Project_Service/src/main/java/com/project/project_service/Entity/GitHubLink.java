package com.project.project_service.Entity;

import jakarta.persistence.*;

@Entity
public class GitHubLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String repositoryUrl;

    @OneToOne
    private Projet projet;

    // Constructeurs, getters et setters
    public GitHubLink() {}

    public GitHubLink(String repositoryUrl, Projet projet) {
        this.repositoryUrl = repositoryUrl;
        this.projet = projet;
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
}