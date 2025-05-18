package com.collaboration.collaborationservice.message.controller;

import com.collaboration.collaborationservice.message.dto.MessageDTO;
import com.collaboration.collaborationservice.message.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels/{channelId}/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @MessageMapping("/send-message")
    public void sendMessage(@Payload MessageDTO messageDTO, SimpMessageHeaderAccessor headerAccessor) {
        String token = headerAccessor.getFirstNativeHeader("Authorization");
        if (token == null) {
            throw new IllegalArgumentException("Token d'authentification manquant");
        }
        messageService.sendMessage(messageDTO, token);
    }

    @GetMapping
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable Long channelId,
                                                        @RequestHeader("Authorization") String token) {
        List<MessageDTO> messages = messageService.getMessagesByChannelId(channelId, token);
        return ResponseEntity.ok(messages);
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageDTO> updateMessage(@PathVariable Long channelId,
                                                    @PathVariable Long messageId,
                                                    @RequestBody MessageDTO messageDTO,
                                                    @RequestHeader("Authorization") String token) {
        MessageDTO updatedMessage = messageService.updateMessage(channelId, messageId, messageDTO, token);
        return ResponseEntity.ok(updatedMessage);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long channelId,
                                              @PathVariable Long messageId,
                                              @RequestHeader("Authorization") String token) {
        messageService.deleteMessage(channelId, messageId, token);
        return ResponseEntity.noContent().build();
    }
}
