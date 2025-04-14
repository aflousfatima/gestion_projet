package com.task.taskservice.Repository;

import com.task.taskservice.Entity.Tag;
import com.task.taskservice.Entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}