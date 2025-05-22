package com.project.project_service.config;


import com.project.project_service.DTO.TaskDTO;
import com.project.project_service.DTO.TaskStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "task-service", url = "http://localhost:8086",fallback = TaskClientFallback.class)  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface TaskClient {

    @GetMapping("/api/chatbot/tasks/status")
    List<TaskStatusResponse> getTaskStatusByProject(@RequestParam("projectId") Long projectId);

    @GetMapping("/api/project/tasks/{projectId}/{userStoryId}")
    List<TaskDTO> getTasksByProjectAndUserStory(
            @PathVariable("projectId") Long projectId,
            @PathVariable("userStoryId") Long userStoryId,
            @RequestHeader("Authorization") String token
    );

    @GetMapping("/api/project/tasks/internal/{projectId}/{userStoryId}")
     List<TaskDTO> getTasksByProjectAndUserStoryInternal(
            @PathVariable("projectId") Long projectId,
            @PathVariable("userStoryId") Long userStoryId
    );

}
