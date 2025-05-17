package com.task.taskservice.Configuration;

import com.task.taskservice.DTO.UserDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AuthClientFallback implements AuthClient {

    @Override
    public String decodeToken(String authorization) {
        // Return a fallback user ID
        return "fallback-token";
    }

    @Override
    public Map<String, Object> getUserDetailsByAuthId(String authId, String authorization) {
        // Return a map indicating fallback
        return Map.of("authId", authId, "fallback", true);
    }

    @Override
    public List<UserDTO> getUsersByIds(String authorizationHeader, List<String> userIds) {
        // Return an empty list of users
        return Collections.emptyList();
    }
}