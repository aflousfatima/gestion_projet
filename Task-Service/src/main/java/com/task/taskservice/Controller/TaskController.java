package com.task.taskservice.Controller;


import com.task.taskservice.DTO.TaskDTO;
import com.task.taskservice.Service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/project/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    // Create a task
    @PostMapping("/{projectId}/{userStoryId}/createTask")
    public ResponseEntity<TaskDTO> createTask(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestBody TaskDTO taskDTO,
            @RequestHeader("Authorization") String token) {
        TaskDTO createdTask = taskService.createTask(projectId ,userStoryId ,taskDTO, token);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    // Update a task

    // Get a task by ID
    @GetMapping("/{projectId}/{userStoryId}/{taskId}")
    public ResponseEntity<TaskDTO> getTask(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @PathVariable Long taskId,
            @RequestHeader("Authorization") String token) {
        TaskDTO taskDTO = taskService.getTaskById(projectId, userStoryId, taskId, token);
        return new ResponseEntity<>(taskDTO, HttpStatus.OK);
    }

    // Get all tasks for a project and user story
    @GetMapping("/{projectId}/{userStoryId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProjectAndUserStory(
            @PathVariable Long projectId,
            @PathVariable Long userStoryId,
            @RequestHeader("Authorization") String token) {
        List<TaskDTO> tasks = taskService.getTasksByProjectAndUserStory(projectId, userStoryId, token);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    // Get all tasks for a project
    @GetMapping("/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {
        List<TaskDTO> tasks = taskService.getTasksByProjectId(projectId, token);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }


    @GetMapping("/active_sprint/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksOfActiveSprint(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token) {

        List<TaskDTO> tasks = taskService.getTasksOfActiveSprint(projectId, token);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }
}