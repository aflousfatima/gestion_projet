package com.collaboration.collaborationservice.channel.service;

import com.collaboration.collaborationservice.channel.config.AuthClient;
import com.collaboration.collaborationservice.channel.controller.ChannelController;
import com.collaboration.collaborationservice.channel.dto.ChannelDTO;
import com.collaboration.collaborationservice.channel.dto.CreateChannelRequest;
import com.collaboration.collaborationservice.channel.dto.UpdateChannelRequest;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.mapper.ChannelMapper;
import com.collaboration.collaborationservice.channel.repository.ChannelRepository;
import com.collaboration.collaborationservice.common.enums.ChannelType;
import com.collaboration.collaborationservice.common.enums.Role;
import com.collaboration.collaborationservice.participant.dto.ParticipantDTO;
import com.collaboration.collaborationservice.participant.entity.Participant;
import com.collaboration.collaborationservice.participant.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChannelService {
    private static final Logger log = LoggerFactory.getLogger(ChannelController.class);

    private final ChannelRepository channelRepository;


    private final ParticipantRepository participantRepository;
    private final AuthClient authClient;
    private final ChannelMapper channelMapper;

    @Autowired
    public ChannelService(
            ChannelRepository channelRepository,
            ParticipantRepository participantRepository,
            AuthClient authClient,
            ChannelMapper channelMapper
           ) {
        this.channelRepository = channelRepository;
        this.participantRepository = participantRepository;
        this.authClient = authClient;
        this.channelMapper = channelMapper;
    }


    @Transactional
    public Channel createChannel(CreateChannelRequest request, String authorizationHeader) {
        Logger log = LoggerFactory.getLogger(ChannelService.class);

        String userId = authClient.decodeToken(authorizationHeader);
        if (userId == null) {
            log.error("Échec du décodage du token: userId est null");
            throw new IllegalArgumentException("Impossible de récupérer l'ID de l'utilisateur à partir du token");
        }
        log.info("Création du canal par l'utilisateur: {}", userId);

        // Créer le canal
        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setType(request.getType());
        channel.setPrivate(request.getIsPrivate());
        channel.setDescription(request.getDescription());
        channel.setProjectId(request.getProjectId());
        channel.setCreatedBy(userId);

        // Sauvegarder le canal
        try {
            channel = channelRepository.save(channel);
            log.info("Canal sauvegardé avec ID: {}", channel.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du canal: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la sauvegarde du canal", e);
        }

        List<Participant> participants = new ArrayList<>();

        // Ajouter le créateur comme participant avec le rôle ADMIN
        Participant creatorParticipant = new Participant();
        creatorParticipant.setUserId(userId);
        creatorParticipant.setRole(Role.ADMIN);
        creatorParticipant.setChannel(channel);
        try {
            participants.add(participantRepository.save(creatorParticipant));
            log.info("Créateur {} ajouté comme participant avec le rôle ADMIN", userId);
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde du créateur comme participant: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la sauvegarde du créateur comme participant", e);
        }

        // Ajouter les autres participants depuis la requête
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            List<String> roles = request.getRoles();
            if (roles == null || roles.size() != request.getParticipantIds().size()) {
                log.error("La liste des rôles ({}) ne correspond pas à la liste des participants ({})",
                        roles == null ? 0 : roles.size(), request.getParticipantIds().size());
                throw new IllegalArgumentException("La liste des rôles doit correspondre à la liste des participants");
            }

            for (int i = 0; i < request.getParticipantIds().size(); i++) {
                String participantUserId = request.getParticipantIds().get(i);
                String roleStr = roles.get(i);

                // Ignorer si le participant est le créateur (déjà ajouté)
                if (participantUserId.equals(userId)) {
                    log.info("Participant {} est le créateur, ignoré car déjà ajouté", participantUserId);
                    continue;
                }

                // Convertir le rôle string en enum Role
                Role role;
                try {
                    role = Role.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.error("Rôle invalide: {}", roleStr);
                    throw new IllegalArgumentException("Rôle invalide : " + roleStr);
                }

                // Créer un nouveau participant
                Participant participant = new Participant();
                participant.setUserId(participantUserId);
                participant.setRole(role);
                participant.setChannel(channel);

                // Sauvegarder le participant
                try {
                    participants.add(participantRepository.save(participant));
                    log.info("Participant {} ajouté avec le rôle {}", participantUserId, role);
                } catch (Exception e) {
                    log.error("Erreur lors de la sauvegarde du participant {}: {}", participantUserId, e.getMessage(), e);
                    throw new RuntimeException("Échec de la sauvegarde du participant: " + participantUserId, e);
                }
            }
        }

        // Associer les participants au canal
        channel.setParticipants(participants);

        // Sauvegarder le canal avec les participants
        try {
            channel = channelRepository.save(channel);
            log.info("Canal final sauvegardé avec {} participants", participants.size());
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde finale du canal: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la sauvegarde finale du canal", e);
        }

        return channel;
    }
    @Transactional(readOnly = true)
    public List<Channel> getAccessibleChannels(String authorizationHeader) {
        // Décoder le token pour obtenir l'ID de l'utilisateur
        String userId = authClient.decodeToken(authorizationHeader);
        if (userId == null) {
            throw new IllegalArgumentException("Impossible de récupérer l'ID de l'utilisateur à partir du token");
        }

        // Récupérer les canaux accessibles (publics ou privés où l'utilisateur est participant)
        return channelRepository.findAccessibleChannelsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ChannelDTO> getAllPublicChannels() {
        List<Channel> channels = channelRepository.findAllPublicChannels();
        return channelMapper.toDTOList(channels);
    }


    @Transactional(readOnly = true)
    public ChannelDTO getChannelById(Long id, String authorizationHeader) throws IllegalAccessException {
        String userId = authClient.decodeToken(authorizationHeader);
        if (userId == null) {
            throw new IllegalArgumentException("Impossible de récupérer l'ID de l'utilisateur à partir du token");
        }

        Optional<Channel> channelOpt = channelRepository.findById(id);
        if (channelOpt.isEmpty()) {
            throw new IllegalArgumentException("Canal non trouvé");
        }
        Channel channel = channelOpt.get();

        // Vérifier si l'utilisateur a accès (public ou participant)
        if (channel.isPrivate() && !channel.getParticipants().stream().anyMatch(p -> p.getUserId().equals(userId))) {
            throw new IllegalAccessException("Accès non autorisé à ce canal");
        }

        return channelMapper.toDTO(channel);
    }
    @Transactional
    public Channel updateChannel(Long id, UpdateChannelRequest request, String authorizationHeader) throws IllegalAccessException {
        String userId = authClient.decodeToken(authorizationHeader);
        if (userId == null) {
            throw new IllegalArgumentException("Impossible de récupérer l'ID de l'utilisateur à partir du token");
        }

        Optional<Channel> channelOpt = channelRepository.findById(id);
        if (channelOpt.isEmpty()) {
            throw new IllegalArgumentException("Canal non trouvé");
        }
        Channel channel = channelOpt.get();

        // Vérifier si l'utilisateur est le créateur
        if (!channel.getCreatedBy().equals(userId)) {
            throw new IllegalAccessException("Seul le créateur peut modifier ce canal");
        }

        // Mettre à jour les champs du canal
        channel.setName(request.getName());
        channel.setType(request.getType());
        channel.setPrivate(request.getIsPrivate());
        channel.setDescription(request.getDescription());
        channel.setProjectId(request.getProjectId());

        // Mettre à jour les participants
        if (request.getParticipantIds() != null) {
            // Supprimer les anciens participants
            participantRepository.deleteByChannelId(id);
            channel.getParticipants().clear();

            // Ajouter les nouveaux participants
            if (!request.getParticipantIds().isEmpty()) {
                List<Participant> participants = new ArrayList<>();
                for (String participantUserId : request.getParticipantIds()) {
                    Participant participant = participantRepository.findByUserId(participantUserId)
                            .orElseGet(() -> {
                                Participant newParticipant = new Participant();
                                newParticipant.setUserId(participantUserId);
                                newParticipant.setRole(Role.MEMBER); // Default role
                                newParticipant.setJoinedAt(LocalDateTime.now());
                                return newParticipant;
                            });
                    participant.setChannel(channel);
                    participants.add(participant);
                }
                participantRepository.saveAll(participants);
                channel.setParticipants(participants);
            }
        }

        return channelRepository.save(channel);
    }
    @Transactional
    public void deleteChannel(Long id, String authorizationHeader) throws IllegalAccessException {
        String userId = authClient.decodeToken(authorizationHeader);
        if (userId == null) {
            throw new IllegalArgumentException("Impossible de récupérer l'ID de l'utilisateur à partir du token");
        }

        Optional<Channel> channelOpt = channelRepository.findById(id);
        if (channelOpt.isEmpty()) {
            throw new IllegalArgumentException("Canal non trouvé");
        }
        Channel channel = channelOpt.get();

        // Vérifier si l'utilisateur est le créateur
        if (!channel.getCreatedBy().equals(userId)) {
            throw new IllegalAccessException("Seul le créateur peut supprimer ce canal");
        }

        channelRepository.delete(channel);
    }

    @Transactional(readOnly = true)
    public List<Channel> getChannelsByProjectId(Long projectId) {
        return channelRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<Channel> getChannelsByType(ChannelType type) {
        return channelRepository.findByType(type);
    }

    @Transactional(readOnly = true)
    public List<Channel> getChannelsCreatedByUser(String userId) {
        return channelRepository.findByCreatedBy(userId);
    }

    @Transactional
    public Channel updateChannelVisibility(Long id, boolean isPrivate, String authorizationHeader) throws IllegalAccessException {
        String userId = authClient.decodeToken(authorizationHeader);
        if (userId == null) {
            throw new IllegalArgumentException("Impossible de récupérer l'ID de l'utilisateur à partir du token");
        }

        Optional<Channel> channelOpt = channelRepository.findById(id);
        if (channelOpt.isEmpty()) {
            throw new IllegalArgumentException("Canal non trouvé");
        }
        Channel channel = channelOpt.get();

        // Vérifier si l'utilisateur est le créateur
        if (!channel.getCreatedBy().equals(userId)) {
            throw new IllegalAccessException("Seul le créateur peut modifier la visibilité de ce canal");
        }

        channel.setPrivate(isPrivate);
        return channelRepository.save(channel);
    }

}