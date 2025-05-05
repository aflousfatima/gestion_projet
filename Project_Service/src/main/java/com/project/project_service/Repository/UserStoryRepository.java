package com.project.project_service.Repository;

import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.UserStoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    List<UserStory> findByProject(Projet projet);
    List<UserStory> findByStatus(UserStoryStatus status);
    List<UserStory> findBySprintId(Long sprintId);  // Méthode de base
}
