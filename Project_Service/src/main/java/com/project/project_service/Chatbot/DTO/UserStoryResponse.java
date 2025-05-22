package com.project.project_service.Chatbot.DTO;

public class UserStoryResponse {
    private Long id;
    private String title;

    public UserStoryResponse(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}