package com.collaboration.collaborationservice.channel.controller;


import com.collaboration.collaborationservice.channel.dto.ChannelDTO;
import com.collaboration.collaborationservice.channel.dto.CreateChannelRequest;
import com.collaboration.collaborationservice.channel.dto.UpdateChannelRequest;
import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.service.ChannelService;
import com.collaboration.collaborationservice.common.enums.ChannelType;
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
    public ResponseEntity<List<ChannelDTO>> getPublicChannels() {
        return ResponseEntity.ok(channelService.getAllPublicChannels());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChannelDTO> getChannelById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorization) throws IllegalAccessException {
        ChannelDTO channel = channelService.getChannelById(id, authorization);
        return ResponseEntity.ok(channel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Channel> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChannelRequest request,
            @RequestHeader("Authorization") String authorization) throws IllegalAccessException {
        Channel updatedChannel = channelService.updateChannel(id, request, authorization);
        return ResponseEntity.ok(updatedChannel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorization) throws IllegalAccessException {
        channelService.deleteChannel(id, authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Channel>> getChannelsByProjectId(@PathVariable Long projectId) {
        List<Channel> channels = channelService.getChannelsByProjectId(projectId);
        return ResponseEntity.ok(channels);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Channel>> getChannelsByType(@PathVariable ChannelType type) {
        List<Channel> channels = channelService.getChannelsByType(type);
        return ResponseEntity.ok(channels);
    }

    @GetMapping("/created-by/{userId}")
    public ResponseEntity<List<Channel>> getChannelsCreatedByUser(@PathVariable String userId) {
        List<Channel> channels = channelService.getChannelsCreatedByUser(userId);
        return ResponseEntity.ok(channels);
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Channel> updateChannelVisibility(
            @PathVariable Long id,
            @RequestParam("private") boolean isPrivate,
            @RequestHeader("Authorization") String authorization) throws IllegalAccessException {
        Channel updatedChannel = channelService.updateChannelVisibility(id, isPrivate, authorization);
        return ResponseEntity.ok(updatedChannel);
    }
}