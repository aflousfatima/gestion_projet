package com.task.taskservice.Repository;

import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Enumeration.WorkItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectIdAndUserStory(Long projectId, Long userStoryId);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByUserStoryIn(List<Long> userStoryIds);

    // New method to find Task by attachment
    Optional<Task> findByAttachmentsContaining(FileAttachment attachment);



    @Query("SELECT COUNT(t) FROM Task t WHERE t.userStory IN :userStoryIds AND t.status = :status")
    long countByUserStoryInAndStatus(List<Long> userStoryIds, WorkItemStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.userStory IN :userStoryIds AND t.status != :status")
    long countByUserStoryInAndStatusNot(List<Long> userStoryIds, WorkItemStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.userStory IN :userStoryIds AND t.status != :status AND t.dueDate < :currentDate")
    long countOverdueByUserStoryIn(List<Long> userStoryIds, WorkItemStatus status, LocalDate currentDate);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.userStory IN :userStoryIds")
    long countByUserStoryIn(List<Long> userStoryIds);

    @Query("SELECT t.status AS status, COUNT(t) AS count FROM Task t WHERE t.userStory IN :userStoryIds GROUP BY t.status")
    List<Object[]> countTasksByStatus(List<Long> userStoryIds);

    @Query("SELECT t.priority AS priority, COUNT(t) AS count FROM Task t WHERE t.userStory IN :userStoryIds GROUP BY t.priority")
    List<Object[]> countTasksByPriority(List<Long> userStoryIds);
}