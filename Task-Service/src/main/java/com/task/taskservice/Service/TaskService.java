package com.task.taskservice.Service;
import com.task.taskservice.DTO.*;
import com.task.taskservice.Entity.*;
import com.task.taskservice.Enumeration.WorkItemPriority;
import com.task.taskservice.Enumeration.WorkItemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.Configuration.ProjectClient;
import com.task.taskservice.Mapper.TaskMapper; // Uppercase Mapper
import com.task.taskservice.Repository.FileAttachmentRepository;
import com.task.taskservice.Repository.TagRepository;
import com.task.taskservice.Repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Task;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final AuthClient authClient;

    private final ProjectClient projectClient;

    private final FileAttachmentRepository fileAttachmentRepository;
    @Autowired
    private final  CloudinaryService cloudinaryService;
    @Autowired
    public TaskService(TaskRepository taskRepository,
                       TagRepository tagRepository, TaskMapper taskMapper, AuthClient authClient,ProjectClient projectClient , CloudinaryService cloudinaryService , FileAttachmentRepository fileAttachmentRepository) {
        this.taskRepository = taskRepository;
        this.tagRepository = tagRepository;
        this.taskMapper = taskMapper;
        this.authClient = authClient;
        this.projectClient = projectClient;
        this.cloudinaryService = cloudinaryService;
        this.fileAttachmentRepository = fileAttachmentRepository;
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
            System.out.println("➡️ Tags reçus pour la tâche : " + taskDTO.getTags());

            Set<Tag> tags = taskDTO.getTags().stream().map(tagName -> {
                System.out.println("🔍 Vérification du tag : " + tagName);

                // Vérifier si le tag existe
                return tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            System.out.println("➕ Tag non trouvé, création du tag : " + tagName);
                            Tag newTag = new Tag();
                            newTag.setName(tagName);
                            Tag savedTag = tagRepository.save(newTag);
                            System.out.println("✅ Tag créé et sauvegardé : " + savedTag.getName() + " (ID: " + savedTag.getId() + ")");
                            return savedTag;
                        });
            }).collect(Collectors.toSet());

            System.out.println("✅ Ensemble des tags associés (avant association à la tâche) : " + tags.stream().map(Tag::getName).collect(Collectors.toSet()));

            // Réinitialiser les tags pour éviter les doublons
            task.setTags(new HashSet<>());
            // Associer les tags à la tâche
            for (Tag tag : tags) {
                task.addTag(tag);
                System.out.println("🔗 Tag associé à la tâche : " + tag.getName());
            }

            System.out.println("✅ Tous les tags ont été associés à la tâche.");
        } else {
            System.out.println("⚠️ Aucun tag reçu pour la tâche.");
        }

        updateProgress(task);
        // Save the task
        Task savedTask = taskRepository.save(task);

        // Convert back to DTO and return
        return taskMapper.toDTO(savedTask);
    }



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


        // Gérer le chronomètre pour le timeSpent
        if (taskDTO.getStatus() != null) {
            WorkItemStatus newStatus = taskDTO.getStatus();
            WorkItemStatus currentStatus = task.getStatus();

            // Si on passe à IN_PROGRESS, démarrer le chronomètre
            if (newStatus == WorkItemStatus.IN_PROGRESS && currentStatus != WorkItemStatus.IN_PROGRESS) {
                task.setStartTime(LocalDateTime.now());
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
                        timeEntry.setTask(task);
                        timeEntry.setDuration(minutes);
                        timeEntry.setAddedBy(updatedBy);
                        timeEntry.setAddedAt(LocalDateTime.now());
                        timeEntry.setType("travail");
                        task.getTimeEntries().add(timeEntry);
                    }
                    task.setStartTime(null); // Réinitialiser
                }
            }

            // Mettre à jour le statut
            task.setStatus(newStatus);

            // Si DONE, définir completedDate
            if (newStatus == WorkItemStatus.DONE) {
                task.setCompletedDate(LocalDate.now());
            }
        }


        // Update fields
        if (taskDTO.getTitle() != null) task.setTitle(taskDTO.getTitle());
        if (taskDTO.getDescription() != null) task.setDescription(taskDTO.getDescription());
        if (taskDTO.getDueDate() != null) task.setDueDate(taskDTO.getDueDate());
        if (taskDTO.getPriority() != null) task.setPriority(taskDTO.getPriority());
        if (taskDTO.getStatus() != null) {
            task.setStatus(taskDTO.getStatus());
            // Gérer completedDate
            if (taskDTO.getStatus() == WorkItemStatus.DONE) {
                task.setCompletedDate(LocalDate.now());
            } else {
                task.setCompletedDate(null); // Réinitialiser si le statut change
            }
        }
        if (taskDTO.getEstimationTime() != null) task.setEstimationTime(taskDTO.getEstimationTime());
        if (taskDTO.getStartDate() != null) task.setStartDate(taskDTO.getStartDate());
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
            Set<String> userIdsSet = new HashSet<>(taskDTO.getAssignedUserIds());
            task.setAssignedUserIds(userIdsSet);
        } else {
            // Preserve existing assignedUserIds
            task.setAssignedUserIds(task.getAssignedUserIds() != null ? task.getAssignedUserIds() : new HashSet<>());
        }

        // Update tags
        if (taskDTO.getTags() != null) {
            Set<Tag> tags = taskDTO.getTags().stream().map(tagName ->
                    tagRepository.findByName(tagName)
                            .orElseGet(() -> tagRepository.save(new Tag()))
            ).collect(Collectors.toSet());
            task.setTags(tags);
        }
        updateProgress(task);
        // Save the updated task
        Task updatedTask = taskRepository.save(task);

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
    public TaskDTO getTaskByTaskId(Long taskId, String token) {
        String userId = authClient.decodeToken(token);
        if (userId == null) {
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with ID: " + taskId));
        return taskMapper.toDTO(task);
    }


    @Transactional
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with ID: " + taskId));
        List<Task> dependentTasks = taskRepository.findByDependenciesId(taskId);
        for (Task dependent : dependentTasks) {
            dependent.getDependencies().remove(task);
            updateProgress(dependent);
            taskRepository.save(dependent);
        }
        taskRepository.delete(task);
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

        taskRepository.saveAndFlush(task);
        logger.info("Saved task: {}", task);

        return task;
    }

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
        timeEntry.setTask(task);
        timeEntry.setDuration(duration);
        timeEntry.setAddedBy(addedBy);
        timeEntry.setAddedAt(LocalDateTime.now());
        timeEntry.setType(type);
        task.getTimeEntries().add(timeEntry);
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

        // Valider le token
        String updatedBy = authClient.decodeToken(token);
        if (updatedBy == null) {
            logger.error("Invalid token: unable to extract user");
            throw new IllegalArgumentException("Invalid token: unable to extract user");
        }

        // Vérifier que les tâches existent
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> {
                    logger.error("Task not found with ID: {}", taskId);
                    return new NoSuchElementException("Task not found with ID: " + taskId);
                });
        Task dependency = taskRepository.findById(dependencyId)
                .orElseThrow(() -> {
                    logger.error("Dependency task not found with ID: {}", dependencyId);
                    return new NoSuchElementException("Dependency task not found with ID: " + dependencyId);
                });

        // Vérifier que la dépendance n'est pas déjà présente
        if (task.getDependencies().stream().anyMatch(dep -> dep.getId().equals(dependencyId))) {
            logger.warn("Task ID {} is already a dependency of task ID {}", dependencyId, taskId);
            throw new IllegalArgumentException("Task ID " + dependencyId + " is already a dependency of task ID " + taskId);
        }

        // Vérifier que taskId et dependencyId ne sont pas identiques
        if (taskId.equals(dependencyId)) {
            logger.error("A task cannot depend on itself: task ID {}", taskId);
            throw new IllegalArgumentException("A task cannot depend on itself");
        }
        // Vérifier si les 2 taches sont dans le meme projet
        if (!dependency.getProjectId().equals(task.getProjectId())) {
            throw new IllegalArgumentException("Dependency task ID " + dependencyId + " does not belong to the same project");
        }
        // Ajouter la dépendance
        task.getDependencies().add(dependency);
        logger.debug("Added dependency ID {} to task ID {}", dependencyId, taskId);

        // Vérifier les cycles de dépendances
        try {
            checkForDependencyCycles(task, task.getDependencies());
        } catch (IllegalArgumentException e) {
            // Revertir l'ajout en cas de cycle
            task.getDependencies().remove(dependency);
            logger.error("Failed to add dependency due to cycle: {}", e.getMessage());
            throw e;
        }

        // Mettre à jour progress
        updateProgress(task);

        // Sauvegarder la tâche
        Task updatedTask = taskRepository.save(task);
        logger.info("Successfully added dependency ID {} to task ID {}", dependencyId, taskId);

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
}