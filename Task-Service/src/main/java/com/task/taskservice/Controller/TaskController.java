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
            @PathVariable Long userStoryId,
            @RequestBody TaskDTO taskDTO,
            @RequestHeader("Authorization") String token) {
        TaskDTO createdTask = taskService.createTask(userStoryId ,taskDTO, token);
        return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
    }

    // Update a task
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long id,
            @RequestBody TaskDTO taskDTO) {
        TaskDTO updatedTask = taskService.updateTask(id, taskDTO);
        return ResponseEntity.ok(updatedTask);
    }

    // Delete a task
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    // Get a task by ID
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO taskDTO = taskService.getTaskById(id);
        return ResponseEntity.ok(taskDTO);
    }

    // Get all tasks
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
}