package com.project.project_service.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name="entreprise")
public class Entreprise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String industry;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "manager_id") // Référence à un client spécifique (le manager)
    private Client manager;
    @OneToMany(mappedBy = "company",cascade = CascadeType.ALL)
    private List<Team> teams;

    @OneToMany(mappedBy = "company",cascade = CascadeType.ALL)
    private List<Projet> projects;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public Client getManager() {
        return manager;
    }

    public void setManager(Client manager) {
        this.manager = manager;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public List<Projet> getProjects() {
        return projects;
    }

    public void setProjects(List<Projet> projects) {
        this.projects = projects;
    }
}