package com.task.taskservice.Repository;

import com.task.taskservice.Entity.ProcessedCommit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedCommitRepository extends JpaRepository<ProcessedCommit, Long> {
    boolean existsByCommitShaAndProjectId(String commitSha, Long projectId);
}