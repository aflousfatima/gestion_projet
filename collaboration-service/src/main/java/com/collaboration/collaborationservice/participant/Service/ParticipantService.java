package com.collaboration.collaborationservice.participant.Service;

import com.collaboration.collaborationservice.channel.config.AuthClient;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.common.enums.Role;
import com.collaboration.collaborationservice.participant.dto.AddParticipantRequest;
import com.collaboration.collaborationservice.participant.dto.ParticipantDTO;
import com.collaboration.collaborationservice.participant.entity.Participant;
import com.collaboration.collaborationservice.participant.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParticipantService {

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private UserStatusService userStatusService;

    public List<ParticipantDTO> getParticipantsByChannelId(Long channelId, String accessToken) {
        List<Participant> participants = participantRepository.findByChannelId(channelId);
        return participants.stream().map(participant -> {
            Map<String, Object> userDetails = authClient.getUserDetailsByAuthId(
                    participant.getUserId(),
                    "Bearer " + accessToken
            );
            ParticipantDTO dto = new ParticipantDTO();
            dto.setId(participant.getId());
            dto.setUserId(participant.getUserId());
            dto.setFirstName((String) userDetails.get("firstName"));
            dto.setLastName((String) userDetails.get("lastName"));
            dto.setRole(participant.getRole().name());
            dto.setJoinedAt(participant.getJoinedAt());
            dto.setStatus(userStatusService.getUserStatus(participant.getUserId()));
            return dto;
        }).collect(Collectors.toList());
    }


    /** Ajouter un participant à un channel */
    public void addParticipant(Long channelId, AddParticipantRequest request, String accessToken) {
        // Vérifier si l'utilisateur existe via Feign
        Map<String, Object> userDetails = authClient.getUserDetailsByAuthId(
                request.getUserId(),
                "Bearer " + accessToken
        );
        if (userDetails == null || userDetails.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        Participant participant = new Participant();
        participant.setChannel(new Channel()); // On suppose que Channel a un constructeur avec l'ID
        participant.setUserId(request.getUserId());
        participant.setRole(Role.valueOf(request.getRole()));
        participant.setJoinedAt(LocalDateTime.now());
        participant.setHost(false); // Par défaut
        participant.setMuted(false); // Par défaut
        participant.setVideoEnabled(false); // Par défaut
        participantRepository.save(participant);
    }

    /** Retirer un participant d'un channel */
    public void removeParticipant(Long channelId, String userId) {
        Participant participant = participantRepository.findByChannelIdAndUserId(channelId, userId);
        if (participant != null) {
            participantRepository.delete(participant);
        } else {
            throw new RuntimeException("Participant non trouvé");
        }
    }
}