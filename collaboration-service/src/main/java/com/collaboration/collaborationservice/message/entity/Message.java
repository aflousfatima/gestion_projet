package com.collaboration.collaborationservice.message.entity;

import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.common.enums.MessageType;
import com.collaboration.collaborationservice.common.valueobjects.MessageContent;
import com.collaboration.collaborationservice.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant sender;

    @Embedded
    private MessageContent content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type; // TEXT, IMAGE, VIDEO, FILE, SYSTEM

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reaction> reactions = new ArrayList<>();

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private boolean modified = false;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private Message replyTo;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}