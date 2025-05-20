package com.collaboration.collaborationservice.participant.controller;

import com.collaboration.collaborationservice.participant.Service.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {
    @Autowired
    private PresenceService presenceService;

    @GetMapping("/channel/{channelId}/online-users")
    public Set<String> getOnlineUsers(@PathVariable Long channelId) {
        return presenceService.getOnlineUsers();
    }
}