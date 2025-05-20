package com.collaboration.collaborationservice.call.controller;

import com.collaboration.collaborationservice.call.dto.CallDTO;
import com.collaboration.collaborationservice.call.entity.Call;
import com.collaboration.collaborationservice.call.service.CallService;
import com.collaboration.collaborationservice.call.service.SignalingMessage;
import com.collaboration.collaborationservice.common.enums.CallType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/{channelId}/calls")
public class CallController {

    @Autowired
    private CallService callService;

    @PostMapping("/initiate")
    public ResponseEntity<CallDTO> initiateCall(
            @PathVariable Long channelId,
            @RequestBody CallType callType,
            @RequestHeader("Authorization") String token) {
        CallDTO call = callService.initiateCall(channelId, callType, token);
        return ResponseEntity.ok(call);
    }

    @PostMapping("/{callId}/join")
    public ResponseEntity<CallDTO> joinCall(
            @PathVariable Long channelId,
            @PathVariable Long callId,
            @RequestHeader("Authorization") String token) {
        CallDTO call = callService.joinCall(callId, token);
        return ResponseEntity.ok(call);
    }

    @PostMapping("/{callId}/end")
    public ResponseEntity<Void> endCall(
            @PathVariable Long channelId,
            @PathVariable Long callId,
            @RequestHeader("Authorization") String token) {
        callService.endCall(callId, token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{callId}/signaling")
    public ResponseEntity<Void> handleSignaling(
            @PathVariable Long channelId,
            @PathVariable Long callId,
            @RequestBody SignalingMessage signalingMessage,
            @RequestHeader("Authorization") String token) {
        callService.handleSignaling(callId, signalingMessage.getSdpInfo(), signalingMessage.getType(), token);
        return ResponseEntity.ok().build();
    }
}