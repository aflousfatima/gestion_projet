package com.collaboration.collaborationservice.message.dto;

import com.collaboration.collaborationservice.common.enums.MessageType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
}