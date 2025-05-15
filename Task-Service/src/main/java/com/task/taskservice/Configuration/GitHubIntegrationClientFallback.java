package com.task.taskservice.Configuration;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

public class GitHubIntegrationClientFallback implements GitHubIntegrationClient {

    @Override
    public ResponseEntity<Object> getCommitsByUserId(String owner, String repo, String userId, String since, Integer perPage) {
        // Return an empty list of commits to avoid breaking the caller
        return ResponseEntity.ok(Collections.emptyList());
    }

    @Override
    public ResponseEntity<Map<String, String>> getAccessToken(String userId) {
        // Return an empty map to indicate no access token available
        return ResponseEntity.ok(Collections.emptyMap());
    }
}