package com.collaboration.collaborationservice.message.repository;

import com.collaboration.collaborationservice.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChannelIdOrderByCreatedAtAsc(Long channelId);
}