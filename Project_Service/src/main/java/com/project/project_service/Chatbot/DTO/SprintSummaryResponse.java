package com.project.project_service.Chatbot.DTO;

import java.time.LocalDate;

public class SprintSummaryResponse {
    private String sprintName;
    private double progress;
    private long totalTasks;
    private long completedTasks;
    private LocalDate endDate;

    public SprintSummaryResponse(String sprintName, double progress, long totalTasks, long completedTasks, LocalDate endDate) {
        this.sprintName = sprintName;
        this.progress = progress;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.endDate = endDate;
    }

    public String getSprintName() { return sprintName; }
    public void setSprintName(String sprintName) { this.sprintName = sprintName; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public long getTotalTasks() { return totalTasks; }
    public void setTotalTasks(long totalTasks) { this.totalTasks = totalTasks; }
    public long getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(long completedTasks) { this.completedTasks = completedTasks; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}