package com.project.project_service.config;

import com.project.project_service.DTO.TaskDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;


public class TaskClientFallback implements TaskClient{

 @Override
 public List<TaskDTO> getTasksByProjectAndUserStory(Long projectId, Long userStoryId, String token){
        return List.of();
    }

@Override
public List<TaskDTO> getTasksByProjectAndUserStoryInternal(Long projectId, Long userStoryId){
        return List.of();
    }

}
