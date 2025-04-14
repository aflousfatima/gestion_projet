package com.task.taskservice.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.task.taskservice.Configuration.AuthClient;
import com.task.taskservice.Configuration.ProjectClient;
import com.task.taskservice.DTO.TaskDTO; // Uppercase DTO
import com.task.taskservice.DTO.UserDTO;
import com.task.taskservice.Entity.FileAttachment;
import com.task.taskservice.Entity.Tag;
import com.task.taskservice.Entity.Task;
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

}