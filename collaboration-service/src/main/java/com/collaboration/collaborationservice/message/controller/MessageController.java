package com.collaboration.collaborationservice.message.controller;

import com.collaboration.collaborationservice.message.dto.MessageDTO;
import com.collaboration.collaborationservice.message.dto.ReactionDTO;
import com.collaboration.collaborationservice.message.service.MessageService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/channels/{channelId}/messages")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
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

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<MessageDTO> addReaction(
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @RequestBody ReactionDTO reactionDTO,
            @RequestHeader("Authorization") String token) {
        logger.info("Requête pour ajouter une réaction au message {} dans le canal {}", messageId, channelId);
        try {

            MessageDTO updatedMessage = messageService.addReaction(channelId, messageId, reactionDTO, token);
            return ResponseEntity.ok(updatedMessage);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de l'ajout de la réaction: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Erreur serveur lors de l'ajout de la réaction: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<MessageDTO> removeReaction(
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @RequestBody ReactionDTO reactionDTO,
            @RequestHeader("Authorization") String token
    ) {
        logger.info("Requête pour supprimer une réaction au message {} dans le canal {}", messageId, channelId);
        try {
            MessageDTO updatedMessage = messageService.removeReaction(channelId, messageId, reactionDTO, token);
            return ResponseEntity.ok(updatedMessage);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de la suppression de la réaction: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Erreur serveur lors de la suppression de la réaction: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }
    @PostMapping("/{messageId}/pin")
    public ResponseEntity<MessageDTO> pinMessage(
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @RequestHeader("Authorization") String token) {
        logger.info("Requête pour épingler/désépingler le message {} dans le canal {}", messageId, channelId);
        try {
            MessageDTO updatedMessage = messageService.pinMessage(channelId, messageId , token);
            return ResponseEntity.ok(updatedMessage);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de l'épinglage du message: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Erreur serveur lors de l'épinglage du message: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/audio")
    public ResponseEntity<MessageDTO> uploadAudioMessage(
            @PathVariable Long channelId,
            @RequestParam("file") MultipartFile audioFile,
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "replyToId", required = false) Long replyToId,
            @RequestParam(value = "duration", required = false) String duration) {
        logger.info("Requête pour envoyer un message audio dans le canal {}", channelId);
        try {
            MessageDTO messageDTO = messageService.uploadAudioMessage(channelId, audioFile, token, replyToId, duration);
            return ResponseEntity.ok(messageDTO);
        } catch (IOException e) {
            logger.error("Erreur lors du téléversement du fichier audio: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de l'envoi du message audio: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }


    @PostMapping("/image")
    public ResponseEntity<MessageDTO> uploadImageMessage(
            @PathVariable Long channelId,
            @RequestParam("file") MultipartFile imageFile,
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "replyToId", required = false) Long replyToId) throws IOException {
        logger.info("Téléversement d'une image pour le canal: {}", channelId);
        MessageDTO messageDTO = messageService.uploadImageMessage(channelId, imageFile, token, replyToId);
        return ResponseEntity.ok(messageDTO);
    }

    @PostMapping("/file")
    public ResponseEntity<MessageDTO> uploadFileMessage(
            @PathVariable Long channelId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "replyToId", required = false) Long replyToId) throws IOException {
        logger.info("Téléversement d'un fichier pour le canal: {}", channelId);
        MessageDTO messageDTO = messageService.uploadFileMessage(channelId, file, token, replyToId);
        return ResponseEntity.ok(messageDTO);
    }
}
