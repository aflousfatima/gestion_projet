package com.project.project_service.Repository;

import com.project.project_service.Entity.UserStoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserStoryHistoryRepository extends JpaRepository<UserStoryHistory, Long> {
    List<UserStoryHistory> findByUserStoryId(Long userStoryId);
}