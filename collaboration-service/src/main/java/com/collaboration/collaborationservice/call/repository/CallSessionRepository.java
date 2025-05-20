package com.collaboration.collaborationservice.call.repository;

import com.collaboration.collaborationservice.call.entity.Call;
import com.collaboration.collaborationservice.call.entity.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallSessionRepository  extends JpaRepository<CallSession, Long> {
}
