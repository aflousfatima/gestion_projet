package com.project.project_service.Repository;

import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.Projet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface ProjetRepository extends JpaRepository<Projet, Long> {
    List<Projet> findByCompany(Entreprise company);
    Projet findByNameAndCompany(String name, Entreprise company);


    @Query("SELECT p FROM Projet p WHERE p.manager.authId = :authId")
    List<Projet> findByManagerAuthId(@Param("authId") String authId);

    Optional<Projet> findByName(String name);
}
