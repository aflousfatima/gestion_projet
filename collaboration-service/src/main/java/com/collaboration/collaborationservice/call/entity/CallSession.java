package com.collaboration.collaborationservice.call.entity;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.common.enums.CallStatus;
import com.collaboration.collaborationservice.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "call_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lien vers le canal (texte/vocal) dans lequel l'appel a lieu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    // Statut de l'appel (ex: ONGOING, ENDED, MISSED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status;

    // Date de début de l'appel
    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    // Date de fin de l'appel (nullable car peut ne pas être encore terminé)
    private Instant endedAt;

    // Liste des participants à cette session d'appel
    @OneToMany(mappedBy = "callSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants;
}
