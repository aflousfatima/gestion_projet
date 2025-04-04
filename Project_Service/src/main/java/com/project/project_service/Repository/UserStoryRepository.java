package com.project.project_service.Repository;

import com.project.project_service.Entity.UserStory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {}
