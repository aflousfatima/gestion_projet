package com.task.taskservice.Repository;


import com.task.taskservice.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}