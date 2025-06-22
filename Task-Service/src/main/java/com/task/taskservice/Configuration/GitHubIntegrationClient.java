package com.task.taskservice.Configuration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "github-integration-service", url = "http://github-integration-service:8087",fallback = GitHubIntegrationClientFallback.class)
public interface GitHubIntegrationClient {
    @GetMapping("/fetch_data/repos/{owner}/{repo}/commits-by-user")
    ResponseEntity<Object> getCommitsByUserId(
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo,
            @RequestParam("userId") String userId,
            @RequestParam(value = "since", required = false) String since,
            @RequestParam(value = "per_page", defaultValue = "30") Integer perPage);
    @GetMapping("/api/github-integration/token/{userId}")
    ResponseEntity<Map<String, String>> getAccessToken(@PathVariable("userId") String userId);



}