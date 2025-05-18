package com.collaboration.collaborationservice.channel.service;

import com.collaboration.collaborationservice.channel.config.AuthClient;
import com.collaboration.collaborationservice.channel.dto.CreateChannelRequest;
import com.collaboration.collaborationservice.channel.dto.UpdateChannelRequest;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.repository.ChannelRepository;
import com.collaboration.collaborationservice.common.enums.ChannelType;
import com.collaboration.collaborationservice.common.enums.Role;
import com.collaboration.collaborationservice.participant.entity.Participant;
import com.collaboration.collaborationservice.participant.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ParticipantRepository participantRepository;
    private final AuthClient authClient;

    @Autowired
    public ChannelService(
            ChannelRepository channelRepository,
            ParticipantRepository participantRepository,
            AuthClient authClient) {
        this.channelRepository = channelRepository;
        this.participantRepository = participantRepository;
        this.authClient = authClient;
    }

    @Transactional
    public Channel createChannel(CreateChannelRequest request, String authorizationHeader) {
        String userId = authClient.decodeToken(authorizationHeader);
        if (userId == null) {
            throw new IllegalArgumentException("Impossible de récupérer l'ID de l'utilisateur à partir du token");
        }

        // Créer le canal
        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setType(request.getType());
        channel.setPrivate(request.getIsPrivate());
        channel.setDescription(request.getDescription());
        channel.setProjectId(request.getProjectId());
        channel.setCreatedBy(userId);

        // Sauvegarder le canal
        channel = channelRepository.save(channel);

        List<Participant> participants = new ArrayList<>();

        // Ajouter le créateur comme participant avec le rôle ADMIN
        Participant creatorParticipant = new Participant();
        creatorParticipant.setUserId(userId);
        creatorParticipant.setRole(Role.ADMIN);
        creatorParticipant.setChannel(channel);
        participants.add(participantRepository.save(creatorParticipant));

        // Ajouter les autres participants depuis la requête
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            // Vérifier que la liste des rôles correspond à la liste des participants
            List<String> roles = request.getRoles();
            if (roles == null || roles.size() != request.getParticipantIds().size()) {
                throw new IllegalArgumentException("La liste des rôles doit correspondre à la liste des participants");
            }

            // Pour chaque userId, créer un nouveau Participant
            for (int i = 0; i < request.getParticipantIds().size(); i++) {
                String participantUserId = request.getParticipantIds().get(i);
                String roleStr = roles.get(i);

                // Ignorer si le participant est le créateur (déjà ajouté)
                if (participantUserId.equals(userId)) {
                    continue;
                }

                // Convertir le rôle string en enum Role
                Role role;
                try {
                    role = Role.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Rôle invalide : " + roleStr);
                }

                // Créer un nouveau participant
                Participant participant = new Participant();
                participant.setUserId(participantUserId);
                participant.setRole(role);
                participant.setChannel(channel);

                // Sauvegarder le participant
                participants.add(participantRepository.save(participant));
            }
        }

        // Associer les participants au canal
        channel.setParticipants(participants);

        // Sauvegarder le canal avec les participants
        return channelRepository.save(channel);
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
    public List<Channel> getAllPublicChannels() {
        // Récupérer tous les canaux publics
        return channelRepository.findAllPublicChannels();
    }


    @Transactional(readOnly = true)
    public Channel getChannelById(Long id, String authorizationHeader) throws IllegalAccessException {
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
        if (channel.isPrivate() && !channel.getParticipants().stream().anyMatch(p -> p.getId().toString().equals(userId))) {
            throw new IllegalAccessException("Accès non autorisé à ce canal");
        }

        return channel;
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