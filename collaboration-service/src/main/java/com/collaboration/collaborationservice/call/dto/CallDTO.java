package com.collaboration.collaborationservice.call.dto;

import com.collaboration.collaborationservice.common.enums.CallStatus;
import com.collaboration.collaborationservice.common.enums.CallType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CallDTO {
    private Long id;
    private Long channelId;
    private String channelName;
    private Long initiatorId;
    private CallStatus status;
    private CallType type;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<Long> participantIds;
}