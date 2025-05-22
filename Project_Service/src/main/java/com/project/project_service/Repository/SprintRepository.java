package com.project.project_service.Repository;

import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Enumeration.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProject(Projet project);

    Sprint findByProjectIdAndStatus(Long projectId, SprintStatus status);


}