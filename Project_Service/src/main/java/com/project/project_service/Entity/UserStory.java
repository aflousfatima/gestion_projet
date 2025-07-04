package com.project.project_service.Entity;

import com.project.project_service.Enumeration.Priority;
import com.project.project_service.Enumeration.UserStoryStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_stories")
public class UserStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Projet project;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(name = "effort_points")
    private Integer effortPoints;

    @Column(name = "created_by", nullable = false)
    private String createdBy; // ID du user qui a créé la User Story

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStoryStatus status;

    @ElementCollection
    private List<Long> dependsOn = new ArrayList<>(); // Pour les dépendances

    @ManyToOne
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToMany
    @JoinTable(
            name = "user_story_tags",
            joinColumns = @JoinColumn(name = "user_story_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Projet getProject() {
        return project;
    }

    public void setProject(Projet project) {
        this.project = project;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Integer getEffortPoints() {
        return effortPoints;
    }

    public void setEffortPoints(Integer effortPoints) {
        this.effortPoints = effortPoints;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserStoryStatus getStatus() {
        return status;
    }

    public void setStatus(UserStoryStatus status) {
        this.status = status;
    }

    // Énumérations

    // Setter pour createdAt avant persistance
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Sprint getSprint() {
        return sprint;
    }

    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
    }

    public List<Long> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<Long> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

}