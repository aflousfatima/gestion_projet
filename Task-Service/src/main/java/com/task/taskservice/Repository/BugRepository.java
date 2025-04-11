package com.task.taskservice.Repository;

import com.task.taskservice.Entity.Bug;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BugRepository extends JpaRepository<Bug, Long> {

}