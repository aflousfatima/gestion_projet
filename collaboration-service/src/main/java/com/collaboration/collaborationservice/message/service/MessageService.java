package com.collaboration.collaborationservice.message.service;

import com.collaboration.collaborationservice.channel.config.AuthClient;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.repository.ChannelRepository;
import com.collaboration.collaborationservice.common.enums.MessageType;
import com.collaboration.collaborationservice.common.valueobjects.MessageContent;
import com.collaboration.collaborationservice.message.dto.MessageDTO;
import com.collaboration.collaborationservice.message.dto.ReactionDTO;
import com.collaboration.collaborationservice.message.entity.Message;
import com.collaboration.collaborationservice.message.entity.Reaction;
import com.collaboration.collaborationservice.message.mapper.MessageMapper;
import com.collaboration.collaborationservice.message.repository.MessageRepository;
import com.collaboration.collaborationservice.message.repository.ReactionRepository;
import com.collaboration.collaborationservice.participant.entity.Participant;
import com.collaboration.collaborationservice.participant.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private AuthClient authClient;

    private final Map<String, String> userNameCache = new ConcurrentHashMap<>();

    private MessageDTO enrichMessageDTO(MessageDTO messageDTO, String token) {
        try {
            String senderName = userNameCache.get(messageDTO.getSenderId());
            if (senderName == null) {
                Map<String, Object> userDetails = authClient.getUserDetailsByAuthId(messageDTO.getSenderId(), token);
                String firstName = (String) userDetails.get("firstName");
                String lastName = (String) userDetails.get("lastName");
                senderName = firstName + " " + lastName;
                userNameCache.put(messageDTO.getSenderId(), senderName);
            }
            messageDTO.setSenderName(senderName);

            if (messageDTO.getReplyToId() != null) {
                String replyToSenderName = userNameCache.get(messageDTO.getReplyToSenderName());
                if (replyToSenderName == null) {
                    Map<String, Object> replyToUserDetails = authClient.getUserDetailsByAuthId(
                            messageDTO.getReplyToSenderName(), token);
                    String replyToFirstName = (String) replyToUserDetails.get("firstName");
                    String replyToLastName = (String) replyToUserDetails.get("lastName");
                    replyToSenderName = replyToFirstName + " " + replyToLastName;
                    userNameCache.put(messageDTO.getReplyToSenderName(), replyToSenderName);
                }
                messageDTO.setReplyToSenderName(replyToSenderName);
            }
        } catch (Exception e) {
            logger.warn("Impossible de récupérer les détails de l'utilisateur: {}", e.getMessage());
            messageDTO.setSenderName("Utilisateur Inconnu");
            if (messageDTO.getReplyToId() != null) {
                messageDTO.setReplyToSenderName("Utilisateur Inconnu");
            }
        }
        return messageDTO;
    }

    @Transactional
    public MessageDTO sendMessage(MessageDTO messageDTO, String token) {
        try {
            logger.info("Envoi d'un message pour channelId: {}", messageDTO.getChannelId());

            String userId = authClient.decodeToken(token);
            if (userId == null) {
                logger.error("Échec du décodage du token: userId est null");
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            Channel channel = channelRepository.findById(messageDTO.getChannelId())
                    .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

            Participant sender = participantRepository.findByUserIdAndChannel(userId, channel)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

            if (messageDTO.getText() == null || messageDTO.getText().isEmpty()) {
                logger.error("Le contenu du message est null ou vide");
                throw new IllegalArgumentException("Le contenu du message ne peut pas être vide");
            }

            Message message = messageMapper.toEntity(messageDTO, channel, sender);
            Message savedMessage = messageRepository.save(message);
            messageRepository.flush();
            logger.info("Message sauvegardé avec id: {}", savedMessage.getId());

            MessageDTO savedMessageDTO = messageMapper.toDTO(savedMessage);
            savedMessageDTO = enrichMessageDTO(savedMessageDTO, token);

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

            String userId = authClient.decodeToken(token);
            if (userId == null) {
                logger.error("Échec du décodage du token: userId est null");
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

            participantRepository.findByUserIdAndChannel(userId, channel)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

            List<Message> messages = messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);
            logger.info("Récupéré {} messages pour channelId: {}", messages.size(), channelId);

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

            String userId = authClient.decodeToken(token);
            if (userId == null) {
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

            Participant sender = participantRepository.findByUserIdAndChannel(userId, channel)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message non trouvé"));

            if (!message.getSender().getUserId().equals(userId)) {
                throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce message");
            }

            if (messageDTO.getText() == null || messageDTO.getText().isEmpty()) {
                throw new IllegalArgumentException("Le contenu du message ne peut pas être vide");
            }

            message.getContent().setText(messageDTO.getText());
            message.setModified(true);
            Message updatedMessage = messageRepository.save(message);
            messageRepository.flush();

            MessageDTO updatedMessageDTO = messageMapper.toDTO(updatedMessage);
            updatedMessageDTO = enrichMessageDTO(updatedMessageDTO, token);

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

            String userId = authClient.decodeToken(token);
            if (userId == null) {
                logger.error("Échec du décodage du token: userId est null");
                throw new IllegalArgumentException("Utilisateur non authentifié");
            }

            Channel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

            Participant sender = participantRepository.findByUserIdAndChannel(userId, channel)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message non trouvé"));

            if (!message.getSender().getUserId().equals(userId)) {
                logger.error("Utilisateur {} n'est pas autorisé à supprimer le message {}", userId, messageId);
                throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer ce message");
            }

            messageRepository.delete(message);
            messageRepository.flush();
            logger.info("Message {} supprimé avec succès", messageId);

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

    @Transactional
    public MessageDTO addReaction(Long channelId, Long messageId, ReactionDTO reactionDTO, String token) {
        logger.info("Ajout d'une réaction au message {} pour channelId: {}", messageId, channelId);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message non trouvé"));

        Reaction reaction = new Reaction();
        reaction.setMessage(message);
        reaction.setParticipant(participant);
        reaction.setEmoji(reactionDTO.getEmoji());
        reactionRepository.save(reaction);

        Message updatedMessage = messageRepository.findById(messageId).get();
        MessageDTO updatedMessageDTO = messageMapper.toDTO(updatedMessage);
        updatedMessageDTO = enrichMessageDTO(updatedMessageDTO, token);

        messagingTemplate.convertAndSend("/topic/messages." + channelId, updatedMessageDTO);
        return updatedMessageDTO;
    }

    @Transactional
    public MessageDTO removeReaction(Long channelId, Long messageId, ReactionDTO reactionDTO, String token) {
        logger.info("Suppression d'une réaction au message {} pour channelId: {}", messageId, channelId);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message non trouvé"));

        Reaction reaction = reactionRepository.findByMessageAndParticipantAndEmoji(
                message,
                participant,
                reactionDTO.getEmoji()
        ).orElseThrow(() -> new IllegalArgumentException("Réaction non trouvée"));

        reactionRepository.delete(reaction);

        Message updatedMessage = messageRepository.findById(messageId).get();
        MessageDTO updatedMessageDTO = messageMapper.toDTO(updatedMessage);
        updatedMessageDTO = enrichMessageDTO(updatedMessageDTO, token);

        messagingTemplate.convertAndSend("/topic/messages." + channelId, updatedMessageDTO);
        return updatedMessageDTO;
    }

    @Transactional
    public MessageDTO pinMessage(Long channelId, Long messageId, String token) {
        logger.info("Épinglage du message {} pour channelId: {}", messageId, channelId);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message non trouvé"));

        message.setPinned(!message.isPinned());
        Message updatedMessage = messageRepository.save(message);
        messageRepository.flush();

        MessageDTO updatedMessageDTO = messageMapper.toDTO(updatedMessage);
        updatedMessageDTO = enrichMessageDTO(updatedMessageDTO, token);

        messagingTemplate.convertAndSend("/topic/messages." + channelId, updatedMessageDTO);
        logger.info("Message épinglé/désépinglé diffusé vers /topic/messages.{}", channelId);
        return updatedMessageDTO;
    }

    @Transactional
    public MessageDTO uploadAudioMessage(Long channelId, MultipartFile audioFile, String token, Long replyToId, String duration) throws IOException {
        logger.info("Téléversement d'un message audio pour channelId: {}", channelId);
        logger.info("Valeur de duration reçue: {}", duration);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        String fileUrl = null;
        String publicId = null;

        if (audioFile != null) {
            Map uploadResult = cloudinaryService.uploadAudio(audioFile);
            fileUrl = (String) uploadResult.get("secure_url");
            publicId = (String) uploadResult.get("public_id");
        }

        Message message = new Message();
        message.setChannel(channel);
        message.setSender(participant);
        MessageContent content = new MessageContent();
        content.setFileUrl(fileUrl);
        content.setMimeType(audioFile != null ? audioFile.getContentType() : "audio/webm");
        content.setDuration(duration != null ? Long.parseLong(duration) : null);
        logger.info("Durée stockée dans MessageContent: {}", content.getDuration());
        message.setContent(content);
        message.setType(MessageType.AUDIO);
        message.setCreatedAt(LocalDateTime.now());
        message.setPinned(false);
        message.setModified(false);

        if (replyToId != null) {
            Message replyTo = messageRepository.findById(replyToId)
                    .orElseThrow(() -> new IllegalArgumentException("Message cité non trouvé"));
            message.setReplyTo(replyTo);
        }

        Message savedMessage = messageRepository.save(message);
        messageRepository.flush();

        MessageDTO messageDTO = messageMapper.toDTO(savedMessage);
        messageDTO.setDuration(content.getDuration());
        logger.info("Durée dans MessageDTO: {}", messageDTO.getDuration());
        messageDTO = enrichMessageDTO(messageDTO, token);

        messagingTemplate.convertAndSend("/topic/messages." + channelId, messageDTO);
        logger.info("Message audio diffusé vers /topic/messages.{}", channelId);

        return messageDTO;
    }

    @Transactional
    public MessageDTO uploadImageMessage(Long channelId, MultipartFile imageFile, String token, Long replyToId) throws IOException {
        logger.info("Téléversement d'une image pour channelId: {}", channelId);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        String fileUrl = null;
        String publicId = null;

        if (imageFile != null) {
            Map uploadResult = cloudinaryService.uploadImage(imageFile);
            fileUrl = (String) uploadResult.get("secure_url");
            publicId = (String) uploadResult.get("public_id");
        }

        Message message = new Message();
        message.setChannel(channel);
        message.setSender(participant);
        MessageContent content = new MessageContent();
        content.setFileUrl(fileUrl);
        content.setMimeType(imageFile != null ? imageFile.getContentType() : "image/jpeg");
        message.setContent(content);
        message.setType(MessageType.IMAGE);
        message.setCreatedAt(LocalDateTime.now());
        message.setPinned(false);
        message.setModified(false);

        if (replyToId != null) {
            Message replyTo = messageRepository.findById(replyToId)
                    .orElseThrow(() -> new IllegalArgumentException("Message cité non trouvé"));
            message.setReplyTo(replyTo);
        }

        Message savedMessage = messageRepository.save(message);
        messageRepository.flush();

        MessageDTO messageDTO = messageMapper.toDTO(savedMessage);
        messageDTO = enrichMessageDTO(messageDTO, token);

        messagingTemplate.convertAndSend("/topic/messages." + channelId, messageDTO);
        logger.info("Message image diffusé vers /topic/messages.{}", channelId);

        return messageDTO;
    }

    @Transactional
    public MessageDTO uploadFileMessage(Long channelId, MultipartFile file, String token, Long replyToId) throws IOException {
        logger.info("Téléversement d'un fichier pour channelId: {}", channelId);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        String fileUrl = null;
        String publicId = null;

        if (file != null) {
            Map uploadResult = cloudinaryService.uploadFile(file);
            fileUrl = (String) uploadResult.get("secure_url");
            publicId = (String) uploadResult.get("public_id");
        }

        Message message = new Message();
        message.setChannel(channel);
        message.setSender(participant);
        MessageContent content = new MessageContent();
        content.setFileUrl(fileUrl);
        content.setMimeType(file != null ? file.getContentType() : "application/octet-stream");
        content.setText(file != null ? file.getOriginalFilename() : "Fichier sans nom"); // Set file name
        message.setContent(content);
        message.setType(MessageType.FILE);
        message.setCreatedAt(LocalDateTime.now());
        message.setPinned(false);
        message.setModified(false);

        if (replyToId != null) {
            Message replyTo = messageRepository.findById(replyToId)
                    .orElseThrow(() -> new IllegalArgumentException("Message cité non trouvé"));
            message.setReplyTo(replyTo);
        }

        Message savedMessage = messageRepository.save(message);
        messageRepository.flush();

        MessageDTO messageDTO = messageMapper.toDTO(savedMessage);
        messageDTO = enrichMessageDTO(messageDTO, token);

        messagingTemplate.convertAndSend("/topic/messages." + channelId, messageDTO);
        logger.info("Message fichier diffusé vers /topic/messages.{}", channelId);

        return messageDTO;
    }
}