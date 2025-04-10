package com.project.project_service.Repository;

import com.project.project_service.Entity.SprintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SprintHistoryRepository extends JpaRepository<SprintHistory, Long> {
    List<SprintHistory> findBySprintIdOrderByDateDesc(Long sprintId);

}