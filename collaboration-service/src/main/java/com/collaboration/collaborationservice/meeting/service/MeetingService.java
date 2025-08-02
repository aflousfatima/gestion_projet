package com.collaboration.collaborationservice.meeting.service;


import com.collaboration.collaborationservice.channel.config.AuthClient;
import com.collaboration.collaborationservice.meeting.dto.MeetingDTO;
import com.collaboration.collaborationservice.meeting.entity.Meeting;
import com.collaboration.collaborationservice.meeting.enums.MeetingStatus;
import com.collaboration.collaborationservice.meeting.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private AuthClient authClient;

    public MeetingDTO createMeeting(MeetingDTO meetingDTO, String authorizationHeader) {
        String creatorId = authClient.decodeToken(authorizationHeader);
        Meeting meeting = new Meeting(
                meetingDTO.getTitle(),
                meetingDTO.getDate(),
                meetingDTO.getTime(),
                meetingDTO.getDuration(),
                meetingDTO.getStatus() != null ? meetingDTO.getStatus() : MeetingStatus.UPCOMING,
                meetingDTO.getParticipantIds(),
                creatorId,
                meetingDTO.getProject(),
                meetingDTO.getMeetingType(),
                meetingDTO.getMeetingPriority(),
                meetingDTO.getParticipantCount(),
                meetingDTO.getTimezone()
        );
        Meeting savedMeeting = meetingRepository.save(meeting);

        // Récupérer les détails des utilisateurs pour cette réunion
        Set<String> authIds = new HashSet<>();
        authIds.add(creatorId);
        authIds.addAll(meeting.getParticipantIds());
        String idsParam = String.join(",", authIds);
        Map<String, Map<String, Object>> userDetailsMap = authClient.getUserDetailsByIds(idsParam);

        return convertToDTO(savedMeeting, userDetailsMap);
    }

    public MeetingDTO updateMeeting(Long id, MeetingDTO meetingDTO, String authorizationHeader) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        meeting.setTitle(meetingDTO.getTitle());
        meeting.setDate(meetingDTO.getDate());
        meeting.setTime(meetingDTO.getTime());
        meeting.setDuration(meetingDTO.getDuration());
        meeting.setStatus(meetingDTO.getStatus());
        meeting.setRescheduled(meetingDTO.isRescheduled());
        meeting.setParticipantIds(meetingDTO.getParticipantIds());
        meeting.setProject(meetingDTO.getProject());
        meeting.setMeetingType(meetingDTO.getMeetingType());
        meeting.setMeetingPriority(meetingDTO.getMeetingPriority());
        meeting.setParticipantCount(meetingDTO.getParticipantCount());
        meeting.setTimezone(meetingDTO.getTimezone());
        Meeting updatedMeeting = meetingRepository.save(meeting);

        // Récupérer les détails des utilisateurs pour cette réunion
        Set<String> authIds = new HashSet<>();
        authIds.add(meeting.getCreatorId());
        authIds.addAll(meeting.getParticipantIds());
        String idsParam = String.join(",", authIds);
        Map<String, Map<String, Object>> userDetailsMap = authClient.getUserDetailsByIds(idsParam);

        return convertToDTO(updatedMeeting, userDetailsMap);
    }

    public void deleteMeeting(Long id, String authorizationHeader) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        String creatorId = authClient.decodeToken(authorizationHeader);
        if (!creatorId.equals(meeting.getCreatorId())) {
            throw new RuntimeException("Unauthorized to delete this meeting");
        }
        meetingRepository.deleteById(id);
    }

    public MeetingDTO cancelMeeting(Long id, String authorizationHeader) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        String creatorId = authClient.decodeToken(authorizationHeader);
        if (!creatorId.equals(meeting.getCreatorId())) {
            throw new RuntimeException("Unauthorized to cancel this meeting");
        }
        meeting.setStatus(MeetingStatus.CANCELLED);
        Meeting updatedMeeting = meetingRepository.save(meeting);

        // Récupérer les détails des utilisateurs pour cette réunion
        Set<String> authIds = new HashSet<>();
        authIds.add(meeting.getCreatorId());
        authIds.addAll(meeting.getParticipantIds());
        String idsParam = String.join(",", authIds);
        Map<String, Map<String, Object>> userDetailsMap = authClient.getUserDetailsByIds(idsParam);

        return convertToDTO(updatedMeeting, userDetailsMap);
    }

    public MeetingDTO rescheduleMeeting(Long id, String newDate, String newTime, String authorizationHeader) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));
        String creatorId = authClient.decodeToken(authorizationHeader);
        if (!creatorId.equals(meeting.getCreatorId())) {
            throw new RuntimeException("Unauthorized to reschedule this meeting");
        }
        meeting.setDate(newDate);
        meeting.setTime(newTime);
        meeting.setStartTime(ZonedDateTime.parse(newDate + "T" + newTime + ":00" + meeting.getTimezone().getOffset()));
        meeting.setRescheduled(true);
        meeting.setStatus(MeetingStatus.UPCOMING);
        Meeting updatedMeeting = meetingRepository.save(meeting);

        // Récupérer les détails des utilisateurs pour cette réunion
        Set<String> authIds = new HashSet<>();
        authIds.add(meeting.getCreatorId());
        authIds.addAll(meeting.getParticipantIds());
        String idsParam = String.join(",", authIds);
        Map<String, Map<String, Object>> userDetailsMap = authClient.getUserDetailsByIds(idsParam);

        return convertToDTO(updatedMeeting, userDetailsMap);
    }

    public long countByStatus(String status) {
        return meetingRepository.countByStatus(status);
    }

    public long countByRescheduled(boolean rescheduled) {
        return meetingRepository.countByRescheduled(rescheduled);
    }

    public List<MeetingDTO> getAllMeetings(String authorizationHeader) {
        authClient.decodeToken(authorizationHeader); // Validation du token
        List<Meeting> meetings = meetingRepository.findAll();

        // Collecter tous les authIds uniques (créateur + participants)
        Set<String> authIds = new HashSet<>();
        for (Meeting meeting : meetings) {
            authIds.add(meeting.getCreatorId());
            authIds.addAll(meeting.getParticipantIds());
        }

        // Convertir le Set en une chaîne séparée par des virgules pour l'appel Feign
        String idsParam = String.join(",", authIds);

        // Récupérer les détails des utilisateurs
        Map<String, Map<String, Object>> userDetailsMap = authClient.getUserDetailsByIds(idsParam);

        // Convertir les réunions en DTO avec les détails des utilisateurs
        return meetings.stream()
                .map(meeting -> convertToDTO(meeting, userDetailsMap))
                .collect(Collectors.toList());
    }

    private MeetingDTO convertToDTO(Meeting meeting, Map<String, Map<String, Object>> userDetailsMap) {
        Map<String, Object> creatorDetails = userDetailsMap.getOrDefault(
                meeting.getCreatorId(),
                Map.of("firstName", "Inconnu", "lastName", "Inconnu", "avatar", "")
        );
        List<Map<String, Object>> participantDetails = meeting.getParticipantIds().stream()
                .map(id -> userDetailsMap.getOrDefault(id, Map.of("firstName", "Inconnu", "lastName", "Inconnu", "avatar", "")))
                .collect(Collectors.toList());

        return new MeetingDTO(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getDate(),
                meeting.getTime(),
                meeting.getDuration(),
                meeting.getStatus(),
                meeting.isRescheduled(),
                meeting.getParticipantIds(),
                meeting.getCreatorId(),
                meeting.getProject(),
                meeting.getMeetingType(),
                meeting.getMeetingPriority(),
                meeting.getParticipantCount(),
                meeting.getTimezone(),
                meeting.getStartTime(),
                (String) creatorDetails.get("firstName"),
                (String) creatorDetails.get("lastName"),
                participantDetails.stream().map(d -> (String) d.get("firstName")).collect(Collectors.toList()),
                participantDetails.stream().map(d -> (String) d.get("lastName")).collect(Collectors.toList())
        );
    }
}