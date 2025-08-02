package com.collaboration.collaborationservice.meeting.dto;

import com.collaboration.collaborationservice.meeting.enums.MeetingStatus;
import com.collaboration.collaborationservice.meeting.enums.MeetingType;
import com.collaboration.collaborationservice.meeting.enums.MeetingPriority;
import com.collaboration.collaborationservice.meeting.enums.Timezone;
import java.time.ZonedDateTime;
import java.util.List;

public class MeetingDTO {

    private Long id;
    private String title;
    private String date;
    private String time;
    private String duration;
    private MeetingStatus status;
    private boolean rescheduled;
    private List<String> participantIds;
    private String creatorId;
    private String project;
    private MeetingType meetingType;
    private MeetingPriority meetingPriority;
    private int participantCount;
    private Timezone timezone;
    private ZonedDateTime startTime;
    private String creatorFirstName;
    private String creatorLastName;
    private List<String> participantFirstNames;
    private List<String> participantLastNames;

    public MeetingDTO() {}

    public MeetingDTO(
            Long id,
            String title,
            String date,
            String time,
            String duration,
            MeetingStatus status,
            boolean rescheduled,
            List<String> participantIds,
            String creatorId,
            String project,
            MeetingType meetingType,
            MeetingPriority meetingPriority,
            int participantCount,
            Timezone timezone,
            ZonedDateTime startTime,
            String creatorFirstName,
            String creatorLastName,
            List<String> participantFirstNames,
            List<String> participantLastNames) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.duration = duration;
        this.status = status;
        this.rescheduled = rescheduled;
        this.participantIds = participantIds;
        this.creatorId = creatorId;
        this.project = project;
        this.meetingType = meetingType;
        this.meetingPriority = meetingPriority;
        this.participantCount = participantCount;
        this.timezone = timezone;
        this.startTime = startTime;
        this.creatorFirstName = creatorFirstName;
        this.creatorLastName = creatorLastName;
        this.participantFirstNames = participantFirstNames;
        this.participantLastNames = participantLastNames;
    }

    public List<String> getParticipantLastNames() {
        return participantLastNames;
    }

    public void setParticipantLastNames(List<String> participantLastNames) {
        this.participantLastNames = participantLastNames;
    }

    public List<String> getParticipantFirstNames() {
        return participantFirstNames;
    }

    public void setParticipantFirstNames(List<String> participantFirstNames) {
        this.participantFirstNames = participantFirstNames;
    }

    public String getCreatorLastName() {
        return creatorLastName;
    }

    public void setCreatorLastName(String creatorLastName) {
        this.creatorLastName = creatorLastName;
    }

    public String getCreatorFirstName() {
        return creatorFirstName;
    }

    public void setCreatorFirstName(String creatorFirstName) {
        this.creatorFirstName = creatorFirstName;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public Timezone getTimezone() {
        return timezone;
    }

    public void setTimezone(Timezone timezone) {
        this.timezone = timezone;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public MeetingPriority getMeetingPriority() {
        return meetingPriority;
    }

    public void setMeetingPriority(MeetingPriority meetingPriority) {
        this.meetingPriority = meetingPriority;
    }

    public MeetingType getMeetingType() {
        return meetingType;
    }

    public void setMeetingType(MeetingType meetingType) {
        this.meetingType = meetingType;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public boolean isRescheduled() {
        return rescheduled;
    }

    public void setRescheduled(boolean rescheduled) {
        this.rescheduled = rescheduled;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public void setStatus(MeetingStatus status) {
        this.status = status;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}