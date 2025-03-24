package com.project.project_service.Repository;

import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.Projet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjetRepository extends JpaRepository<Projet, Long> {
    List<Projet> findByCompany(Entreprise company);
}
