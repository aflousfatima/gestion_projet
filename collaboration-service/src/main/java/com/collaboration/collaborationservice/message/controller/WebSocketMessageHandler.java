package com.collaboration.collaborationservice.message.controller;

import com.collaboration.collaborationservice.message.dto.MessageDTO;
import com.collaboration.collaborationservice.message.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.io.IOException;


@Controller
public class WebSocketMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);
    @Autowired
    private MessageService messageService;


    @MessageMapping("/send-audio")
    public void handleAudioMessage(MessageDTO messageDTO, String token) throws IOException {
        // Ne pas appeler uploadAudioMessage ici, car le fichier est téléversé via HTTP
        // La diffusion WebSocket est gérée par le endpoint HTTP après téléversement
        logger.info("Message audio reçu via WebSocket pour channelId: {}", messageDTO.getChannelId());
    }
}