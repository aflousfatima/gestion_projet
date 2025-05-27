package com.task.taskservice.Repository;

import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.userStory = :userStoryId")
    List<Task> findByProjectIdAndUserStory(Long projectId, Long userStoryId);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByUserStoryIn(List<Long> userStoryIds);

    // New method to find Task by attachment
    Optional<Task> findByAttachmentsContaining(FileAttachment attachment);

    List<Task> findByDependenciesId(Long taskId);


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


    List<Task> findByDueDateBeforeAndStatusNot(LocalDate dueDate, WorkItemStatus status);

    @Query("SELECT t.status AS status, COUNT(t) AS count FROM Task t WHERE t.projectId = :projectId GROUP BY t.status")
    List<Object[]> countTasksByStatusForProject(Long projectId);

    List<Task> findByPriority(WorkItemPriority priority);

    @Query("SELECT t FROM Task t WHERE EXISTS (SELECT d FROM t.dependencies d WHERE d.status != :status)")
    List<Task> findBlockedTasksByDependencies(WorkItemStatus status);

    @Query("SELECT t FROM Task t WHERE :userId MEMBER OF t.assignedUserIds")
    List<Task> findByAssignedUserId(String userId);

    Optional<Task> findByTitle(String title);
}