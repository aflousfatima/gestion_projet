package com.task.taskservice.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;



@Entity
public class WorkItemHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workitem_id")
    private WorkItem workItem;  // Référence vers la tâche associée

    private String action;  // Type d'action effectuée (ex: "modification du statut", "ajout de commentaire", etc.)

    private String authorId; // Nouveau champ pour stocker l'ID de l'utilisateur
    private LocalDateTime date;  // Date et heure de l'action

    private String description;  // Détails de l'action (par exemple, "Changement de statut de TO_DO à IN_PROGRESS")

    public WorkItemHistory() {
    }


    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WorkItem getWorkItem() { return workItem; }
    public void setWorkItem(WorkItem workItem) {
        this.workItem = workItem;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getTimestamp() { return date; }
    public void setTimestamp(LocalDateTime timestamp) { this.date = timestamp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }
}

