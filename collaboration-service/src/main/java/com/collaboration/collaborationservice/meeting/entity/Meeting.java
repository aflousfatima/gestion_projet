package com.collaboration.collaborationservice.meeting.entity;

import com.collaboration.collaborationservice.meeting.enums.MeetingStatus;
import com.collaboration.collaborationservice.meeting.enums.MeetingType;
import com.collaboration.collaborationservice.meeting.enums.MeetingPriority;
import com.collaboration.collaborationservice.meeting.enums.Timezone;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String date; // Format YYYY-MM-DD

    @Column(nullable = false)
    private String time; // Format HH:MM

    @Column(nullable = false)
    private String duration;

    @Column(nullable = false)
    private ZonedDateTime startTime; // Changé de LocalDateTime à ZonedDateTime

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status;

    @Column(nullable = false)
    private boolean rescheduled;

    @ElementCollection
    private List<String> participantIds;

    @Column(name = "creator_id", nullable = false)
    private String creatorId;

    @Column(name = "project_id", nullable = false)
    private String project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingType meetingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingPriority meetingPriority;

    @Column(nullable = false)
    private int participantCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Timezone timezone;

    // Constructeurs
    public Meeting() {}

    public Meeting(
            String title,
            String date,
            String time,
            String duration,
            MeetingStatus status,
            List<String> participantIds,
            String creatorId,
            String project,
            MeetingType meetingType,
            MeetingPriority meetingPriority,
            int participantCount,
            Timezone timezone) {
        this.title = title;
        this.date = date;
        this.time = time;
        this.duration = duration;
        this.status = status;
        this.rescheduled = false;
        this.participantIds = participantIds;
        this.creatorId = creatorId;
        this.project = project;
        this.meetingType = meetingType;
        this.meetingPriority = meetingPriority;
        this.participantCount = participantCount;
        this.timezone = timezone;
        // Parsing avec ZonedDateTime
        this.startTime = ZonedDateTime.parse(date + "T" + time + ":00" + timezone.getOffset());
    }

    // Getters et Setters (ajustés pour ZonedDateTime)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public ZonedDateTime getStartTime() { return startTime; }
    public void setStartTime(ZonedDateTime startTime) { this.startTime = startTime; }
    public MeetingStatus getStatus() { return status; }
    public void setStatus(MeetingStatus status) { this.status = status; }
    public boolean isRescheduled() { return rescheduled; }
    public void setRescheduled(boolean rescheduled) { this.rescheduled = rescheduled; }
    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
        this.participantCount = participantIds.size();
    }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }
    public MeetingType getMeetingType() { return meetingType; }
    public void setMeetingType(MeetingType meetingType) { this.meetingType = meetingType; }
    public MeetingPriority getMeetingPriority() { return meetingPriority; }
    public void setMeetingPriority(MeetingPriority meetingPriority) { this.meetingPriority = meetingPriority; }
    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }
    public Timezone getTimezone() { return timezone; }
    public void setTimezone(Timezone timezone) { this.timezone = timezone; }
}