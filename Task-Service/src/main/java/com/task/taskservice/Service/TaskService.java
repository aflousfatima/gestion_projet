package com.task.taskservice.Service;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.DTO.TaskDTO;
import com.task.taskservice.Entity.Task;
import com.task.taskservice.Entity.WorkItem;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Mapper.TaskMapper;
import com.task.taskservice.Repository.TaskRepository;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private TaskMapper taskMapper; // Injecte le mapper généré par MapStruct

    // Create a task
    public TaskDTO createTask(Long userStoryId, TaskDTO taskDTO, String token) {
        // Valider le token et extraire l'utilisateur
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Invalid token");
        }

        // Valider les champs obligatoires
        if (taskDTO.getTitle() == null || taskDTO.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (userStoryId == null) {
            throw new IllegalArgumentException("User story ID is required");
        }

        // Mapper le DTO vers l'entité
        Task task = taskMapper.toEntity(taskDTO);

        // Définir les champs gérés par le backend
        task.setCreatedBy(userIdStr);
        task.setCreationDate(LocalDate.now());
        task.setLastModifiedDate(LocalDate.now());
        task.setUserStory(userStoryId); // Lier à la user story

        // Valider et définir les champs optionnels
        if (task.getProgress() != null && (task.getProgress() < 0 || task.getProgress() > 100)) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }
        if (task.getStatus() == null) {
            task.setStatus(WorkItemStatus.TO_DO); // Par défaut
        }
        if (task.getAssignedUser() == null) {
            task.setAssignedUser(new ArrayList<>()); // Liste vide par défaut
        }
        if (task.getTags() == null) {
            task.setTags(new HashSet<>()); // Ensemble vide par défaut
        }

        // Ne pas définir ces champs lors de la création
        task.setCompletedDate(null);
        task.setTimeSpent(null);
        task.setComments(new ArrayList<>());
        task.setAttachments(new ArrayList<>());
        task.setDependencies(new ArrayList<>()); // Pas de dépendances lors de la création

        // Sauvegarder la tâche
        Task savedTask = taskRepository.save(task);
        return taskMapper.toDTO(savedTask);
    }

    // Update a task
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (optionalTask.isEmpty()) {
            throw new RuntimeException("Task not found with id: " + id);
        }
        Task task = optionalTask.get();
        task.setLastModifiedDate(LocalDate.now());
        taskMapper.updateEntity(taskDTO, task); // Utilise le mapper pour mettre à jour l'entité

        // Update dependencies if provided
        if (taskDTO.getDependencyIds() != null) {
            List<Task> dependencies = taskRepository.findAllById(taskDTO.getDependencyIds());
            task.setDependencies(dependencies);
        }

        Task updatedTask = taskRepository.save(task);
        return taskMapper.toDTO(updatedTask); // Utilise le mapper pour renvoyer le DTO
    }

    // Delete a task
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    // Retrieve a task by ID
    public TaskDTO getTaskById(Long id) {
        Optional<Task> optionalTask = taskRepository.findById(id);
        if (optionalTask.isEmpty()) {
            throw new RuntimeException("Task not found with id: " + id);
        }
        return taskMapper.toDTO(optionalTask.get()); // Utilise le mapper
    }

    // Retrieve all tasks
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toDTO) // Utilise le mapper avec une référence de méthode
                .collect(Collectors.toList());
    }
}