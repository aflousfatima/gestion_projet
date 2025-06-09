package com.task.taskservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

public class TaskAssignedEvent {
    @JsonProperty("taskId")
    private final Long taskId;

    @JsonProperty("userIds")
    private final Set<String> userIds;

    @JsonProperty("assignedBy")
    private final String assignedBy;

    @JsonProperty("assignedAt")
    private final LocalDateTime assignedAt;

    public TaskAssignedEvent(
            @JsonProperty("taskId") Long taskId,
            @JsonProperty("userIds") Set<String> userIds,
            @JsonProperty("assignedBy") String assignedBy,
            @JsonProperty("assignedAt") LocalDateTime assignedAt) {
        this.taskId = taskId;
        this.userIds = userIds;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Set<String> getUserIds() {
        return userIds;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
}