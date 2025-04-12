package com.task.taskservice.DTO;

import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private String content;
    private String author;
    private Long workItemId; // ou taskId selon ta structure
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Long getWorkItemId() {
        return workItemId;
    }

    public void setWorkItemId(Long workItemId) {
        this.workItemId = workItemId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime  updatedAt) {
        this.updatedAt = updatedAt;
    }
}
