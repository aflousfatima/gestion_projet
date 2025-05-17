package com.collaboration.collaborationservice.channel.service;

import com.collaboration.collaborationservice.channel.config.AuthClient;
import com.collaboration.collaborationservice.channel.dto.CreateChannelRequest;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.repository.ChannelRepository;
import com.collaboration.collaborationservice.participant.entity.Participant;
import com.collaboration.collaborationservice.participant.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        // Décoder le token pour obtenir l'ID de l'utilisateur
        String userId  = authClient.decodeToken(authorizationHeader);
        if (userId == null) {
            throw new IllegalArgumentException("Impossible de récupérer l'ID de l'utilisateur à partir du token");
        }

        // Créer une nouvelle instance de Channel
        Channel channel = new Channel();
        channel.setName(request.getName());
        channel.setType(request.getType());
        channel.setPrivate(request.getIsPrivate());
        channel.setDescription(request.getDescription());
        channel.setProjectId(request.getProjectId());
        channel.setCreatedBy(userId); // Assigner l'ID de l'utilisateur créateur

        // Sauvegarder le canal
        channel = channelRepository.save(channel);

        // Ajouter les participants si fournis
        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            List<Participant> participants = participantRepository.findAllById(request.getParticipantIds());
            if (participants.size() != request.getParticipantIds().size()) {
                throw new IllegalArgumentException("Certains participants spécifiés n'existent pas");
            }
            for (Participant participant : participants) {
                participant.setChannel(channel);
                participantRepository.save(participant);
            }
            channel.setParticipants(participants);
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
    public List<Channel> getAllPublicChannels() {
        // Récupérer tous les canaux publics
        return channelRepository.findAllPublicChannels();
    }
}