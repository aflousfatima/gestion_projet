package com.collaboration.collaborationservice.participant.controller;

import com.collaboration.collaborationservice.participant.Service.ParticipantService;
import com.collaboration.collaborationservice.participant.dto.AddParticipantRequest;
import com.collaboration.collaborationservice.participant.dto.ParticipantDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels/{channelId}/participants")
public class ParticipantController {

    @Autowired
    private ParticipantService participantService;

    /** Récupérer la liste des participants */
    @GetMapping
    public ResponseEntity<List<ParticipantDTO>> getParticipants(
            @PathVariable Long channelId,
            @RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.replace("Bearer ", "");
        List<ParticipantDTO> participants = participantService.getParticipantsByChannelId(channelId, accessToken);
        return ResponseEntity.ok(participants);
    }

    /** Ajouter un participant */
    @PostMapping
    public ResponseEntity<Void> addParticipant(
            @PathVariable Long channelId,
            @RequestBody AddParticipantRequest request,
            @RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.replace("Bearer ", "");
        participantService.addParticipant(channelId, request, accessToken);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Retirer un participant */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long channelId,
            @PathVariable String userId) {
        participantService.removeParticipant(channelId, userId);
        return ResponseEntity.noContent().build();
    }
}
