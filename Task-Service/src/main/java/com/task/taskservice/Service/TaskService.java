package com.task.taskservice.Service;
import com.task.taskservice.Configuration.GitHubIntegrationClient;
import com.task.taskservice.DTO.*;
import com.task.taskservice.Entity.*;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import com.task.taskservice.Repository.ProcessedCommitRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.Configuration.ProjectClient;
import com.task.taskservice.Mapper.TaskMapper; // Uppercase Mapper
import com.task.taskservice.Repository.FileAttachmentRepository;
import com.task.taskservice.Repository.TagRepository;
import com.task.taskservice.Repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Task;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@EnableScheduling
public class TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final AuthClient authClient;

    private final ProjectClient projectClient;
    private final GitHubIntegrationClient gitHubIntegrationClient;
    @Autowired
    private EntityManager entityManager;

    private final FileAttachmentRepository fileAttachmentRepository;

    private  final ProcessedCommitRepository processedCommitRepository;
    @Autowired
    private final  CloudinaryService cloudinaryService;
    @Autowired
    public TaskService(TaskRepository taskRepository,
                       TagRepository tagRepository, TaskMapper taskMapper, AuthClient authClient,ProjectClient projectClient , CloudinaryService cloudinaryService , FileAttachmentRepository fileAttachmentRepository , GitHubIntegrationClient gitHubIntegrationClient , ProcessedCommitRepository processedCommitRepository) {
        this.taskRepository = taskRepository;
        this.tagRepository = tagRepository;
        this.taskMapper = taskMapper;
        this.authClient = authClient;
        this.projectClient = projectClient;
        this.cloudinaryService = cloudinaryService;
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.gitHubIntegrationClient= gitHubIntegrationClient;
        this.processedCommitRepository= processedCommitRepository;
    }


    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "createTaskRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "createTaskBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "createTaskRetryFallback")
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
            checkForDependencyCycles(task, dependencies);
            task.setDependencies(dependencies);
        }
        // Directly set user IDs without validation
        if (taskDTO.getAssignedUserIds() != null) {
            System.out.println("➡️ Liste des userIds assignés reçue : " + taskDTO.getAssignedUserIds());

            Set<String> userIdsSet = new HashSet<>(taskDTO.getAssignedUserIds());
            System.out.println("➡️ Conversion en Set (suppression des doublons éventuels) : " + userIdsSet);

            task.setAssignedUserIds(userIdsSet);
            System.out.println("✅ Les userIds ont été assignés à la tâche : " + task.getAssignedUserIds());
        } else {
            System.out.println("⚠️ Aucun userId assigné reçu dans le taskDTO.");
        }


        // Gestion des tags
        if (taskDTO.getTags() != null && !taskDTO.getTags().isEmpty()) {
            logger.info("➡️ Tags reçus pour la tâche : {}", taskDTO.getTags());

            Set<Tag> tags = new HashSet<>();
            for (String tagName : taskDTO.getTags()) {
                logger.info("🔍 Vérification du tag : {}", tagName);
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            logger.info("➕ Tag non trouvé, création du tag : {}", tagName);
                            Tag newTag = new Tag();
                            newTag.setName(tagName);
                            newTag.setWorkItems(new HashSet<>()); // Initialiser la collection
                            return tagRepository.save(newTag); // Sauvegarder explicitement le tag
                        });
                tags.add(tag);
                logger.info("✅ Tag ajouté à la liste : {}", tag.getName());
            }

            // Réinitialiser les tags de la tâche
            task.setTags(new HashSet<>());
            // Associer les tags à la tâche et synchroniser la relation bidirectionnelle
            for (Tag tag : tags) {
                task.getTags().add(tag); // Ajouter au côté WorkItem
                tag.getWorkItems().add(task); // Synchroniser le côté Tag
                logger.info("🔗 Tag associé à la tâche : {}", tag.getName());
            }

            logger.info("✅ Tous les tags ont été associés à la tâche : {}", tags.stream().map(Tag::getName).collect(Collectors.toSet()));
        } else {
            logger.info("⚠️ Aucun tag reçu pour la tâche.");
        }

        // Enregistrer l'historique pour la création
        logHistory(task, "CREATION", "Task created: " + task.getTitle(), createdBy);

        updateProgress(task);
        // Save the task
        Task savedTask = taskRepository.save(task);

        // Convert back to DTO and return
        return taskMapper.toDTO(savedTask);
    }

    public TaskDTO createTaskBulkheadFallback(Long projectId, Long userStoryId, TaskDTO taskDTO, String token, Throwable t) {
        logger.error("Bulkhead fallback for createTask: {}", t.getMessage());
        throw new RuntimeException("Too many concurrent task creation requests");
    }

    public TaskDTO createTaskRetryFallback(Long projectId, Long userStoryId, TaskDTO taskDTO, String token, Throwable t) {
        logger.error("Retry fallback for createTask: {}", t.getMessage());
        throw new RuntimeException("Failed to create task after retries");
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "updateTaskRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "updateTaskBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "updateTaskRetryFallback")
    @Transactional
    public TaskDTO updateTask(Long taskId, TaskDTO taskDTO, String token) {
        // Validate token
        String updatedBy = authClient.decodeToken(token);

        // Fetch existing task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with ID: " + taskId));

        // Valider les dépendances si le statut change à IN_PROGRESS ou DONE
        if (taskDTO.getStatus() != null &&
                (taskDTO.getStatus() == WorkItemStatus.IN_PROGRESS || taskDTO.getStatus() == WorkItemStatus.DONE)) {
            validateDependencies(task);
        }

// Suivre les changements pour l'historique
        StringBuilder changes = new StringBuilder();

        // Gérer le chronomètre pour le timeSpent
        if (taskDTO.getStatus() != null) {
            WorkItemStatus newStatus = taskDTO.getStatus();
            WorkItemStatus currentStatus = task.getStatus();

            // Si on passe à IN_PROGRESS, démarrer le chronomètre
            if (newStatus == WorkItemStatus.IN_PROGRESS && currentStatus != WorkItemStatus.IN_PROGRESS) {
                task.setStartTime(LocalDateTime.now());
                changes.append("Status Changed from ").append(currentStatus).append(" à IN_PROGRESS; ");
            }
            // Si on quitte IN_PROGRESS
            else if (currentStatus == WorkItemStatus.IN_PROGRESS && newStatus != WorkItemStatus.IN_PROGRESS) {
                if (task.getStartTime() != null) {
                    long minutes = ChronoUnit.MINUTES.between(task.getStartTime(), LocalDateTime.now());
                    if (minutes > 0) {
                        // Ajouter au totalTimeSpent
                        task.setTotalTimeSpent(task.getTotalTimeSpent() != null ? task.getTotalTimeSpent() + minutes : minutes);

                        // Enregistrer dans l'historique
                        TimeEntry timeEntry = new TimeEntry();
                        timeEntry.setWorkItem(task);
                        timeEntry.setDuration(minutes);
                        timeEntry.setAddedBy(updatedBy);
                        timeEntry.setAddedAt(LocalDateTime.now());
                        timeEntry.setType("travail");
                        task.getTimeEntries().add(timeEntry);
                        changes.append("Added Time: ").append(minutes).append(" minutes; ");
                    }
                    task.setStartTime(null); // Réinitialiser
                }
                changes.append("Status changed from IN_PROGRESS to ").append(newStatus).append("; ");
            }

            // Mettre à jour le statut
            task.setStatus(newStatus);

            // Si DONE, définir completedDate
            // Notifier Project-Service si la tâche est DONE
            if (taskDTO.getStatus() == WorkItemStatus.DONE && task.getUserStory() != null) {
                try {
                    logger.info("Notifying Project-Service for UserStory {}", task.getUserStory());
                    projectClient.checkAndUpdateUserStoryStatus(task.getProjectId(), task.getUserStory(), token);
                    logger.info("Successfully notified Project-Service for UserStory {}", task.getUserStory());
                } catch (Exception e) {
                    logger.error("Failed to notify Project-Service for UserStory {}: {}", task.getUserStory(), e.getMessage(), e);
                }
            }
        }

        if (taskDTO.getStatus() != null) {
            task.setStatus(taskDTO.getStatus());
            if (taskDTO.getStatus() == WorkItemStatus.DONE) {
                task.setCompletedDate(LocalDate.now());
            }
            Task savedTask = taskRepository.save(task);
            logger.info("After save: Task {} status={}", taskId, savedTask.getStatus());
            taskMapper.toDTO(savedTask); // Forcer le flush si nécessaire
            if (taskDTO.getStatus() == WorkItemStatus.DONE) {
                notifyUserStoryStatusUpdate(task.getProjectId(), task.getUserStory(), token);
            }
        }

        // Update fields
        if (taskDTO.getTitle() != null && !taskDTO.getTitle().equals(task.getTitle())) {
            changes.append("Titre changé de '").append(task.getTitle()).append("' à '").append(taskDTO.getTitle()).append("'; ");
            task.setTitle(taskDTO.getTitle());
        }
        if (taskDTO.getDescription() != null && !taskDTO.getDescription().equals(task.getDescription())) {
            changes.append("Description modifiée; ");
            task.setDescription(taskDTO.getDescription());
        }
        if (taskDTO.getDueDate() != null && !taskDTO.getDueDate().equals(task.getDueDate())) {
            changes.append("Date d'échéance changée à ").append(taskDTO.getDueDate()).append("; ");
            task.setDueDate(taskDTO.getDueDate());
        }
        if (taskDTO.getPriority() != null && taskDTO.getPriority() != task.getPriority()) {
            changes.append("Priorité changée de ").append(task.getPriority()).append(" à ").append(taskDTO.getPriority()).append("; ");
            task.setPriority(taskDTO.getPriority());
        }
        if (taskDTO.getEstimationTime() != null && !taskDTO.getEstimationTime().equals(task.getEstimationTime())) {
            changes.append("Estimation changée à ").append(taskDTO.getEstimationTime()).append(" minutes; ");
            task.setEstimationTime(taskDTO.getEstimationTime());
        }
        if (taskDTO.getStartDate() != null && !taskDTO.getStartDate().equals(task.getStartDate())) {
            changes.append("Date de début changée à ").append(taskDTO.getStartDate()).append("; ");
            task.setStartDate(taskDTO.getStartDate());
        }
        if (taskDTO.getStatus() != null) {
            task.setStatus(taskDTO.getStatus());
            if (taskDTO.getStatus() == WorkItemStatus.DONE) {
                task.setCompletedDate(LocalDate.now());
            }
            Task savedTask = taskRepository.save(task);
            logger.info("After save: Task {} status={}", taskId, savedTask.getStatus());
            taskRepository.flush(); // Forcer l'écriture en base
            entityManager.clear(); // Vider la session Hibernate
            logger.info("After flush and clear: Task {} status={}", taskId, savedTask.getStatus());
            if (taskDTO.getStatus() == WorkItemStatus.DONE) {
                notifyUserStoryStatusUpdate(task.getProjectId(), task.getUserStory(), token);
            }
        }
        task.setUpdatedBy(updatedBy);
        task.setLastModifiedDate(LocalDate.now());

        // Mise à jour des dépendances
        if (taskDTO.getDependencyIds() != null) {
            List<Task> dependencies = taskRepository.findAllById(taskDTO.getDependencyIds());
            if (dependencies.size() != taskDTO.getDependencyIds().size()) {
                throw new IllegalArgumentException("One or more dependency IDs are invalid");
            }
            // Vérifier les cycles
            checkForDependencyCycles(task, dependencies);
            task.setDependencies(dependencies);
        }

        // Update assigned user IDs only if explicitly provided and non-empty
        if (taskDTO.getAssignedUserIds() != null && !taskDTO.getAssignedUserIds().isEmpty()) {
            Set<String> newUserIds = new HashSet<>(taskDTO.getAssignedUserIds());
            if (!newUserIds.equals(task.getAssignedUserIds())) {
                changes.append("Utilisateurs assignés modifiés; ");
                task.setAssignedUserIds(newUserIds);
            }
        }

        // Update tags
        if (taskDTO.getTags() != null && !taskDTO.getTags().equals(task.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))) {
            changes.append("Tags modifiés; ");
            Set<Tag> tags = taskDTO.getTags().stream().map(tagName ->
                            tagRepository.findByName(tagName)
                                    .orElseGet(() -> tagRepository.save(new Tag())))
                    .collect(Collectors.toSet());
            task.setTags(tags);
        }

        if (changes.length() > 0) {
            logHistory(task, "MISE_A_JOUR", changes.toString(), updatedBy);
        }
        updateProgress(task);
        // Save the updated task
        Task updatedTask = taskRepository.save(task);
        taskRepository.flush(); // Forcer l'écriture
        entityManager.clear(); // Vider la session
        logger.info("After final save: Task {} status={}", taskId, updatedTask.getStatus());
        // Relire la tâche pour confirmer
        // Relire la tâche pour confirmer
        Task reloadedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found after save: " + taskId));
        logger.info("After reload: Task {} status={}", taskId, reloadedTask.getStatus());

        // Notifier Project-Service si la tâche est DONE
        if (taskDTO.getStatus() == WorkItemStatus.DONE && task.getUserStory() != null) {
            notifyUserStoryStatusUpdate(task.getProjectId(), task.getUserStory(), token);
        }
        // Convert to DTO
        TaskDTO responseDTO = taskMapper.toDTO(updatedTask);

        // Populate assignedUsers with user details
        if (updatedTask.getAssignedUserIds() != null && !updatedTask.getAssignedUserIds().isEmpty()) {
            try {
                List<String> userIdList = updatedTask.getAssignedUserIds().stream().collect(Collectors.toList());
                String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
                List<UserDTO> assignedUsers = authClient.getUsersByIds(authHeader, userIdList);
                responseDTO.setAssignedUsers(assignedUsers != null ? assignedUsers : Collections.emptyList());
            } catch (Exception e) {
                System.err.println("Failed to fetch user details: " + e.getMessage());
                responseDTO.setAssignedUsers(Collections.emptyList());
            }
        } else {
            responseDTO.setAssignedUsers(Collections.emptyList());
        }

        return responseDTO;
    }

    public TaskDTO updateTaskRateLimiterFallback(Long taskId, TaskDTO taskDTO, String token, Throwable t) {
        logger.error("RateLimiter fallback for updateTask: {}", t.getMessage());
        throw new RuntimeException("Rate limit exceeded for task update");
    }

    public TaskDTO updateTaskBulkheadFallback(Long taskId, TaskDTO taskDTO, String token, Throwable t) {
        logger.error("Bulkhead fallback for updateTask: {}", t.getMessage());
        throw new RuntimeException("Too many concurrent task update requests");
    }

    public TaskDTO updateTaskRetryFallback(Long taskId, TaskDTO taskDTO, String token, Throwable t) {
        logger.error("Retry fallback for updateTask: {}", t.getMessage());
        throw new RuntimeException("Failed to update task after retries");
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "updateTaskByCommitRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "updateTaskByCommitBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "updateTaskByCommitRetryFallback")
    @Transactional
    public TaskDTO updateTask_bycommit(Long taskId, TaskDTO taskDTO) {
        // Validate token

        // Fetch existing task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with ID: " + taskId));

        // Valider les dépendances si le statut change à IN_PROGRESS ou DONE
        if (taskDTO.getStatus() != null &&
                (taskDTO.getStatus() == WorkItemStatus.IN_PROGRESS || taskDTO.getStatus() == WorkItemStatus.DONE)) {
            validateDependencies(task);
        }

// Suivre les changements pour l'historique
        StringBuilder changes = new StringBuilder();

        // Gérer le chronomètre pour le timeSpent
        if (taskDTO.getStatus() != null) {
            WorkItemStatus newStatus = taskDTO.getStatus();
            WorkItemStatus currentStatus = task.getStatus();

            // Si on passe à IN_PROGRESS, démarrer le chronomètre
            if (newStatus == WorkItemStatus.IN_PROGRESS && currentStatus != WorkItemStatus.IN_PROGRESS) {
                task.setStartTime(LocalDateTime.now());
                changes.append("Status Changed from ").append(currentStatus).append(" à IN_PROGRESS; ");
            }
            // Si on quitte IN_PROGRESS
            else if (currentStatus == WorkItemStatus.IN_PROGRESS && newStatus != WorkItemStatus.IN_PROGRESS) {
                if (task.getStartTime() != null) {
                    long minutes = ChronoUnit.MINUTES.between(task.getStartTime(), LocalDateTime.now());
                    if (minutes > 0) {
                        // Ajouter au totalTimeSpent
                        task.setTotalTimeSpent(task.getTotalTimeSpent() != null ? task.getTotalTimeSpent() + minutes : minutes);

                        // Enregistrer dans l'historique
                        TimeEntry timeEntry = new TimeEntry();
                        timeEntry.setWorkItem(task);
                        timeEntry.setDuration(minutes);
                        timeEntry.setAddedAt(LocalDateTime.now());
                        timeEntry.setType("travail");
                        task.getTimeEntries().add(timeEntry);
                        changes.append("Added Time: ").append(minutes).append(" minutes; ");
                    }
                    task.setStartTime(null); // Réinitialiser
                }
                changes.append("Status changed from IN_PROGRESS to ").append(newStatus).append("; ");
            }

            // Mettre à jour le statut
            task.setStatus(newStatus);

            // Si DONE, définir completedDate
            // Notifier Project-Service si la tâche est DONE

        }


        // Update fields
        if (taskDTO.getTitle() != null && !taskDTO.getTitle().equals(task.getTitle())) {
            changes.append("Titre changé de '").append(task.getTitle()).append("' à '").append(taskDTO.getTitle()).append("'; ");
            task.setTitle(taskDTO.getTitle());
        }
        if (taskDTO.getDescription() != null && !taskDTO.getDescription().equals(task.getDescription())) {
            changes.append("Description modifiée; ");
            task.setDescription(taskDTO.getDescription());
        }
        if (taskDTO.getDueDate() != null && !taskDTO.getDueDate().equals(task.getDueDate())) {
            changes.append("Date d'échéance changée à ").append(taskDTO.getDueDate()).append("; ");
            task.setDueDate(taskDTO.getDueDate());
        }
        if (taskDTO.getPriority() != null && taskDTO.getPriority() != task.getPriority()) {
            changes.append("Priorité changée de ").append(task.getPriority()).append(" à ").append(taskDTO.getPriority()).append("; ");
            task.setPriority(taskDTO.getPriority());
        }
        if (taskDTO.getEstimationTime() != null && !taskDTO.getEstimationTime().equals(task.getEstimationTime())) {
            changes.append("Estimation changée à ").append(taskDTO.getEstimationTime()).append(" minutes; ");
            task.setEstimationTime(taskDTO.getEstimationTime());
        }
        if (taskDTO.getStartDate() != null && !taskDTO.getStartDate().equals(task.getStartDate())) {
            changes.append("Date de début changée à ").append(taskDTO.getStartDate()).append("; ");
            task.setStartDate(taskDTO.getStartDate());
        }

        task.setLastModifiedDate(LocalDate.now());

        // Mise à jour des dépendances
        if (taskDTO.getDependencyIds() != null) {
            List<Task> dependencies = taskRepository.findAllById(taskDTO.getDependencyIds());
            if (dependencies.size() != taskDTO.getDependencyIds().size()) {
                throw new IllegalArgumentException("One or more dependency IDs are invalid");
            }
            // Vérifier les cycles
            checkForDependencyCycles(task, dependencies);
            task.setDependencies(dependencies);
        }

        // Update assigned user IDs only if explicitly provided and non-empty
        if (taskDTO.getAssignedUserIds() != null && !taskDTO.getAssignedUserIds().isEmpty()) {
            Set<String> newUserIds = new HashSet<>(taskDTO.getAssignedUserIds());
            if (!newUserIds.equals(task.getAssignedUserIds())) {
                changes.append("Utilisateurs assignés modifiés; ");
                task.setAssignedUserIds(newUserIds);
            }
        }

        // Update tags
        if (taskDTO.getTags() != null && !taskDTO.getTags().equals(task.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))) {
            changes.append("Tags modifiés; ");
            Set<Tag> tags = taskDTO.getTags().stream().map(tagName ->
                            tagRepository.findByName(tagName)
                                    .orElseGet(() -> tagRepository.save(new Tag())))
                    .collect(Collectors.toSet());
            task.setTags(tags);
        }

        if (changes.length() > 0) {
            logHistory_bycommit(task, "MISE_A_JOUR", changes.toString());
        }
        updateProgress(task);
        // Save the updated task
        Task updatedTask = taskRepository.save(task);
        taskRepository.flush(); // Forcer l'écriture
        entityManager.clear(); // Vider la session
        logger.info("After final save: Task {} status={}", taskId, updatedTask.getStatus());
        // Relire la tâche pour confirmer
        // Relire la tâche pour confirmer
        Task reloadedTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found after save: " + taskId));
        logger.info("After reload: Task {} status={}", taskId, reloadedTask.getStatus());

        // Notifier Project-Service si la tâche est DONE

        // Convert to DTO
        TaskDTO responseDTO = taskMapper.toDTO(updatedTask);



        return responseDTO;
    }

    public TaskDTO updateTaskByCommitRateLimiterFallback(Long taskId, TaskDTO taskDTO, Throwable t) {
        logger.error("RateLimiter fallback for updateTask_bycommit: {}", t.getMessage());
        throw new RuntimeException("Rate limit exceeded for task update by commit");
    }

    public TaskDTO updateTaskByCommitBulkheadFallback(Long taskId, TaskDTO taskDTO, Throwable t) {
        logger.error("Bulkhead fallback for updateTask_bycommit: {}", t.getMessage());
        throw new RuntimeException("Too many concurrent task update by commit requests");
    }

    public TaskDTO updateTaskByCommitRetryFallback(Long taskId, TaskDTO taskDTO, Throwable t) {
        logger.error("Retry fallback for updateTask_bycommit: {}", t.getMessage());
        throw new RuntimeException("Failed to update task by commit after retries");
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "getTaskByIdRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "getTaskByIdBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "getTaskByIdRetryFallback")
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

    public TaskDTO getTaskByIdRateLimiterFallback(Long projectId, Long userStoryId, Long taskId, String token, Throwable t) {
        logger.error("RateLimiter fallback for getTaskById: {}", t.getMessage());
        throw new RuntimeException("Rate limit exceeded for fetching task");
    }

    public TaskDTO getTaskByIdBulkheadFallback(Long projectId, Long userStoryId, Long taskId, String token, Throwable t) {
        logger.error("Bulkhead fallback for getTaskById: {}", t.getMessage());
        throw new RuntimeException("Too many concurrent task fetch requests");
    }

    public TaskDTO getTaskByIdRetryFallback(Long projectId, Long userStoryId, Long taskId, String token, Throwable t) {
        logger.error("Retry fallback for getTaskById: {}", t.getMessage());
        throw new RuntimeException("Failed to fetch task after retries");
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "getTaskByTaskIdRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "getTaskByTaskIdBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "getTaskByTaskIdRetryFallback")
    @Transactional(readOnly = true)
    public TaskDTO getTaskByTaskId(Long taskId, String token) {
        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with ID: " + taskId));
        return taskMapper.toDTO(task);
    }

    public TaskDTO getTaskByTaskIdRateLimiterFallback(Long taskId, String token, Throwable t) {
        logger.error("RateLimiter fallback for getTaskByTaskId: {}", t.getMessage());
        throw new RuntimeException("Rate limit exceeded for fetching task");
    }

    public TaskDTO getTaskByTaskIdBulkheadFallback(Long taskId, String token, Throwable t) {
        logger.error("Bulkhead fallback for getTaskByTaskId: {}", t.getMessage());
        throw new RuntimeException("Too many concurrent task fetch requests");
    }

    public TaskDTO getTaskByTaskIdRetryFallback(Long taskId, String token, Throwable t) {
        logger.error("Retry fallback for getTaskByTaskId: {}", t.getMessage());
        throw new RuntimeException("Failed to fetch task after retries");
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "deleteTaskRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "deleteTaskBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "deleteTaskRetryFallback")
    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with ID: " + taskId));

        // Enregistrer l'historique pour la suppression
        logHistory(task, "SUPPRESSION", "Tâche supprimée: " + task.getTitle(), task.getUpdatedBy() != null ? task.getUpdatedBy() : "Système");
        List<Task> dependentTasks = taskRepository.findByDependenciesId(taskId);
        for (Task dependent : dependentTasks) {
            dependent.getDependencies().remove(task);
            updateProgress(dependent);
            taskRepository.save(dependent);
        }
        taskRepository.delete(task);
    }
    public void deleteTaskRateLimiterFallback(Long taskId, Throwable t) {
        logger.error("RateLimiter fallback for deleteTask: {}", t.getMessage());
        throw new RuntimeException("Rate limit exceeded for task deletion");
    }

    public void deleteTaskBulkheadFallback(Long taskId, Throwable t) {
        logger.error("Bulkhead fallback for deleteTask: {}", t.getMessage());
        throw new RuntimeException("Too many concurrent task deletion requests");
    }

    public void deleteTaskRetryFallback(Long taskId, Throwable t) {
        logger.error("Retry fallback for deleteTask: {}", t.getMessage());
        throw new RuntimeException("Failed to delete task after retries");
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "getTasksByProjectAndUserStoryRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "getTasksByProjectAndUserStoryBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "getTasksByProjectAndUserStoryRetryFallback")
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProjectAndUserStory(Long projectId, Long userStoryId, String token) {
        // Validate token
        String createdBy = authClient.decodeToken(token);
        if (createdBy == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        // Fetch tasks
        entityManager.clear(); // Vider la session Hibernate
        List<Task> tasks = taskRepository.findByProjectIdAndUserStory(projectId, userStoryId);
        logger.info("Retrieved {} tasks for projectId={} and userStoryId={}", tasks.size(), projectId, userStoryId);
        tasks.forEach(task -> logger.info("Task id={} status={}", task.getId(), task.getStatus()));

        // Débogage : Vérifier directement dans la base
        Query debugQuery = entityManager.createNativeQuery(
                "SELECT id, status FROM WorkItem WHERE id = :taskId AND projectId = :projectId AND userStory = :userStoryId");
        debugQuery.setParameter("taskId", 20L);
        debugQuery.setParameter("projectId", projectId);
        debugQuery.setParameter("userStoryId", userStoryId);
        List<Object[]> debugResult = debugQuery.getResultList();
        debugResult.forEach(row -> logger.info("Debug DB: Task id={} status={}", row[0], row[1]));
        // Convert to DTOs
        return tasks.stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByProjectAndUserStoryRateLimiterFallback(Long projectId, Long userStoryId, String token, Throwable t) {
        logger.error("RateLimiter fallback for getTasksByProjectAndUserStory: {}", t.getMessage());
        return Collections.emptyList();
    }

    public List<TaskDTO> getTasksByProjectAndUserStoryBulkheadFallback(Long projectId, Long userStoryId, String token, Throwable t) {
        logger.error("Bulkhead fallback for getTasksByProjectAndUserStory: {}", t.getMessage());
        return Collections.emptyList();
    }

    public List<TaskDTO> getTasksByProjectAndUserStoryRetryFallback(Long projectId, Long userStoryId, String token, Throwable t) {
        logger.error("Retry fallback for getTasksByProjectAndUserStory: {}", t.getMessage());
        return Collections.emptyList();
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "getTasksByProjectIdRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "getTasksByProjectIdBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "getTasksByProjectIdRetryFallback")
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

    public List<TaskDTO> getTasksByProjectIdRateLimiterFallback(Long projectId, String token, Throwable t) {
        logger.error("RateLimiter fallback for getTasksByProjectId: {}", t.getMessage());
        return Collections.emptyList();
    }

    public List<TaskDTO> getTasksByProjectIdBulkheadFallback(Long projectId, String token, Throwable t) {
        logger.error("Bulkhead fallback for getTasksByProjectId: {}", t.getMessage());
        return Collections.emptyList();
    }

    public List<TaskDTO> getTasksByProjectIdRetryFallback(Long projectId, String token, Throwable t) {
        logger.error("Retry fallback for getTasksByProjectId: {}", t.getMessage());
        return Collections.emptyList();
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "getTasksOfActiveSprintRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "getTasksOfActiveSprintBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "getTasksOfActiveSprintRetryFallback")
    public List<TaskDTO> getTasksOfActiveSprint(Long projectId, String token) {
        // Log initial pour indiquer que la méthode est appelée
        System.out.println("Received request to fetch tasks for active sprint of project ID: " + projectId);

        // Log pour l'appel au microservice pour obtenir les UserStory IDs
        System.out.println("Fetching active sprint user stories for project ID: " + projectId);

        List<Long> activeStoryIds = projectClient.getUserStoriesOfActiveSprint(projectId);

        // Log des userStoryIds récupérés
        if (activeStoryIds.isEmpty()) {
            System.out.println("No active story IDs found for project ID: " + projectId);
            return Collections.emptyList();
        }

        System.out.println("Found " + activeStoryIds.size() + " active story IDs for project ID: " + projectId);

        // Recherche des tâches associées aux UserStories récupérées
        List<Task> tasks = taskRepository.findByUserStoryIn(activeStoryIds);

        // Log du nombre de tâches trouvées
        System.out.println("Found " + tasks.size() + " tasks associated with the active sprint for project ID: " + projectId);

        // Si aucune tâche n'est trouvée, loguer et retourner une liste vide
        if (tasks.isEmpty()) {
            System.out.println("No tasks found for the active sprint of project ID: " + projectId);
            return Collections.emptyList();
        }

        // Récupérer les détails des utilisateurs
        System.out.println("Fetching user details for assigned users");

        // Extraire tous les userIds uniques
        Set<String> allUserIds = tasks.stream()
                .flatMap(task -> task.getAssignedUserIds().stream())
                .collect(Collectors.toSet());

        List<UserDTO> users = Collections.emptyList();
        if (!allUserIds.isEmpty()) {
            try {
                users = authClient.getUsersByIds(token, new ArrayList<>(allUserIds));
                System.out.println("Successfully fetched " + users.size() + " user details");
            } catch (Exception e) {
                System.err.println("Error fetching user details: " + e.getMessage());
                // Continuer avec une liste vide pour ne pas bloquer la réponse
            }
        }

        // Créer une map pour accès rapide
        Map<String, UserDTO> userMap = users.stream()
                .collect(Collectors.toMap(UserDTO::getId, user -> user));

        // Log avant de convertir les entités Task en DTOs
        System.out.println("Converting " + tasks.size() + " tasks to DTOs for project ID: " + projectId);

        // Conversion des entités Task en DTOs
        List<TaskDTO> taskDTOs = tasks.stream()
                .map(task -> {
                    TaskDTO dto = taskMapper.toDTO(task);
                    // Ajouter les détails des utilisateurs
                    dto.setAssignedUsers(task.getAssignedUserIds().stream()
                            .map(userMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());

        // Log après la conversion
        System.out.println("Successfully converted " + taskDTOs.size() + " tasks to DTOs for project ID: " + projectId);

        // Retourne la liste des DTOs de tâches
        return taskDTOs;
    }


    public List<TaskDTO> getTasksOfActiveSprintRateLimiterFallback(Long projectId, String token, Throwable t) {
        logger.error("RateLimiter fallback for getTasksOfActiveSprint: {}", t.getMessage());
        return Collections.emptyList();
    }

    public List<TaskDTO> getTasksOfActiveSprintBulkheadFallback(Long projectId, String token, Throwable t) {
        logger.error("Bulkhead fallback for getTasksOfActiveSprint: {}", t.getMessage());
        return Collections.emptyList();
    }

    public List<TaskDTO> getTasksOfActiveSprintRetryFallback(Long projectId, String token, Throwable t) {
        logger.error("Retry fallback for getTasksOfActiveSprint: {}", t.getMessage());
        return Collections.emptyList();
    }


    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "attachFileToTaskRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "attachFileToTaskBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "attachFileToTaskRetryFallback")
    @Transactional
    public Task attachFileToTask(Long taskId, MultipartFile file, String token) throws IOException {
        logger.info("Attaching file to task ID: {}, file: {}", taskId, file.getOriginalFilename());

        String uploadedBy = authClient.decodeToken(token);
        logger.info("Decoded token, uploadedBy: {}", uploadedBy);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));
        logger.info("Found task: {}", task);

        if (file.isEmpty()) {
            logger.error("Uploaded file is empty");
            throw new IllegalArgumentException("File cannot be empty");
        }

        Map uploadResult = cloudinaryService.uploadFile(file);
        logger.info("Uploaded file to Cloudinary: secure_url={}, public_id={}",
                uploadResult.get("secure_url"), uploadResult.get("public_id"));

        String contentType = file.getContentType();
        if (contentType == null) {
            logger.warn("Content type is null, defaulting to application/octet-stream");
            contentType = "application/octet-stream";
        }

        FileAttachment attachment = new FileAttachment(
                file.getOriginalFilename(),
                contentType,
                file.getSize(),
                uploadResult.get("secure_url").toString(), // Store original secure_url
                uploadResult.get("public_id").toString(),
                uploadedBy
        );
        attachment.setUploadedAt(LocalDateTime.now());
        logger.info("Created attachment: {}", attachment);

        task.getAttachments().add(attachment);
        logger.info("Added attachment, attachments count: {}", task.getAttachments().size());

        logHistory(task, "AJOUT_PIECE_JOINTE", "Pièce jointe ajoutée: " + file.getOriginalFilename(), uploadedBy);

        taskRepository.saveAndFlush(task);
        logger.info("Saved task: {}", task);

        return task;
    }
    public Task attachFileToTaskRateLimiterFallback(Long taskId, MultipartFile file, String token, Throwable t) throws IOException {
        logger.error("RateLimiter fallback for attachFileToTask: {}", t.getMessage());
        throw new RuntimeException("Rate limit exceeded for file attachment");
    }

    public Task attachFileToTaskBulkheadFallback(Long taskId, MultipartFile file, String token, Throwable t) throws IOException {
        logger.error("Bulkhead fallback for attachFileToTask: {}", t.getMessage());
        throw new RuntimeException("Too many concurrent file attachment requests");
    }

    public Task attachFileToTaskRetryFallback(Long taskId, MultipartFile file, String token, Throwable t) throws IOException {
        logger.error("Retry fallback for attachFileToTask: {}", t.getMessage());
        throw new RuntimeException("Failed to attach file after retries");
    }

    @RateLimiter(name = "TaskServiceLimiter", fallbackMethod = "deleteFileFromTaskRateLimiterFallback")
    @Bulkhead(name = "TaskServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "deleteFileFromTaskBulkheadFallback")
    @Retry(name = "TaskServiceRetry", fallbackMethod = "deleteFileFromTaskRetryFallback")
    @Transactional
    public void deleteFileFromTask(String publicId, String token) throws IOException {
        logger.info("Deleting file with publicId: {}", publicId);

        // Find the FileAttachment
        FileAttachment attachment = fileAttachmentRepository.findByPublicId(publicId)
                .orElseThrow(() -> {
                    logger.warn("No file attachment found with publicId: {}", publicId);
                    return new IllegalArgumentException("File attachment not found with publicId: " + publicId);
                });

        // Find the Task(s) referencing this attachment
        Task task = taskRepository.findByAttachmentsContaining(attachment)
                .orElseThrow(() -> {
                    logger.warn("No task found with attachment publicId: {}", publicId);
                    return new IllegalArgumentException("No task found with attachment publicId: " + publicId);
                });

        // Remove the attachment from the Task
        task.getAttachments().remove(attachment);
        logger.info("Removed attachment {} from task {}", attachment.getId(), task.getId());
        // Delete from Cloudinary
        try {
            cloudinaryService.deleteFile(publicId);
            logger.info("Deleted file from Cloudinary with publicId: {}", publicId);
        } catch (IOException e) {
            logger.error("Failed to delete file from Cloudinary with publicId: {}", publicId, e);
            throw new IOException("Failed to delete file from Cloudinary: " + e.getMessage(), e);
        }

        // Save the updated Task to update relationships
        taskRepository.save(task);
        logger.info("Saved task {} after removing attachment", task.getId());

        // Delete the FileAttachment (optional, as cascade should handle it)
        fileAttachmentRepository.delete(attachment);

        logger.info("Deleted file attachment from database with publicId: {}", publicId);
    }



    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats(Long projectId, String token) {
        // Fetch tasks for the active sprint
        List<Long> activeStoryIds = projectClient.getUserStoriesOfActiveSprint(projectId);
        if (activeStoryIds.isEmpty()) {
            return new DashboardStatsDTO(0, 0, 0, 0,Map.of(), Map.of());
        }

        // Calculate existing metrics
        long completedTasks = taskRepository.countByUserStoryInAndStatus(activeStoryIds, WorkItemStatus.DONE);
        long notCompletedTasks = taskRepository.countByUserStoryInAndStatusNot(activeStoryIds, WorkItemStatus.DONE);
        long overdueTasks = taskRepository.countOverdueByUserStoryIn(activeStoryIds, WorkItemStatus.DONE, LocalDate.now());
        long totalTasks = taskRepository.countByUserStoryIn(activeStoryIds);

        // Calculate tasks by status
        Map<String, Long> tasksByStatus = taskRepository.countTasksByStatus(activeStoryIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((WorkItemStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));

        Map<String, Long> tasksByPriority = taskRepository.countTasksByPriority(activeStoryIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((WorkItemPriority) row[0]).name(),
                        row -> (Long) row[1]
                ));

        // Return DTO with stats
        return new DashboardStatsDTO(completedTasks, notCompletedTasks, overdueTasks, totalTasks, tasksByStatus , tasksByPriority);
    }


    public void deleteFileFromTaskRateLimiterFallback(String publicId, String token, Throwable t) throws IOException {
        logger.error("RateLimiter fallback for deleteFileFromTask: {}", t.getMessage());
        throw new RuntimeException("Rate limit exceeded for file deletion");
    }

    public void deleteFileFromTaskBulkheadFallback(String publicId, String token, Throwable t) throws IOException {
        logger.error("Bulkhead fallback for deleteFileFromTask: {}", t.getMessage());
        throw new RuntimeException("Too many concurrent file deletion requests");
    }

    public void deleteFileFromTaskRetryFallback(String publicId, String token, Throwable t) throws IOException {
        logger.error("Retry fallback for deleteFileFromTask: {}", t.getMessage());
        throw new RuntimeException("Failed to delete file after retries");
    }

    @Transactional(readOnly = true)
    public List<TaskCalendarDTO> getTasksForCalendar(Long projectId, String token) {
        // Fetch tasks for the active sprint
        List<Long> activeStoryIds = projectClient.getUserStoriesOfActiveSprint(projectId);
        if (activeStoryIds.isEmpty()) {
            return List.of();
        }

        List<Task> tasks = taskRepository.findByUserStoryIn(activeStoryIds);

        // Map tasks to TaskCalendarDTO
        return tasks.stream()
                .map(task -> new TaskCalendarDTO(
                        task.getId(),
                        task.getTitle(),
                        task.getStartDate(),
                        task.getDueDate()
                ))
                .collect(Collectors.toList());
    }

    private void validateDependencies(Task task) {
        if (task.getDependencies() != null && !task.getDependencies().isEmpty()) {
            for (Task dependency : task.getDependencies()) {
                if (dependency.getStatus() != WorkItemStatus.DONE) {
                    throw new IllegalStateException("Cannot proceed: Dependency task ID " + dependency.getId() + " is not completed.");
                }
            }
        }
    }

    private void checkForDependencyCycles(Task task, List<Task> newDependencies) {
        Set<Long> visited = new HashSet<>();
        checkForCycles(task, newDependencies, visited);
    }

    private void checkForCycles(Task task, List<Task> dependencies, Set<Long> visited) {
        if (task.getId() != null && visited.contains(task.getId())) {
            throw new IllegalArgumentException("Dependency cycle detected involving task ID: " + task.getId());
        }
        visited.add(task.getId());
        for (Task dependency : dependencies) {
            if (dependency.getDependencies() != null) {
                checkForCycles(dependency, dependency.getDependencies(), visited);
            }
        }
        visited.remove(task.getId());
    }


    @Transactional(readOnly = true)
    public List<TimeEntryDTO> getTimeEntries(Long taskId, String token) {
        // Valider l'existence de la tâche
        getTaskByTaskId(taskId, token);

        // Récupérer l'entité Task
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with ID: " + taskId));

        // Convertir les TimeEntry en TimeEntryDTO
        return task.getTimeEntries().stream()
                .map(te -> {
                    TimeEntryDTO dto = new TimeEntryDTO();
                    dto.setId(te.getId());
                    dto.setDuration(te.getDuration());
                    dto.setAddedBy(te.getAddedBy());
                    dto.setAddedAt(te.getAddedAt());
                    dto.setType(te.getType());
                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Transactional
    public void addManualTimeEntry(Long taskId, Long duration, String type, String token) {
        String addedBy = authClient.decodeToken(token);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with ID: " + taskId));
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        task.setTotalTimeSpent(task.getTotalTimeSpent() != null ? task.getTotalTimeSpent() + duration : duration);
        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setWorkItem(task);
        timeEntry.setDuration(duration);
        timeEntry.setAddedBy(addedBy);
        timeEntry.setAddedAt(LocalDateTime.now());
        timeEntry.setType(type);
        task.getTimeEntries().add(timeEntry);
        logHistory(task, "AJOUT_TEMPS", "Temps ajouté: " + duration + " minutes (" + type + ")", addedBy);
        updateProgress(task);
        taskRepository.save(task);
    }


    private void updateProgress(Task task) {
        if (task.getStatus() == WorkItemStatus.TO_DO) {
            task.setProgress(0.0);
        } else if (task.getStatus() == WorkItemStatus.DONE) {
            task.setProgress(100.0);
        } else if (task.getStatus() == WorkItemStatus.IN_PROGRESS || task.getStatus() == WorkItemStatus.BLOCKED) {
            if (task.getEstimationTime() != null && task.getEstimationTime() > 0 && task.getTotalTimeSpent() != null) {
                double progress = (task.getTotalTimeSpent().doubleValue() / task.getEstimationTime()) * 100;
                task.setProgress(Math.min(progress, 90.0));
            } else {
                task.setProgress(0.0); // Par défaut si estimationTime est absent
            }
        }
    }




    // Nouvelle méthode : Ajouter une dépendance

    @Transactional
    public TaskDTO addDependency(Long taskId, Long dependencyId, String token) {
        logger.info("Adding dependency ID {} to task ID {}", dependencyId, taskId);

        // Validate token
        String updatedBy = authClient.decodeToken(token);
        if (updatedBy == null) {
            logger.error("Invalid token: unable to extract user");
            throw new IllegalArgumentException("Invalid authentication token");
        }

        // Verify tasks exist
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    logger.error("Task not found with ID: {}", taskId);
                    return new NoSuchElementException("Task with ID " + taskId + " not found");
                });
        Task dependency = taskRepository.findById(dependencyId)
                .orElseThrow(() -> {
                    logger.error("Dependency task not found with ID: {}", dependencyId);
                    return new NoSuchElementException("Dependency task with ID " + dependencyId + " not found");
                });

        // Prevent self-dependency
        if (taskId.equals(dependencyId)) {
            logger.error("A task cannot depend on itself: task ID {}", taskId);
            throw new IllegalArgumentException("A task cannot depend on itself");
        }

        // Verify same project
        if (!task.getProjectId().equals(dependency.getProjectId())) {
            logger.error("Dependency task ID {} is not in the same project as task ID {}", dependencyId, taskId);
            throw new IllegalArgumentException("Dependency task must belong to the same project");
        }

        // Optional: Verify same user story (uncomment if required)
        /*
        if (!task.getUserStory().equals(dependency.getUserStory())) {
            logger.error("Dependency task ID {} is not in the same user story as task ID {}", dependencyId, taskId);
            throw new IllegalArgumentException("Dependency task must belong to the same user story");
        }
        */

        // Check if dependency already exists
        if (task.getDependencies().stream().anyMatch(dep -> dep.getId().equals(dependencyId))) {
            logger.warn("Task ID {} is already a dependency of task ID {}", dependencyId, taskId);
            throw new IllegalArgumentException("Task ID " + dependencyId + " is already a dependency");
        }

        // Add dependency
        task.getDependencies().add(dependency);
        logHistory(task, "AJOUT_DEPENDANCE", "Dépendance ajoutée: Tâche ID " + dependencyId, updatedBy);

        logger.debug("Added dependency ID {} to task ID {}", dependencyId, taskId);

        // Check for dependency cycles
        try {
            checkForDependencyCycles(task, task.getDependencies());
        } catch (IllegalArgumentException e) {
            task.getDependencies().remove(dependency);
            logger.error("Failed to add dependency due to cycle: {}", e.getMessage());
            throw e;
        }

        // Update progress
        updateProgress(task);

        // Save and return
        Task updatedTask = taskRepository.save(task);
        logger.info("Successfully added dependency ID {} to task ID {}", dependencyId, taskId);
        return toTaskDTOWithUsers(updatedTask, token);
    }

    // Nouvelle méthode : Supprimer une dépendance
    @Transactional
    public TaskDTO removeDependency(Long taskId, Long dependencyId, String token) {
        logger.info("Removing dependency ID {} from task ID {}", dependencyId, taskId);

        // Valider le token
        String updatedBy = authClient.decodeToken(token);
        if (updatedBy == null) {
            logger.error("Invalid token: unable to extract user");
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        // Vérifier que la tâche existe
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    logger.error("Task not found with ID: {}", taskId);
                    return new NoSuchElementException("Task not found with ID: " + taskId);
                });

        // Trouver la dépendance à supprimer
        Task dependency = task.getDependencies().stream()
                .filter(dep -> dep.getId().equals(dependencyId))
                .findFirst()
                .orElseThrow(() -> {
                    logger.warn("Dependency ID {} is not a dependency of task ID {}", dependencyId, taskId);
                    return new IllegalArgumentException("Dependency ID " + dependencyId + " is not a dependency of task ID " + taskId);
                });

        // Supprimer la dépendance
        task.getDependencies().remove(dependency);
        logHistory(task, "SUPPRESSION_DEPENDANCE", "Dépendance supprimée: Tâche ID " + dependencyId, updatedBy);
        logger.debug("Removed dependency ID {} from task ID {}", dependencyId, taskId);

        // Mettre à jour progress
        updateProgress(task);

        // Sauvegarder la tâche
        Task updatedTask = taskRepository.save(task);
        logger.info("Successfully removed dependency ID {} from task ID {}", dependencyId, taskId);

        // Convertir en DTO et retourner
        TaskDTO responseDTO = taskMapper.toDTO(updatedTask);
        if (updatedTask.getAssignedUserIds() != null && !updatedTask.getAssignedUserIds().isEmpty()) {
            try {
                List<String> userIdList = updatedTask.getAssignedUserIds().stream().collect(Collectors.toList());
                String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
                List<UserDTO> assignedUsers = authClient.getUsersByIds(authHeader, userIdList);
                responseDTO.setAssignedUsers(assignedUsers != null ? assignedUsers : Collections.emptyList());
            } catch (Exception e) {
                logger.error("Failed to fetch user details: {}", e.getMessage());
                responseDTO.setAssignedUsers(Collections.emptyList());
            }
        } else {
            responseDTO.setAssignedUsers(Collections.emptyList());
        }
        return responseDTO;
    }


    @Transactional(readOnly = true)
    public List<TaskDTO> getPotentialDependencies(Long taskId, String token) {
        logger.info("Fetching potential dependencies for task ID {}", taskId);

        // Validate token
        String userId = authClient.decodeToken(token);
        if (userId == null) {
            logger.error("Invalid token: unable to extract user");
            throw new IllegalArgumentException("Invalid authentication token");
        }

        // Verify task exists
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    logger.error("Task not found with ID: {}", taskId);
                    return new NoSuchElementException("Task with ID " + taskId + " not found");
                });

        // Fetch all tasks in the same project, excluding the task itself and its current dependencies
        List<Task> potentialDependencies = taskRepository.findByProjectId(task.getProjectId())
                .stream()
                .filter(t -> !t.getId().equals(taskId))
                .filter(t -> task.getDependencies().stream().noneMatch(dep -> dep.getId().equals(t.getId())))
                .collect(Collectors.toList());

        // Convert to DTOs
        List<TaskDTO> taskDTOs = potentialDependencies.stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());

        logger.info("Found {} potential dependencies for task ID {}", taskDTOs.size(), taskId);
        return taskDTOs;
    }

    private TaskDTO toTaskDTOWithUsers(Task task, String token) {
        TaskDTO responseDTO = taskMapper.toDTO(task);
        if (task.getAssignedUserIds() != null && !task.getAssignedUserIds().isEmpty()) {
            try {
                List<String> userIdList = task.getAssignedUserIds().stream().collect(Collectors.toList());
                String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
                List<UserDTO> assignedUsers = authClient.getUsersByIds(authHeader, userIdList);
                responseDTO.setAssignedUsers(assignedUsers != null ? assignedUsers : Collections.emptyList());
            } catch (Exception e) {
                logger.error("Failed to fetch user details: {}", e.getMessage());
                responseDTO.setAssignedUsers(Collections.emptyList());
            }
        } else {
            responseDTO.setAssignedUsers(Collections.emptyList());
        }
        return responseDTO;
    }


    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByUserAndActiveSprints(String token) {
        logger.info("Fetching tasks for user in active sprints");
        String userId = authClient.decodeToken(token);
        if (userId == null) {
            logger.error("Invalid token: unable to extract user");
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }
        logger.info("User ID extracted: {}", userId);

        // Récupérer les projets de l'utilisateur
        ProjectResponseWithRoleDTO projectResponse = projectClient.getProjectsByUser(userId);
        List<ProjectDTO> userProjects = projectResponse.getProjects().stream()
                .map(project -> new ProjectDTO(
                        project.getId(),
                        project.getName(),
                        project.getDescription(),
                        project.getCreationDate() != null ? project.getCreationDate().toLocalDate() : null,
                        project.getStartDate() != null ? project.getStartDate().toLocalDate() : null,
                        project.getDeadline() != null ? project.getDeadline().toLocalDate() : null,
                        project.getStatus(),
                        project.getPhase(),
                        project.getPriority()
                ))
                .collect(Collectors.toList());
        if (userProjects.isEmpty()) {
            logger.info("No projects found for user ID: {}", userId);
            return Collections.emptyList();
        }
        logger.info("Found {} projects for user ID: {}", userProjects.size(), userId);

        // Récupérer les user stories des sprints actifs pour chaque projet
        List<Long> allActiveStoryIds = userProjects.stream()
                .map(project -> projectClient.getUserStoriesOfActiveSprint(project.getId()))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        if (allActiveStoryIds.isEmpty()) {
            logger.info("No active story IDs found for user ID: {}", userId);
            return Collections.emptyList();
        }
        logger.info("Found {} active story IDs for user ID: {}", allActiveStoryIds.size(), userId);

        // Récupérer les tâches assignées à l'utilisateur
        List<Task> tasks = taskRepository.findByUserStoryIn(allActiveStoryIds)
                .stream()
                .filter(task -> task.getAssignedUserIds().contains(userId))
                .collect(Collectors.toList());
        if (tasks.isEmpty()) {
            logger.info("No tasks found for user ID: {} in active sprints", userId);
            return Collections.emptyList();
        }
        logger.info("Found {} tasks for user ID: {}", tasks.size(), userId);

        // Récupérer les détails des utilisateurs assignés
        Set<String> allUserIds = tasks.stream()
                .flatMap(task -> task.getAssignedUserIds().stream())
                .collect(Collectors.toSet());
        List<UserDTO> users = Collections.emptyList();
        if (!allUserIds.isEmpty()) {
            try {
                users = authClient.getUsersByIds(token, new ArrayList<>(allUserIds));
                logger.info("Successfully fetched {} user details", users.size());
            } catch (Exception e) {
                logger.error("Error fetching user details: {}", e.getMessage());
            }
        }
        Map<String, UserDTO> userMap = users.stream()
                .collect(Collectors.toMap(UserDTO::getId, user -> user));

        // Convertir en TaskDTO
        List<TaskDTO> taskDTOs = tasks.stream()
                .map(task -> {
                    TaskDTO dto = taskMapper.toDTO(task);
                    dto.setAssignedUsers(task.getAssignedUserIds().stream()
                            .map(userMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
        logger.info("Successfully converted {} tasks to DTOs for user ID: {}", taskDTOs.size(), userId);
        return taskDTOs;
    }


    private void logHistory(WorkItem workItem, String action, String description, String userId) {
        WorkItemHistory history = new WorkItemHistory();
        history.setWorkItem(workItem);
        history.setAction(action);
        history.setDescription(description);
        history.setAuthorId(userId); // Stocker l'ID de l'utilisateur
        history.setTimestamp(LocalDateTime.now());
        workItem.getHistory().add(history);
    }


    private void logHistory_bycommit(WorkItem workItem, String action, String description) {
        WorkItemHistory history = new WorkItemHistory();
        history.setWorkItem(workItem);
        history.setAction(action);
        history.setDescription(description);
        history.setTimestamp(LocalDateTime.now());
        workItem.getHistory().add(history);
    }
    @Transactional(readOnly = true)
    public List<WorkItemHistoryDTO> getTaskHistory(Long taskId, String token) {
        String userId = authClient.decodeToken(token);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Tâche non trouvée avec l'ID: " + taskId));
        return task.getHistory().stream()
                .map(history -> new WorkItemHistoryDTO(
                        history.getId(),
                        history.getAction(),
                        history.getDescription(),
                        history.getAuthorId(), // Retourner l'ID pour compatibilité, mais sera remplacé par getTaskHistoryWithAuthorNames
                        history.getTimestamp()
                ))
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<WorkItemHistoryDTO> getTaskHistoryWithAuthorNames(Long taskId, String token) {
        String userId = authClient.decodeToken(token);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Tâche non trouvée avec l'ID: " + taskId));

        String bearerToken = token.startsWith("Bearer ") ? token : "Bearer " + token;

        // Cache pour éviter des appels multiples au service d'authentification
        Map<String, Map<String, Object>> userCache = new HashMap<>();

        return task.getHistory().stream()
                .map(history -> {
                    String authorId = history.getAuthorId();
                    Map<String, Object> userDetails = userCache.computeIfAbsent(authorId,
                            id -> authClient.getUserDetailsByAuthId(id, bearerToken)
                    );

                    String firstName = (String) userDetails.getOrDefault("firstName", "Inconnu");
                    String lastName = (String) userDetails.getOrDefault("lastName", "Inconnu");
                    String fullName = firstName + " " + lastName;

                    return new WorkItemHistoryDTO(
                            history.getId(),
                            history.getAction(),
                            history.getDescription(),
                            fullName, // Utiliser le nom complet
                            history.getTimestamp()
                    );
                })
                .collect(Collectors.toList());
    }


    public List<Task> getTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    public long countTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId).size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyUserStoryStatusUpdate(Long projectId, Long userStoryId, String token) {
        logger.info("Calling checkAndUpdateUserStoryStatus for UserStory {}", userStoryId);
        try {
            Thread.sleep(200); // Attendre 200ms pour propagation
            projectClient.checkAndUpdateUserStoryStatus(projectId, userStoryId, token);
            logger.info("Successfully updated UserStory {}", userStoryId);
        } catch (Exception e) {
            logger.error("Failed to update UserStory {}: {}", userStoryId, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProjectAndUserStoryInternal(Long projectId, Long userStoryId) {
        entityManager.clear(); // Vider la session Hibernate
        List<Task> tasks = taskRepository.findByProjectIdAndUserStory(projectId, userStoryId);
        logger.info("Retrieved {} tasks for projectId={} and userStoryId={}", tasks.size(), projectId, userStoryId);
        tasks.forEach(task -> logger.info("Task id={} status={}", task.getId(), task.getStatus()));

        // Débogage natif
        Query debugQuery = entityManager.createNativeQuery(
                "SELECT id, status FROM WorkItem WHERE id = :taskId AND projectId = :projectId AND userStory = :userStoryId");
        debugQuery.setParameter("taskId", 20L);
        debugQuery.setParameter("projectId", projectId);
        debugQuery.setParameter("userStoryId", userStoryId);
        List<Object[]> debugResult = debugQuery.getResultList();
        debugResult.forEach(row -> logger.info("Debug DB: Task id={} status={}", row[0], row[1]));

        return tasks.stream().map(taskMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public TaskDTO createTask_bycommit(Long projectId, Long userStoryId, TaskDTO taskDTO) {
        // Validate token

        // Set defaults
        if (taskDTO.getCreationDate() == null) {
            taskDTO.setCreationDate(LocalDate.now());
        }
        taskDTO.setProjectId(projectId);
        taskDTO.setUserStoryId(userStoryId);

        // Convert DTO to entity
        Task task = taskMapper.toEntity(taskDTO);

        // Fetch and set dependencies
        if (taskDTO.getDependencyIds() != null && !taskDTO.getDependencyIds().isEmpty()) {
            List<Task> dependencies = taskRepository.findAllById(taskDTO.getDependencyIds());
            if (dependencies.size() != taskDTO.getDependencyIds().size()) {
                throw new IllegalArgumentException("One or more dependency IDs are invalid");
            }
            checkForDependencyCycles(task, dependencies);
            task.setDependencies(dependencies);
        }
        // Directly set user IDs without validation
        if (taskDTO.getAssignedUserIds() != null) {
            System.out.println("➡️ Liste des userIds assignés reçue : " + taskDTO.getAssignedUserIds());

            Set<String> userIdsSet = new HashSet<>(taskDTO.getAssignedUserIds());
            System.out.println("➡️ Conversion en Set (suppression des doublons éventuels) : " + userIdsSet);

            task.setAssignedUserIds(userIdsSet);
            System.out.println("✅ Les userIds ont été assignés à la tâche : " + task.getAssignedUserIds());
        } else {
            System.out.println("⚠️ Aucun userId assigné reçu dans le taskDTO.");
        }


        // Gestion des tags
        if (taskDTO.getTags() != null && !taskDTO.getTags().isEmpty()) {
            logger.info("➡️ Tags reçus pour la tâche : {}", taskDTO.getTags());

            Set<Tag> tags = new HashSet<>();
            for (String tagName : taskDTO.getTags()) {
                logger.info("🔍 Vérification du tag : {}", tagName);
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            logger.info("➕ Tag non trouvé, création du tag : {}", tagName);
                            Tag newTag = new Tag();
                            newTag.setName(tagName);
                            newTag.setWorkItems(new HashSet<>()); // Initialiser la collection
                            return tagRepository.save(newTag); // Sauvegarder explicitement le tag
                        });
                tags.add(tag);
                logger.info("✅ Tag ajouté à la liste : {}", tag.getName());
            }

            // Réinitialiser les tags de la tâche
            task.setTags(new HashSet<>());
            // Associer les tags à la tâche et synchroniser la relation bidirectionnelle
            for (Tag tag : tags) {
                task.getTags().add(tag); // Ajouter au côté WorkItem
                tag.getWorkItems().add(task); // Synchroniser le côté Tag
                logger.info("🔗 Tag associé à la tâche : {}", tag.getName());
            }

            logger.info("✅ Tous les tags ont été associés à la tâche : {}", tags.stream().map(Tag::getName).collect(Collectors.toSet()));
        } else {
            logger.info("⚠️ Aucun tag reçu pour la tâche.");
        }

        // Enregistrer l'historique pour la création
        logHistory_bycommit(task, "CREATION", "Task created: " + task.getTitle());

        updateProgress(task);
        // Save the task
        Task savedTask = taskRepository.save(task);

        // Convert back to DTO and return
        return taskMapper.toDTO(savedTask);
    }
    private Map<String, Object> parseCommitMessage(String commitMessage) {
        Map<String, Object> result = new HashMap<>();
        Pattern pattern = Pattern.compile("\\b(fix|task|issue)\\s+'([^']+)'(?:\\s+#US(\\d+))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(commitMessage);
        if (matcher.find()) {
            String type = matcher.group(1).toLowerCase();
            String title = matcher.group(2);
            String userStoryId = matcher.group(3); // Peut être null si #USxxx absent
            result.put("type", type.equals("fix") || type.equals("task") ? "task" : "issue");
            result.put("title", title);
            if (userStoryId != null) {
                result.put("userStoryId", Long.parseLong(userStoryId));
            }
            logger.info("Parsed commit message: type={}, title={}, userStoryId={}",
                    result.get("type"), title, userStoryId != null ? userStoryId : "none");
        } else {
            logger.info("No match found for commit message: {}", commitMessage);
        }
        return result;
    }

    private void processCommit(Map<String, Object> commit, Long projectId) {
        String commitSha = (String) commit.get("sha");
        if (commitSha == null) {
            logger.info("Commit SHA is null for commit: {}", commit);
            return;
        }

        if (processedCommitRepository.existsByCommitShaAndProjectId(commitSha, projectId)) {
            logger.info("Commit {} already processed for project {}", commitSha, projectId);
            return;
        }

        String message = (String) ((Map<String, Object>) commit.get("commit")).get("message");
        Map<String, Object> parsed = parseCommitMessage(message);
        if (parsed.isEmpty()) {
            logger.info("No task or issue reference found in commit message: {}", message);
            return;
        }

        String type = (String) parsed.get("type");
        String title = (String) parsed.get("title");
        Long userStoryId = (Long) parsed.get("userStoryId");

        // Si aucun userStoryId n'est fourni, utiliser une user story par défaut
        if (userStoryId == null) {
            List<Long> userStoryIds = projectClient.getUserStoriesOfActiveSprint(projectId);
            if (userStoryIds != null && !userStoryIds.isEmpty()) {
                userStoryId = userStoryIds.get(0);
                logger.info("No user story ID in commit message, using default userStoryId: {}", userStoryId);
            } else {
                logger.info("No active user stories found for project {}, skipping commit: {}", projectId, message);
                return;
            }
        }

        if ("task".equals(type)) {
            List<Task> tasks = taskRepository.findByProjectIdAndUserStory(projectId, userStoryId);
            Optional<Task> matchingTask = tasks.stream()
                    .filter(task -> task.getTitle().equalsIgnoreCase(title))
                    .findFirst();
            if (matchingTask.isPresent()) {
                TaskDTO taskDTO = taskMapper.toDTO(matchingTask.get());
                taskDTO.setStatus(WorkItemStatus.DONE);
                updateTask_bycommit(matchingTask.get().getId(), taskDTO);
                logger.info("Updated task with title '{}' to DONE based on commit", title);
            } else {
                logger.info("Task with title '{}' not found, creating new task", title);
                createTaskFromCommit(projectId, userStoryId, title, message);
            }
        } else if ("issue".equals(type)) {
            createTaskFromCommit(projectId, userStoryId, title, message);
        }

        ProcessedCommit processedCommit = new ProcessedCommit();
        processedCommit.setCommitSha(commitSha);
        processedCommit.setProjectId(projectId);
        processedCommitRepository.save(processedCommit);
        logger.info("Marked commit {} as processed for project {}", commitSha, projectId);
    }

    private void createTaskFromCommit(Long projectId, Long userStoryId, String title, String commitMessage) {
        try {
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setTitle(title);
            taskDTO.setDescription("Created from commit: " + commitMessage);
            taskDTO.setStatus(WorkItemStatus.TO_DO);
            taskDTO.setPriority(WorkItemPriority.MEDIUM);
            taskDTO.setCreationDate(LocalDate.now());
            taskDTO.setProjectId(projectId);
            createTask_bycommit(projectId, userStoryId, taskDTO);
            logger.info("Created new task with title '{}' in project {} for user story {}", title, projectId, userStoryId);
        } catch (Exception e) {
            logger.error("Failed to create task for commit with title '{}': {}", title, e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void processCommitsForProjects() {
        logger.info("Starting scheduled commit processing for all projects");
        try {
            ResponseEntity<List<Long>> projectsResponse = projectClient.getActiveProjectIds();
            if (!projectsResponse.getStatusCode().is2xxSuccessful() || projectsResponse.getBody() == null) {
                logger.info("Failed to fetch active projects");
                return;
            }
            List<Long> projectIds = projectsResponse.getBody();

            for (Long projectId : projectIds) {
                ResponseEntity<Map<String, String>> repoResponse = projectClient.getGitHubRepository(projectId);
                if (!repoResponse.getStatusCode().is2xxSuccessful() || repoResponse.getBody() == null) {
                    logger.info("No GitHub repository linked for project: " + projectId);
                    continue;
                }
                String repoUrl = repoResponse.getBody().get("repositoryUrl");
                if (repoUrl == null || repoUrl.isEmpty()) {
                    logger.info("No GitHub repository linked for project: " + projectId);
                    continue;
                }

                Map<String, String> repoDetails = extractRepoDetails(repoUrl);
                String owner = repoDetails.get("owner");
                String repo = repoDetails.get("repo");
                if (owner == null || repo == null) {
                    logger.info("Invalid repository URL for project: " + projectId + ", URL: " + repoUrl);
                    continue;
                }

                ResponseEntity<Map<String, String>> userResponse = projectClient.getGitHubUserId(projectId);
                if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null || userResponse.getBody().get("userId") == null) {
                    logger.info("No GitHub user linked for project: " + projectId);
                    continue;
                }
                String userId = userResponse.getBody().get("userId");

                ResponseEntity<Object> commitsResponse = gitHubIntegrationClient.getCommitsByUserId(
                        owner, repo, userId, null, 30);
                if (commitsResponse.getStatusCode().is2xxSuccessful() && commitsResponse.getBody() != null) {
                    List<Map<String, Object>> commits = (List<Map<String, Object>>) commitsResponse.getBody();
                    if (commits.isEmpty()) {
                        logger.info("No new commits found for project: " + projectId + ", owner/repo: " + owner + "/" + repo);
                        continue;
                    }

                    for (Map<String, Object> commit : commits) {
                        processCommit(commit, projectId);
                    }
                } else {
                    logger.info("Failed to fetch commits for project: " + projectId + ", owner/repo: " + owner + "/" + repo);
                }
            }
            logger.info("Completed scheduled commit processing");
        } catch (Exception e) {
            logger.info("Error during scheduled commit processing: " + e.getMessage());
        }
    }

    private Map<String, String> extractRepoDetails(String repoUrl) {
        if (repoUrl == null || !repoUrl.startsWith("https://github.com/")) {
            return Map.of();
        }
        String[] parts = repoUrl.replace("https://github.com/", "").split("/");
        if (parts.length >= 2) {
            return Map.of("owner", parts[0], "repo", parts[1].replace(".git", ""));
        }
        return Map.of();
    }
}