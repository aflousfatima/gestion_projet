package com.project.project_service.Repository;

import com.project.project_service.Entity.GitHubLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GitHubLinkRepository extends JpaRepository<GitHubLink, Long> {
    GitHubLink findByProjetId(Long projectId);

    @Query("SELECT g FROM GitHubLink g WHERE g.projet.id = :projectId")
    Optional<GitHubLink> findByProjectId(@Param("projectId") Long projectId);
}