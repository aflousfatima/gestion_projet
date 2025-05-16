package com.collaboration.collaborationservice.call.repository;

import com.collaboration.collaborationservice.call.entity.Call;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallRepository extends JpaRepository<Call, Long> {
    List<Call> findByChannelId(Long channelId);
}