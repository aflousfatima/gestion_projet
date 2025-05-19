package com.collaboration.collaborationservice.message.dto;

import com.collaboration.collaborationservice.common.enums.MessageType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class MessageDTO {
    private Long id;
    private Long channelId;
    private String senderId;
    private String senderName;
    private String text;
    private String fileUrl;
    private String mimeType;
    private MessageType type;
    private LocalDateTime createdAt;
    private Map<String, String[]> reactions = new HashMap<>();
    private Long replyToId; // ID du message auquel on répond
    private String replyToText; // Texte du message cité
    private String replyToSenderName; // Nom de l'expéditeur du message cité
    private boolean pinned;
    private boolean modified;
}