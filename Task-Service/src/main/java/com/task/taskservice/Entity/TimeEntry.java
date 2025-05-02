package com.task.taskservice.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TimeEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workitem_id") // Changé de task_id à workitem_id
    private WorkItem workItem; // Changé de Task à WorkItem

    private Long duration; // En minutes
    private String addedBy; // Utilisateur
    private LocalDateTime addedAt;
    private String type; // Par exemple, "travail", "réunion"

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WorkItem getWorkItem() { return workItem; }
    public void setWorkItem(WorkItem workItem) { this.workItem = workItem; }
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}