package com.project.project_service.Repository;

import com.project.project_service.Entity.GitHubLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubLinkRepository extends JpaRepository<GitHubLink, Long> {
    GitHubLink findByProjetId(Long projectId);
}