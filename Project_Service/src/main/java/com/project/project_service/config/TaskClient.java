package com.project.project_service.config;


import com.project.project_service.DTO.TaskDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "task-service", url = "http://localhost:8086")  // Utilisez un URL dynamique ou un nom de service si vous utilisez Eureka
public interface TaskClient {

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
