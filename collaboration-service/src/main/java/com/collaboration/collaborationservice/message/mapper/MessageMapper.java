package com.collaboration.collaborationservice.message.mapper;

import com.collaboration.collaborationservice.message.dto.MessageDTO;
import com.collaboration.collaborationservice.message.entity.Message;
import com.collaboration.collaborationservice.message.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.method.AuthorizeReturnObject;
import org.springframework.stereotype.Component;
import com.collaboration.collaborationservice.message.entity.Reaction;

import java.util.Map;
import java.util.stream.Collectors;
@Component
public class MessageMapper {

    @Autowired
    private MessageRepository messageRepository;

    public MessageDTO toDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setChannelId(message.getChannel().getId());
        dto.setSenderId(message.getSender().getUserId());
        dto.setSenderName(message.getSender().getUserId()); // Remplace par un appel à un service pour obtenir le nom réel si nécessaire
        dto.setText(message.getContent().getText());
        dto.setFileUrl(message.getContent().getFileUrl());
        dto.setMimeType(message.getContent().getMimeType());
        dto.setModified(message.isModified());
        dto.setType(message.getType());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setPinned(message.isPinned()); // Ajout explicite du champ pinned
        // Mapper les réactions
        Map<String, String[]> reactions = message.getReactions().stream()
                .collect(Collectors.groupingBy(
                        Reaction::getEmoji,
                        Collectors.mapping(reaction -> reaction.getParticipant().getUserId(), Collectors.toList())
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toArray(new String[0])
                ));
        dto.setReactions(reactions);
        if (message.getReplyTo() != null) {
            dto.setReplyToId(message.getReplyTo().getId());
            dto.setReplyToText(message.getReplyTo().getContent().getText());
            dto.setReplyToSenderName(message.getReplyTo().getSender().getUserId()); // À enrichir avec le nom
        }
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
        message.setPinned(dto.isPinned()); // Ajout pour gérer pinned dans l'entité
        if (dto.getReplyToId() != null) {
            Message replyTo = messageRepository.findById(dto.getReplyToId())
                    .orElseThrow(() -> new IllegalArgumentException("Message cité non trouvé"));
            message.setReplyTo(replyTo);
        }
        return message;
    }
}
