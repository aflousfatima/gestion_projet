package com.collaboration.collaborationservice.channel.service;

import com.collaboration.collaborationservice.channel.entity.Channel;
import com.collaboration.collaborationservice.channel.repository.ChannelRepository;
import org.springframework.stereotype.Service;

@Service
public class ChannelService {
    private final ChannelRepository channelRepository;

    public ChannelService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    public Channel createChannel(Channel channel) {
        return channelRepository.save(channel);
    }

    public Channel findById(Long id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
    }
}