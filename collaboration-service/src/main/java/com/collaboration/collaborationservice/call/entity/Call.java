package com.collaboration.collaborationservice.call.entity;


import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.common.enums.CallStatus;
import com.collaboration.collaborationservice.common.enums.CallType;
import com.collaboration.collaborationservice.common.valueobjects.SdpInfo;
import com.collaboration.collaborationservice.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calls")
@Getter
@Setter
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private Participant initiator; // Participant qui a démarré l'appel

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status; // INITIATED, ACTIVE, ENDED, FAILED

    @Embedded
    private SdpInfo sdpInfo; // Informations WebRTC (SDP pour la connexion)

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @ManyToMany
    @JoinTable(
            name = "call_participants",
            joinColumns = @JoinColumn(name = "call_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id")
    )
    private List<Participant> participants = new ArrayList<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallType type; // VOICE, VIDEO

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}