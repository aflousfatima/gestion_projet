package com.task.taskservice.Mapper; // Uppercase Mapper

import com.task.taskservice.Entity.Task;
import com.task.taskservice.Entity.User;
import com.task.taskservice.Entity.Tag;
import com.task.taskservice.DTO.TaskDTO; // Uppercase DTO
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TaskMapper {

    public TaskDTO toDTO(Task task) {
        if (task == null) {
            return null;
        }

        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(task.getId());
        taskDTO.setTitle(task.getTitle());
        taskDTO.setDescription(task.getDescription());
        taskDTO.setCreationDate(task.getCreationDate());
        taskDTO.setStartDate(task.getStartDate());
        taskDTO.setDueDate(task.getDueDate());
        taskDTO.setCompletedDate(task.getCompletedDate());
        taskDTO.setEstimationTime(task.getEstimationTime());
        taskDTO.setTimeSpent(task.getTimeSpent());
        taskDTO.setStatus(task.getStatus());
        taskDTO.setPriority(task.getPriority());
        taskDTO.setProgress(task.getProgress());
        taskDTO.setUserStoryId(task.getUserStory());
        taskDTO.setProjectId(task.getProjectId());
        taskDTO.setCreatedBy(task.getCreatedBy());

        if (task.getDependencies() != null) {
            taskDTO.setDependencyIds(task.getDependencies().stream()
                    .map(Task::getId)
                    .collect(Collectors.toList()));
        }

        if (task.getAssignedUsers() != null) {
            taskDTO.setAssignedUserIds(task.getAssignedUsers().stream()
                    .map(User::getId)
                    .collect(Collectors.toSet()));
        }

        if (task.getTags() != null) {
            taskDTO.setTagIds(task.getTags().stream()
                    .map(Tag::getId)
                    .collect(Collectors.toSet()));
        }

        return taskDTO;
    }

    public Task toEntity(TaskDTO taskDTO) {
        if (taskDTO == null) {
            return null;
        }

        Task task = new Task();
        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setCreationDate(taskDTO.getCreationDate());
        task.setStartDate(taskDTO.getStartDate());
        task.setDueDate(taskDTO.getDueDate());
        task.setCompletedDate(taskDTO.getCompletedDate());
        task.setEstimationTime(taskDTO.getEstimationTime());
        task.setTimeSpent(taskDTO.getTimeSpent());
        task.setStatus(taskDTO.getStatus());
        task.setPriority(taskDTO.getPriority());
        task.setProgress(taskDTO.getProgress());
        task.setUserStory(taskDTO.getUserStoryId());
        task.setProjectId(taskDTO.getProjectId());
        task.setCreatedBy(taskDTO.getCreatedBy());

        return task;
    }
}