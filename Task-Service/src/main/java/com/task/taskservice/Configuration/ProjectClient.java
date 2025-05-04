package com.task.taskservice.Configuration;

import com.task.taskservice.DTO.ProjectDTO;
import com.task.taskservice.DTO.ProjectResponseWithRoleDTO;
import com.task.taskservice.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "project-service", url = "http://localhost:8085")
public interface ProjectClient {

    @GetMapping("/api/projects/by-user")
    ProjectResponseWithRoleDTO getProjectsByUser(@RequestParam("authId") String authId);

    // Méthode pour récupérer les UserStoryIds du sprint actif pour un projet
    @GetMapping("/api/projects/{projectId}/sprint/actif/user_stories")
    List<Long> getUserStoriesOfActiveSprint(@PathVariable("projectId") Long projectId);


}


