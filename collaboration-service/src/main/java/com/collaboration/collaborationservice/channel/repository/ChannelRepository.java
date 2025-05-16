package com.collaboration.collaborationservice.channel.repository;

import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.common.enums.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    // Ajoute des méthodes personnalisées si besoin, ex:
    List<Channel> findByType(ChannelType type);
}