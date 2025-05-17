package com.collaboration.collaborationservice.attachment.entity;


import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.common.valueobjects.AttachmentInfo;
import com.collaboration.collaborationservice.message.entity.Message;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@Getter
@Setter
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message; // Optionnel, si l'attachment est lié à un message

    @Embedded
    private AttachmentInfo info; // Contient ex. nom du fichier, taille, type MIME
    private String fileType;

    private String url;
    private String uploaderId; // ID de l'utilisateur

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}