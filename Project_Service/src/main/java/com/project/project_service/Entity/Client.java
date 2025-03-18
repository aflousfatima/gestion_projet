package com.project.project_service.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity

@NoArgsConstructor
@AllArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String department;
    private String role;
    @Column(name = "authId")  // Empêche Hibernate de renommer en auth_id
    private String authId;  // ID provenant de Keycloak (ou autre système d'authentification)

    @ManyToOne(cascade = CascadeType.ALL) // Ou un autre type de cascade selon les besoins
    @JoinColumn(name = "company_id")
    private Entreprise company;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    public Entreprise getCompany() {
        return company;
    }

    public void setCompany(Entreprise company) {
        this.company = company;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }
}
