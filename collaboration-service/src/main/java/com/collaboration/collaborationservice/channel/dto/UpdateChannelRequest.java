package com.collaboration.collaborationservice.channel.dto;

import com.collaboration.collaborationservice.common.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateChannelRequest {

    @NotBlank(message = "Le nom du canal est requis")
    private String name;

    @NotNull(message = "Le type de canal est requis")
    private ChannelType type;

    @NotNull(message = "Le statut public/privé est requis")
    private Boolean isPrivate;

    private String description;

    private Long projectId; // Optionnel, ID du projet

    private List<String> participantIds; // Optionnel, liste des IDs des participants
}