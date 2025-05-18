package com.collaboration.collaborationservice.message.mapper;

import com.collaboration.collaborationservice.message.dto.MessageDTO;
import com.collaboration.collaborationservice.message.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageDTO toDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setChannelId(message.getChannel().getId());
        dto.setSenderId(message.getSender().getUserId());
        dto.setSenderName(message.getSender().getUserId()); // Remplace par un appel à un service pour obtenir le nom réel si nécessaire
        dto.setText(message.getContent().getText());
        dto.setFileUrl(message.getContent().getFileUrl());
        dto.setMimeType(message.getContent().getMimeType());
        dto.setType(message.getType());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }

    public Message toEntity(MessageDTO dto, com.collaboration.collaborationservice.channel.entity.Channel channel, com.collaboration.collaborationservice.participant.entity.Participant sender) {
        Message message = new Message();
        message.setChannel(channel);
        message.setSender(sender);
        com.collaboration.collaborationservice.common.valueobjects.MessageContent content = new com.collaboration.collaborationservice.common.valueobjects.MessageContent();
        content.setText(dto.getText());
        content.setFileUrl(dto.getFileUrl());
        content.setMimeType(dto.getMimeType());
        message.setContent(content);
        message.setType(dto.getType());
        message.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : java.time.LocalDateTime.now());
        return message;
    }
}
