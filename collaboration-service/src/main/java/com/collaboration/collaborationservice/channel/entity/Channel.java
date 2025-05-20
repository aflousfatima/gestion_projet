package com.collaboration.collaborationservice.channel.entity;


import com.collaboration.collaborationservice.call.entity.Call;
import com.collaboration.collaborationservice.common.enums.ChannelType;
import com.collaboration.collaborationservice.message.entity.Message;
import com.collaboration.collaborationservice.participant.entity.Participant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "channels")
@Getter
@Setter
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type; // TEXT ou VOCAL

    @Column(name = "created_by", nullable = false) // Champ pour l'ID de l'utilisateur créateur
    private String createdBy;
    @Column(nullable = false)
    private boolean isPrivate; // true pour privé, false pour public

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_id") // Champ pour stocker l'ID du projet (optionnel)
    private Long projectId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations existantes
    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Call> calls = new ArrayList<>();

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}