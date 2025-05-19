package com.collaboration.collaborationservice.message.repository;

import com.collaboration.collaborationservice.message.entity.Message;
import com.collaboration.collaborationservice.message.entity.Reaction;
import com.collaboration.collaborationservice.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByMessageId(Long messageId);
    Optional<Reaction> findByMessageAndParticipantAndEmoji(Message message, Participant participant, String emoji);
}