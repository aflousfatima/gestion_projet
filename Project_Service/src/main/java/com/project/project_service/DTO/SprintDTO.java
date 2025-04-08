package com.project.project_service.DTO;

import com.project.project_service.Entity.Sprint;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class SprintDTO {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String goal;
    private int capacity;
    private String status;
    private List<UserStoryDTO> userStories;
    // Constructors
    public SprintDTO() {
    }

    public SprintDTO(Sprint sprint) {
        this.id = sprint.getId();
        this.name = sprint.getName();
        this.startDate = sprint.getStartDate();
        this.endDate = sprint.getEndDate();
        this.goal = sprint.getGoal();
        this.capacity=sprint.getCapacity();
        this.status = sprint.getStatus().name(); // Utilisation de .name() pour convertir l'enum en String
        this.userStories = sprint.getUserStories().stream()
                .map(UserStoryDTO::new)
                .collect(Collectors.toList());
    }

    // Getters and Setters
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() { return status; } // Ajout du getter manquant

    public void setStatus(String status) {
        this.status = status;
    }


    public List<UserStoryDTO> getUserStories() {
        return userStories;
    }

    public void setUserStories(List<UserStoryDTO> userStories) {
        this.userStories = userStories;
    }
}