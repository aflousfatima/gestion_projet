package com.collaboration.collaborationservice.participant.entity;

import com.collaboration.collaborationservice.call.entity.CallSession;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.common.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Getter
@Setter
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "user_id", nullable = false)
    private String userId; // Ou une entité User

    @Enumerated(EnumType.STRING)
    private Role role; // Ex: ADMIN, MEMBER

    private boolean isHost;
    private boolean isMuted;

    private boolean isVideoEnabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_session_id")
    private CallSession callSession;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
}