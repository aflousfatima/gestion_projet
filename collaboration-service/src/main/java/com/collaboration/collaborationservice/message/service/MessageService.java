package com.collaboration.collaborationservice.message.service;

import com.collaboration.collaborationservice.channel.config.AuthClient;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.repository.ChannelRepository;
import com.collaboration.collaborationservice.message.dto.MessageDTO;
import com.collaboration.collaborationservice.message.entity.Message;
import com.collaboration.collaborationservice.message.mapper.MessageMapper;
import com.collaboration.collaborationservice.message.repository.MessageRepository;
import com.collaboration.collaborationservice.participant.entity.Participant;
import com.collaboration.collaborationservice.participant.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private AuthClient authClient;

    private MessageDTO enrichMessageDTO(MessageDTO messageDTO, String token) {
        try {
            Map<String, Object> userDetails = authClient.getUserDetailsByAuthId(
                    messageDTO.getSenderId(),
                    token
            );
            String firstName = (String) userDetails.get("firstName");
            String lastName = (String) userDetails.get("lastName");
            messageDTO.setSenderName(firstName + " " + lastName);
        } catch (Exception e) {
            logger.warn("Impossible de récupérer les détails de l'utilisateur pour senderId: {}. Erreur: {}",
                    messageDTO.getSenderId(), e.getMessage());
            messageDTO.setSenderName("Utilisateur Inconnu");
        }
        return messageDTO;
    }

    @Transactional
    public MessageDTO sendMessage(MessageDTO messageDTO, String token) {
        try {
            logger.info("Envoi d'un message pour channelId: {}", messageDTO.getChannelId());

            // Récupérer l'utilisateur à partir du token
            String userId = authClient.decodeToken(token);
            if (userId == null) {
                logger.error("Échec du décodage du token: userId est null");
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            // Récupérer le canal
            Channel channel = channelRepository.findById(messageDTO.getChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

            // Vérifier que l'utilisateur est un participant du canal
            Participant sender = participantRepository.findByUserIdAndChannel(userId, channel)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

            // Valider le message
            if (messageDTO.getText() == null || messageDTO.getText().isEmpty()) {
                logger.error("Le contenu du message est null ou vide");
                throw new IllegalArgumentException("Le contenu du message ne peut pas être vide");
            }

            // Créer et sauvegarder le message
            Message message = messageMapper.toEntity(messageDTO, channel, sender);
            Message savedMessage = messageRepository.save(message);
            messageRepository.flush(); // Forcer la persistance immédiate
            logger.info("Message sauvegardé avec id: {}", savedMessage.getId());

            // Convertir en DTO et enrichir avec les détails de l'utilisateur
            MessageDTO savedMessageDTO = messageMapper.toDTO(savedMessage);
            savedMessageDTO = enrichMessageDTO(savedMessageDTO, token);

            // Diffuser le message via WebSocket
            messagingTemplate.convertAndSend("/topic/messages." + messageDTO.getChannelId(), savedMessageDTO);
            logger.info("Message diffusé vers /topic/messages.{}", messageDTO.getChannelId());

            return savedMessageDTO;
        } catch (Exception e) {
            logger.error("Échec de l'envoi du message: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi du message", e);
        }
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessagesByChannelId(Long channelId, String token) {
        try {
            logger.info("Récupération des messages pour channelId: {}", channelId);

            // Récupérer l'utilisateur à partir du token
            String userId = authClient.decodeToken(token);
            if (userId == null) {
                logger.error("Échec du décodage du token: userId est null");
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            // Récupérer le canal
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

            // Vérifier que l'utilisateur est un participant du canal
            participantRepository.findByUserIdAndChannel(userId, channel)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

            // Récupérer les messages (tri décroissant pour avoir les nouveaux en haut)
            List<Message> messages = messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);
            logger.info("Récupéré {} messages pour channelId: {}", messages.size(), channelId);

            // Convertir en DTO et enrichir avec les détails de l'utilisateur
            return messages.stream()
                    .map(messageMapper::toDTO)
                    .map(dto -> enrichMessageDTO(dto, token))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Échec de la récupération des messages: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la récupération des messages", e);
        }
    }

    @Transactional
    public MessageDTO updateMessage(Long channelId, Long messageId, MessageDTO messageDTO, String token) {
        try {
            logger.info("Mise à jour du message {} pour channelId: {}", messageId, channelId);

            // Récupérer l'utilisateur à partir du token
            String userId = authClient.decodeToken(token);
            if (userId == null) {
                logger.error("Échec du décodage du token: userId est null");
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            // Récupérer le canal
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

            // Vérifier que l'utilisateur est un participant du canal
            Participant sender = participantRepository.findByUserIdAndChannel(userId, channel)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

            // Récupérer le message
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message non trouvé"));

            // Vérifier que l'utilisateur est l'auteur du message
            if (!message.getSender().getUserId().equals(userId)) {
                logger.error("Utilisateur {} n'est pas autorisé à modifier le message {}", userId, messageId);
                throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce message");
            }

            // Valider le contenu mis à jour
            if (messageDTO.getText() == null || messageDTO.getText().isEmpty()) {
                logger.error("Le contenu du message mis à jour est null ou vide");
                throw new IllegalArgumentException("Le contenu du message ne peut pas être vide");
            }

            // Mettre à jour le message
            message.getContent().setText(messageDTO.getText());
            if (messageDTO.getFileUrl() != null) {
                message.getContent().setFileUrl(messageDTO.getFileUrl());
            }
            if (messageDTO.getMimeType() != null) {
                message.getContent().setMimeType(messageDTO.getMimeType());
            }
            message.setType(messageDTO.getType() != null ? messageDTO.getType() : message.getType());
            Message updatedMessage = messageRepository.save(message);
            messageRepository.flush();
            logger.info("Message {} mis à jour avec succès", messageId);

            // Convertir en DTO et enrichir avec les détails de l'utilisateur
            MessageDTO updatedMessageDTO = messageMapper.toDTO(updatedMessage);
            updatedMessageDTO = enrichMessageDTO(updatedMessageDTO, token);

            // Diffuser la mise à jour via WebSocket
            messagingTemplate.convertAndSend("/topic/messages." + channelId, updatedMessageDTO);
            logger.info("Mise à jour du message diffusée vers /topic/messages.{}", channelId);

            return updatedMessageDTO;
        } catch (Exception e) {
            logger.error("Échec de la mise à jour du message: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la mise à jour du message", e);
        }
    }

    @Transactional
    public void deleteMessage(Long channelId, Long messageId, String token) {
        try {
            logger.info("Suppression du message {} pour channelId: {}", messageId, channelId);

            // Récupérer l'utilisateur à partir du token
            String userId = authClient.decodeToken(token);
            if (userId == null) {
                logger.error("Échec du décodage du token: userId est null");
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            // Récupérer le canal
            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

            // Vérifier que l'utilisateur est un participant du canal
            Participant sender = participantRepository.findByUserIdAndChannel(userId, channel)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

            // Récupérer le message
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message non trouvé"));

            // Vérifier que l'utilisateur est l'auteur du message
            if (!message.getSender().getUserId().equals(userId)) {
                logger.error("Utilisateur {} n'est pas autorisé à supprimer le message {}", userId, messageId);
                throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer ce message");
            }

            // Supprimer le message
            messageRepository.delete(message);
            messageRepository.flush();
            logger.info("Message {} supprimé avec succès", messageId);

            // Notifier les clients via WebSocket
            MessageDTO deletedMessageDTO = new MessageDTO();
            deletedMessageDTO.setId(messageId);
            deletedMessageDTO.setChannelId(channelId);
            deletedMessageDTO.setType(com.collaboration.collaborationservice.common.enums.MessageType.SYSTEM);
            deletedMessageDTO.setText("Message supprimé");
            messagingTemplate.convertAndSend("/topic/messages." + channelId, deletedMessageDTO);
            logger.info("Suppression du message diffusée vers /topic/messages.{}", channelId);
        } catch (Exception e) {
            logger.error("Échec de la suppression du message: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la suppression du message", e);
        }
    }
}