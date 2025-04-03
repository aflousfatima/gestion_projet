package com.project.project_service.DTO;

public class ManagerDTO {
    private Long id;
    private String authId; // Add authId
    private String firstName;
    private String lastName;
    private String role;

    public ManagerDTO(Long id, String authId, String firstName, String lastName, String role) {
        this.id = id;
        this.authId = authId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}