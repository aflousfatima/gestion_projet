package com.auth.authentification_service.DTO;

public  class InvitationRequest {
    private String email;
    private String role;
    private String entrepriseId;
    private String project;

    // Getters et setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(String entrepriseId) { this.entrepriseId = entrepriseId; }


    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}

