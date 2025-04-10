package com.project.project_service.Entity;

import com.project.project_service.Enumeration.PhaseProjet;
import com.project.project_service.Enumeration.PriorityProjet;
import com.project.project_service.Enumeration.StatusProjet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Entreprise company;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Client manager; // Add this field to store the manager

    // Nouveau champ pour la date de création
    @Column(name = "creation_date")
    private LocalDate creationDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private StatusProjet status;

    @Enumerated(EnumType.STRING)
    private PhaseProjet phase;

    @Enumerated(EnumType.STRING)
    private PriorityProjet priority;

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

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public Client getManager() {
        return manager;
    }

    public void setManager(Client manager) {
        this.manager = manager;
    }



    public PhaseProjet getPhase() {
        return phase;
    }

    public void setPhase(PhaseProjet phase) {
        this.phase = phase;
    }


    public PriorityProjet getPriority() {
        return priority;
    }

    public void setPriority(PriorityProjet priority) {
        this.priority = priority;
    }

    public StatusProjet getStatus() {
        return status;
    }

    public void setStatus(StatusProjet status) {
        this.status = status;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}