package com.collaboration.collaborationservice.common.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class MessageContent {
    private String text;
    private String fileUrl; // si MessageType = FILE/IMAGE/VIDEO
    private String mimeType;
}