package com.collaboration.collaborationservice.channel.repository;

import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.common.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    List<Channel> findByType(ChannelType type);

    @Query("SELECT c FROM Channel c WHERE c.isPrivate = false")
    List<Channel> findAllPublicChannels();

    @Query("SELECT c FROM Channel c WHERE c.isPrivate = false OR EXISTS " +
            "(SELECT p FROM Participant p WHERE p.channel = c AND p.id = :userId)")
    List<Channel> findAccessibleChannelsByUserId(@Param("userId") String userId);
}