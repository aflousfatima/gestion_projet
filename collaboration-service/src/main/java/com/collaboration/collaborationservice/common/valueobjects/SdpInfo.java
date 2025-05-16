package com.collaboration.collaborationservice.common.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class SdpInfo {
    @Column(columnDefinition = "TEXT")
    private String sdp;

    @Column(name = "sdp_type")
    private String sdpType; // ex. "offer", "answer"
}