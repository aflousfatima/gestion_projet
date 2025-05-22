package com.task.taskservice.Chatbot.DTO;

public class TaskInput {
    private String title;
    private String description;
    private float estimationTime;
    private float progress;
    private int projectId;
    private String creationDate;
    private String startDate;
    private String dueDate;
    private String status;
    private String priority;
    private String itemtags;
    private String assignedUserIds;

    // Constructeur
    public TaskInput(String title, String description, float estimationTime, float progress, int projectId,
                     String creationDate, String startDate, String dueDate, String status, String priority,
                     String itemtags, String assignedUserIds) {
        this.title = title;
        this.description = description;
        this.estimationTime = estimationTime;
        this.progress = progress;
        this.projectId = projectId;
        this.creationDate = creationDate;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.itemtags = itemtags;
        this.assignedUserIds = assignedUserIds;
    }

    // Getters et setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public float getEstimationTime() { return estimationTime; }
    public void setEstimationTime(float estimationTime) { this.estimationTime = estimationTime; }
    public float getProgress() { return progress; }
    public void setProgress(float progress) { this.progress = progress; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getCreationDate() { return creationDate; }
    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getItemtags() { return itemtags; }
    public void setItemtags(String itemtags) { this.itemtags = itemtags; }
    public String getAssignedUserIds() { return assignedUserIds; }
    public void setAssignedUserIds(String assignedUserIds) { this.assignedUserIds = assignedUserIds; }
}