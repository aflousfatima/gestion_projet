package com.task.taskservice.DTO;

import java.util.Map;

public class DashboardStatsDTO {
    private long completedTasks;
    private long notCompletedTasks;
    private long overdueTasks;
    private long totalTasks;
    private Map<String, Long> tasksByStatus;
    private Map<String, Long> tasksByPriority;

    // Constructor
    public DashboardStatsDTO(
            long completedTasks,
            long notCompletedTasks,
            long overdueTasks,
            long totalTasks,
            Map<String, Long> tasksByStatus,
            Map<String, Long> tasksByPriority
    ) {
        this.completedTasks = completedTasks;
        this.notCompletedTasks = notCompletedTasks;
        this.overdueTasks = overdueTasks;
        this.totalTasks = totalTasks;
        this.tasksByStatus = tasksByStatus;
        this.tasksByPriority = tasksByPriority;
    }

    // Getters and Setters
    public long getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(long completedTasks) {
        this.completedTasks = completedTasks;
    }

    public long getNotCompletedTasks() {
        return notCompletedTasks;
    }

    public void setNotCompletedTasks(long notCompletedTasks) {
        this.notCompletedTasks = notCompletedTasks;
    }

    public long getOverdueTasks() {
        return overdueTasks;
    }

    public void setOverdueTasks(long overdueTasks) {
        this.overdueTasks = overdueTasks;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public Map<String, Long> getTasksByStatus() {
        return tasksByStatus;
    }

    public void setTasksByStatus(Map<String, Long> tasksByStatus) {
        this.tasksByStatus = tasksByStatus;
    }

    public Map<String, Long> getTasksByPriority() {
        return tasksByPriority;
    }

    public void setTasksByPriority(Map<String, Long> tasksByPriority) {
        this.tasksByPriority = tasksByPriority;
    }
}