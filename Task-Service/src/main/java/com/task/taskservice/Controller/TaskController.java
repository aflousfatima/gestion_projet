package com.task.taskservice.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.task.taskservice.DTO.TaskDTO;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Mapper.TaskMapper;
import com.task.taskservice.Service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@RestController
@RequestMapping("/api/project/tasks")
public class TaskController {


    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskMapper taskMapper;

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
    @PutMapping("/{taskId}/updateTask")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskDTO taskDTO,
            @RequestHeader("Authorization") String token) {
        TaskDTO updatedTask = taskService.updateTask(taskId, taskDTO, token);
        return new ResponseEntity<>(updatedTask, HttpStatus.OK);
    }

    @DeleteMapping("/{taskId}/deleteTask")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

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


    @PostMapping("/{taskId}/attachments")
    public ResponseEntity<TaskDTO> uploadFileToTask(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        try {
            Task updatedTask = taskService.attachFileToTask(taskId, file, token);
            TaskDTO taskDTO = taskMapper.toDTO(updatedTask); // Convert to DTO with signed URLs
            return new ResponseEntity<>(taskDTO, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(
            @RequestParam String publicId,
            @RequestHeader("Authorization") String token) {
        logger.info("Received DELETE request for publicId: {}", publicId);
        try {
            taskService.deleteFileFromTask(publicId, token);
            return new ResponseEntity<>("File deleted successfully.", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            logger.warn("File not found for publicId: {}", publicId);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IOException e) {
            logger.error("Failed to delete file from Cloudinary for publicId: {}", publicId, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("Failed to delete file for publicId: {}", publicId, e);
            return new ResponseEntity<>("Failed to delete file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}