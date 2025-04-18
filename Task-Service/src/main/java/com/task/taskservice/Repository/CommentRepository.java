package com.task.taskservice.Repository;

import com.task.taskservice.Entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByWorkItemIdOrderByCreatedAtAsc(Long workItemId);
}
