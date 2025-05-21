package com.collaboration.collaborationservice.participant.repository;

import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByUserId(String userId);
    @Modifying
    @Query("DELETE FROM Participant p WHERE p.channel.id = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);
    List<Participant> findByUserIdIn(List<String> userIds);
    @Query("SELECT p FROM Participant p WHERE p.userId = :userId AND p.channel = :channel")
    Optional<Participant> findByUserIdAndChannel(@Param("userId") String userId, @Param("channel") Channel channel);

    List<Participant> findByChannelId(Long channelId);
    Participant findByChannelIdAndUserId(Long channelId, String userId);
}
