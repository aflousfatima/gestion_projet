package com.collaboration.collaborationservice.message.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactionDTO {
    private Long id;
    private Long messageId;
    private String participantId;
    private String emoji;
}