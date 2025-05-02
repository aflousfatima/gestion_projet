package com.task.taskservice.Repository;

import com.task.taskservice.Entity.Bug;
import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Enumeration.WorkItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface BugRepository extends JpaRepository<Bug, Long> {
    List<Bug> findByProjectIdAndUserStory(Long projectId, Long userStoryId);
    List<Bug> findByProjectId(Long projectId);
    List<Bug> findByUserStoryIn(List<Long> userStoryIds);
    long countByUserStoryInAndStatus(List<Long> userStoryIds, WorkItemStatus status);
    long countByUserStoryInAndStatusNot(List<Long> userStoryIds, WorkItemStatus status);
    @Query("SELECT COUNT(b) FROM Bug b WHERE b.userStory IN :userStoryIds AND b.status != :status AND b.dueDate < :currentDate")
    long countOverdueByUserStoryIn(List<Long> userStoryIds, WorkItemStatus status, LocalDate currentDate);
    long countByUserStoryIn(List<Long> userStoryIds);
    @Query("SELECT b.status, COUNT(b) FROM Bug b WHERE b.userStory IN :userStoryIds GROUP BY b.status")
    List<Object[]> countBugsByStatus(List<Long> userStoryIds);
    @Query("SELECT b.priority, COUNT(b) FROM Bug b WHERE b.userStory IN :userStoryIds GROUP BY b.priority")
    List<Object[]> countBugsByPriority(List<Long> userStoryIds);
    List<Bug> findByAttachmentsContaining(FileAttachment attachment);
}