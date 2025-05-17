package com.collaboration.collaborationservice.channel.controller;


import com.collaboration.collaborationservice.channel.dto.CreateChannelRequest;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.service.ChannelService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelService channelService;

    @Autowired
    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping("/createChanel")
    public ResponseEntity<Channel> createChannel(
            @Valid @RequestBody CreateChannelRequest request,
            @RequestHeader("Authorization") String authorization) {
        Channel createdChannel = channelService.createChannel(request, authorization);
        return ResponseEntity.ok(createdChannel);
    }

    @GetMapping
    public ResponseEntity<List<Channel>> getAccessibleChannels(
            @RequestHeader("Authorization") String authorization) {
        List<Channel> channels = channelService.getAccessibleChannels(authorization);
        return ResponseEntity.ok(channels);
    }

    @GetMapping("/public")
    public ResponseEntity<List<Channel>> getAllPublicChannels() {
        List<Channel> channels = channelService.getAllPublicChannels();
        return ResponseEntity.ok(channels);
    }
}