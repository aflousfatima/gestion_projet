package com.collaboration.collaborationservice.channel.dto;


import com.collaboration.collaborationservice.common.enums.ChannelType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ChannelDTO {
    private Long id;
    private String name;
    private ChannelType type;
    private boolean isPrivate;
    private String description;
    private Long projectId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> participantIds;

    // Constructeurs
    public ChannelDTO() {}

    public ChannelDTO(com.collaboration.collaborationservice.channel.entity.Channel channel) {
        this.id = channel.getId();
        this.name = channel.getName();
        this.type = channel.getType();
        this.isPrivate = channel.isPrivate();
        this.description = channel.getDescription();
        this.projectId = channel.getProjectId();
        this.createdBy = channel.getCreatedBy();
        this.createdAt = channel.getCreatedAt();
        this.updatedAt = channel.getUpdatedAt();
        this.participantIds = channel.getParticipants()
                .stream()
                .map(participant -> participant.getUserId())
                .collect(Collectors.toList());
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ChannelType getType() { return type; }
    public void setType(ChannelType type) { this.type = type; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }
}