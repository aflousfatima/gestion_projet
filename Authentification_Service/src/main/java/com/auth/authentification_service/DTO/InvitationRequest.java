package com.auth.authentification_service.DTO;

public  class InvitationRequest {
    private String email;
    private String role;
    private String entreprise;
    private String project;

    // Getters et setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEntreprise() { return entreprise; }
    public void setEntreprise(String entreprise) { this.entreprise = entreprise; }


    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}

