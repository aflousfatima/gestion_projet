package com.project.project_service.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity

@NoArgsConstructor
@AllArgsConstructor
public class Projet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @ManyToOne(cascade = CascadeType.ALL) // Ou un autre type de cascade selon les besoins
    @JoinColumn(name = "company_id")
    private Entreprise company;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Client manager; // Add this field to store the manager

    // Nouveau champ pour la date de création
    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Entreprise getCompany() {
        return company;
    }

    public void setCompany(Entreprise company) {
        this.company = company;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Client getManager() {
        return manager;
    }

    public void setManager(Client manager) {
        this.manager = manager;
    }
}