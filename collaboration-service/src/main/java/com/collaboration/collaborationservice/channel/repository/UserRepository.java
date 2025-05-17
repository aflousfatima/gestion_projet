package com.collaboration.collaborationservice.channel.repository;

import com.collaboration.collaborationservice.channel.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
