package com.project.project_service.Chatbot.DTO;

import java.time.LocalDate;

public class ProjectRemainingTimeResponse {
    private String projectName;
    private long remainingDays;
    private LocalDate endDate;

    public ProjectRemainingTimeResponse(String projectName, long remainingDays, LocalDate endDate) {
        this.projectName = projectName;
        this.remainingDays = remainingDays;
        this.endDate = endDate;
    }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public long getRemainingDays() { return remainingDays; }
    public void setRemainingDays(long remainingDays) { this.remainingDays = remainingDays; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}