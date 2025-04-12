package com.task.taskservice.Service;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.Configuration.ProjectClient;
import com.task.taskservice.DTO.TaskDTO; // Uppercase DTO
import com.task.taskservice.Entity.Tag;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Entity.User;
import com.task.taskservice.Mapper.TaskMapper; // Uppercase Mapper
import com.task.taskservice.Repository.TagRepository;
import com.task.taskservice.Repository.TaskRepository;
import com.task.taskservice.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final AuthClient authClient;

    private final ProjectClient projectClient;


    @Autowired
    public TaskService(TaskRepository taskRepository, UserRepository userRepository,
                       TagRepository tagRepository, TaskMapper taskMapper, AuthClient authClient,ProjectClient projectClient) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.taskMapper = taskMapper;
        this.authClient = authClient;
        this.projectClient = projectClient;
    }

    @Transactional
    public TaskDTO createTask(Long projectId, Long userStoryId, TaskDTO taskDTO, String token) {
        // Validate token
        String createdBy = authClient.decodeToken(token);

        // Set defaults
        if (taskDTO.getCreationDate() == null) {
            taskDTO.setCreationDate(LocalDate.now());
        }
        taskDTO.setProjectId(projectId);
        taskDTO.setUserStoryId(userStoryId);
        taskDTO.setCreatedBy(createdBy);

        // Convert DTO to entity
        Task task = taskMapper.toEntity(taskDTO);

        // Fetch and set dependencies
        if (taskDTO.getDependencyIds() != null && !taskDTO.getDependencyIds().isEmpty()) {
            List<Task> dependencies = taskRepository.findAllById(taskDTO.getDependencyIds());
            if (dependencies.size() != taskDTO.getDependencyIds().size()) {
                throw new IllegalArgumentException("One or more dependency IDs are invalid");
            }
            task.setDependencies(dependencies);
        }

        // Fetch and set assigned users
        if (taskDTO.getAssignedUserIds() != null && !taskDTO.getAssignedUserIds().isEmpty()) {
            Set<User> assignedUsers = userRepository.findAllById(taskDTO.getAssignedUserIds())
                    .stream().collect(Collectors.toSet());
            if (assignedUsers.size() != taskDTO.getAssignedUserIds().size()) {
                throw new IllegalArgumentException("One or more user IDs are invalid");
            }
            task.setAssignedUsers(assignedUsers);
        }

        // Fetch and set tags
        if (taskDTO.getTagIds() != null && !taskDTO.getTagIds().isEmpty()) {
            Set<Tag> tags = tagRepository.findAllById(taskDTO.getTagIds())
                    .stream().collect(Collectors.toSet());
            if (tags.size() != taskDTO.getTagIds().size()) {
                throw new IllegalArgumentException("One or more tag IDs are invalid");
            }
            task.setTags(tags);
        }

        // Save the task
        Task savedTask = taskRepository.save(task);

        // Convert back to DTO and return
        return taskMapper.toDTO(savedTask);
    }


    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long projectId, Long userStoryId, Long taskId, String token) {
        // Validate token
        String createdBy = authClient.decodeToken(token);
        if (createdBy == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        // Fetch task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        // Verify project and user story
        if (!task.getProjectId().equals(projectId) || !task.getUserStory().equals(userStoryId)) {
            throw new IllegalArgumentException("Task does not belong to the specified project or user story");
        }

        // Convert to DTO
        return taskMapper.toDTO(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProjectAndUserStory(Long projectId, Long userStoryId, String token) {
        // Validate token
        String createdBy = authClient.decodeToken(token);
        if (createdBy == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        // Fetch tasks
        List<Task> tasks = taskRepository.findByProjectIdAndUserStory(projectId, userStoryId);

        // Convert to DTOs
        return tasks.stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProjectId(Long projectId, String token) {
        // Validate token
        String createdBy = authClient.decodeToken(token);
        if (createdBy == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        // Fetch tasks
        List<Task> tasks = taskRepository.findByProjectId(projectId);

        // Convert to DTOs
        return tasks.stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());
    }


    public List<TaskDTO> getTasksOfActiveSprint(Long projectId, String token) {

        // Log initial pour indiquer que la méthode est appelée
        System.out.println("Received request to fetch tasks for active sprint of project ID: " + projectId);

        // Log pour l'appel au microservice pour obtenir les UserStory IDs
        System.out.println("Fetching active sprint user stories for project ID: " + projectId);

        List<Long> activeStoryIds = projectClient.getUserStoriesOfActiveSprint(projectId);

        // Log des userStoryIds récupérés
        if (activeStoryIds.isEmpty()) {
            System.out.println("No active story IDs found for project ID: " + projectId);
            return Collections.emptyList();  // Retourne une liste vide si aucune UserStory n'est trouvée
        }

        System.out.println("Found " + activeStoryIds.size() + " active story IDs for project ID: " + projectId);

        // Recherche des tâches associées aux UserStories récupérées
        List<Task> tasks = taskRepository.findByUserStoryIn(activeStoryIds);

        // Log du nombre de tâches trouvées
        System.out.println("Found " + tasks.size() + " tasks associated with the active sprint for project ID: " + projectId);

        // Si aucune tâche n'est trouvée, loguer et retourner une liste vide
        if (tasks.isEmpty()) {
            System.out.println("No tasks found for the active sprint of project ID: " + projectId);
            return Collections.emptyList();  // Retourne une liste vide si aucune tâche n'est associée
        }

        // Log avant de convertir les entités Task en DTOs
        System.out.println("Converting " + tasks.size() + " tasks to DTOs for project ID: " + projectId);

        // Conversion des entités Task en DTOs
        List<TaskDTO> taskDTOs = tasks.stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());

        // Log après la conversion
        System.out.println("Successfully converted " + taskDTOs.size() + " tasks to DTOs for project ID: " + projectId);

        // Retourne la liste des DTOs de tâches
        return taskDTOs;
    }

}