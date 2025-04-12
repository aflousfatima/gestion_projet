package com.task.taskservice.Repository;

import com.task.taskservice.Entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectIdAndUserStory(Long projectId, Long userStoryId);
    List<Task> findByProjectId(Long projectId);


    List<Task> findByUserStoryIn(List<Long> userStoryIds);

}