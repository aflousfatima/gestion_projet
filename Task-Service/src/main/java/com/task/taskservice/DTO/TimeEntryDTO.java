package com.task.taskservice.DTO;

import java.time.LocalDateTime;

public class TimeEntryDTO {
    private Long id;
    private Long duration;
    private String addedBy;
    private LocalDateTime addedAt;
    private String type;

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}