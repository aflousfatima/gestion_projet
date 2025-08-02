package com.collaboration.collaborationservice.meeting.controller;

import com.collaboration.collaborationservice.meeting.dto.MeetingDTO;
import com.collaboration.collaborationservice.meeting.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @PostMapping
    public ResponseEntity<MeetingDTO> createMeeting(@RequestBody MeetingDTO meetingDTO,
                                                    @RequestHeader("Authorization") String authorizationHeader) {
        MeetingDTO createdMeeting = meetingService.createMeeting(meetingDTO, authorizationHeader);
        return ResponseEntity.ok(createdMeeting);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeetingDTO> updateMeeting(@PathVariable Long id, @RequestBody MeetingDTO meetingDTO,
                                                    @RequestHeader("Authorization") String authorizationHeader) {
        MeetingDTO updatedMeeting = meetingService.updateMeeting(id, meetingDTO, authorizationHeader);
        return ResponseEntity.ok(updatedMeeting);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id,
                                              @RequestHeader("Authorization") String authorizationHeader) {
        meetingService.deleteMeeting(id, authorizationHeader);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<MeetingDTO> cancelMeeting(@PathVariable Long id,
                                                    @RequestHeader("Authorization") String authorizationHeader) {
        MeetingDTO cancelledMeeting = meetingService.cancelMeeting(id, authorizationHeader);
        return ResponseEntity.ok(cancelledMeeting);
    }

    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<MeetingDTO> rescheduleMeeting(@PathVariable Long id,
                                                        @RequestParam String newDate,
                                                        @RequestParam String newTime,
                                                        @RequestHeader("Authorization") String authorizationHeader) {
        MeetingDTO rescheduledMeeting = meetingService.rescheduleMeeting(id, newDate, newTime, authorizationHeader);
        return ResponseEntity.ok(rescheduledMeeting);
    }

    @GetMapping("/count/status/{status}")
    public ResponseEntity<Long> countByStatus(@PathVariable String status,
                                              @RequestHeader("Authorization") String authorizationHeader) {
        long count = meetingService.countByStatus(status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/rescheduled")
    public ResponseEntity<Long> countByRescheduled(@RequestParam boolean rescheduled,
                                                   @RequestHeader("Authorization") String authorizationHeader) {
        long count = meetingService.countByRescheduled(rescheduled);
        return ResponseEntity.ok(count);
    }

    @GetMapping
    public ResponseEntity<List<MeetingDTO>> getAllMeetings(@RequestHeader("Authorization") String authorizationHeader) {
        List<MeetingDTO> meetings = meetingService.getAllMeetings(authorizationHeader);
        return ResponseEntity.ok(meetings);
    }
}