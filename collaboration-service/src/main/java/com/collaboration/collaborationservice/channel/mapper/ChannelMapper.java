package com.collaboration.collaborationservice.channel.mapper;

import com.collaboration.collaborationservice.channel.dto.ChannelDTO;
import com.collaboration.collaborationservice.channel.entity.Channel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChannelMapper {

    public ChannelDTO toDTO(Channel channel) {
        return new ChannelDTO(channel);
    }

    public List<ChannelDTO> toDTOList(List<Channel> channels) {
        return channels.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}