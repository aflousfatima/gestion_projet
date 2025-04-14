package com.task.taskservice.Repository;

import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectIdAndUserStory(Long projectId, Long userStoryId);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByUserStoryIn(List<Long> userStoryIds);

    // New method to find Task by attachment
    Optional<Task> findByAttachmentsContaining(FileAttachment attachment);

}