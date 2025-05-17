package com.collaboration.collaborationservice.participant.repository;

import com.collaboration.collaborationservice.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findAllById(Iterable<Long> ids);
}
