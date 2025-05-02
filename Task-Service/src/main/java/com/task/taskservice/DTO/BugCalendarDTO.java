package com.task.taskservice.DTO;

import java.time.LocalDate;

public class BugCalendarDTO {
    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate dueDate;

    public BugCalendarDTO(Long id, String title, LocalDate startDate, LocalDate dueDate) {
        this.id = id;
        this.title = title;
        this.startDate = startDate;
        this.dueDate = dueDate;
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}