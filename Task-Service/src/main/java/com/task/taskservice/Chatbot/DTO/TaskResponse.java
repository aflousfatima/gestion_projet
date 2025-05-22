package com.task.taskservice.Chatbot.DTO;

import com.task.taskservice.Enumeration.WorkItemPriority;
import java.time.LocalDate;
import java.util.List;

public class TaskResponse {
    private Long id;
    private String title;
    private LocalDate dueDate;
    private WorkItemPriority priority;
    private List<String> assignedUsers;
    private List<Long> dependenciesId;

    public TaskResponse(Long id, String title, LocalDate dueDate, WorkItemPriority priority, List<String> assignedUsers, List<Long> dependenciesId) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
        this.assignedUsers = assignedUsers;
        this.dependenciesId = dependenciesId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public WorkItemPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkItemPriority priority) {
        this.priority = priority;
    }

    public List<String> getAssignedUsers() {
        return assignedUsers;
    }

    public void setAssignedUsers(List<String> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }

    public List<Long> getDependenciesId() {
        return dependenciesId;
    }

    public void setDependenciesId(List<Long> dependenciesId) {
        this.dependenciesId = dependenciesId;
    }
}