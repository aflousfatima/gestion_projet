package com.collaboration.collaborationservice.call.service;

import com.collaboration.collaborationservice.call.dto.CallDTO;
import com.collaboration.collaborationservice.call.entity.Call;
import com.collaboration.collaborationservice.call.repository.CallRepository;
import com.collaboration.collaborationservice.call.repository.CallSessionRepository;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.repository.ChannelRepository;
import com.collaboration.collaborationservice.channel.config.AuthClient;
import com.collaboration.collaborationservice.common.enums.CallStatus;
import com.collaboration.collaborationservice.common.enums.CallType;
import com.collaboration.collaborationservice.common.valueobjects.SdpInfo;
import com.collaboration.collaborationservice.participant.entity.Participant;
import com.collaboration.collaborationservice.participant.repository.ParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CallService {

    private static final Logger logger = LoggerFactory.getLogger(CallService.class);

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private CallSessionRepository callSessionRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AuthClient authClient;

    @Transactional
    public CallDTO initiateCall(Long channelId, CallType callType, String token) {
        logger.info("Initiation d'un appel de {} pour channelId: {}", callType, channelId);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            logger.error("Échec du décodage du token: userId est null");
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }
        logger.debug("Utilisateur authentifié: userId={}", userId);

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Canal non trouvé"));
        logger.debug("Canal trouvé: channelId={}", channelId);

        Participant initiator = participantRepository.findByUserIdAndChannel(userId, channel)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));
        logger.debug("Initiateur trouvé: userId={}", userId);

        Call call = new Call();
        call.setChannel(channel);
        call.setInitiator(initiator);
        call.setStatus(CallStatus.INITIATED);
        call.setType(callType);
        call.setStartedAt(LocalDateTime.now());
        call.setParticipants(List.of(initiator));

        Call savedCall = callRepository.save(call);
        callRepository.flush();
        logger.debug("Appel sauvegardé: callId={}", savedCall.getId());

        // Notifier les participants via WebSocket
        messagingTemplate.convertAndSend("/topic/calls." + channelId, convertToDto(savedCall));
        logger.debug("Notification envoyée via WebSocket pour channelId={}", channelId);

        return convertToDto(savedCall);
    }

    @Transactional
    public CallDTO joinCall(Long callId, String token) {
        logger.info("Utilisateur rejoint l'appel {}", callId);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            logger.error("Échec du décodage du token: userId est null");
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Appel non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, call.getChannel())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        if (!call.getParticipants().contains(participant)) {
            call.getParticipants().add(participant);
            call.setStatus(CallStatus.ACTIVE);
            callRepository.save(call);
            callRepository.flush();
        }

        CallDTO callDto = convertToDto(call);
        messagingTemplate.convertAndSend("/topic/calls." + call.getChannel().getId(), callDto);
        return callDto;
    }

    @Transactional
    public void endCall(Long callId, String token) {
        logger.info("Utilisateur termine l'appel {}", callId);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            logger.error("Échec du décodage du token: userId est null");
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Appel non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, call.getChannel())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        call.setStatus(CallStatus.ENDED);
        call.setEndedAt(LocalDateTime.now());
        callRepository.save(call);
        callRepository.flush();

        messagingTemplate.convertAndSend("/topic/calls." + call.getChannel().getId(), convertToDto(call));
    }

    @Transactional
    public void handleSignaling(Long callId, SdpInfo sdpInfo, String type, String token) {
        logger.info("Traitement de la signalisation pour callId: {}, type: {}", callId, type);

        String userId = authClient.decodeToken(token);
        if (userId == null) {
            logger.error("Échec du décodage du token: userId est null");
            throw new IllegalArgumentException("Utilisateur non authentifié");
        }

        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new IllegalArgumentException("Appel non trouvé"));

        Participant participant = participantRepository.findByUserIdAndChannel(userId, call.getChannel())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non autorisé dans ce canal"));

        messagingTemplate.convertAndSend("/topic/signaling." + callId, new SignalingMessage(userId, sdpInfo, type));
    }

    private CallDTO convertToDto(Call call) {
        CallDTO dto = new CallDTO();
        dto.setId(call.getId());
        dto.setChannelId(call.getChannel().getId());
        dto.setChannelName(call.getChannel().getName());
        dto.setInitiatorId(call.getInitiator().getId());
        dto.setStatus(call.getStatus());
        dto.setType(call.getType());
        dto.setStartedAt(call.getStartedAt());
        dto.setEndedAt(call.getEndedAt());
        dto.setParticipantIds(call.getParticipants().stream()
                .map(Participant::getId)
                .collect(Collectors.toList()));
        return dto;
    }
}