package com.task.taskservice.Configuration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "project-service", url = "http://localhost:8085")
public interface ProjectClient {

    // Méthode pour récupérer les UserStoryIds du sprint actif pour un projet
    @GetMapping("/api/projects/{projectId}/sprint/actif/user_stories")
    List<Long> getUserStoriesOfActiveSprint(@PathVariable("projectId") Long projectId);
}


