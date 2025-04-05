package com.project.project_service.DTO;

import com.project.project_service.Entity.Sprint;

import java.time.LocalDate;

public class SprintDTO {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String goal;
    private int capacity;
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
}