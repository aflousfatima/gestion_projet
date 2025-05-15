package com.project.project_service.Service;

import com.project.project_service.DTO.TaskDTO;
import com.project.project_service.DTO.UserStoryDTO;
import com.project.project_service.DTO.UserStoryRequest;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.Tag;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.Priority;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.UserStoryStatus;
import com.project.project_service.Enumeration.WorkItemStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.Repository.TagRepository;
import com.project.project_service.Repository.UserStoryRepository;
import com.project.project_service.config.AuthClient;
import com.project.project_service.config.TaskClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@EnableScheduling
public class UserStoryService {

    private static final Logger logger = LoggerFactory.getLogger(UserStoryService.class);
    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private AuthClient authClient;

    @Autowired
    private TaskClient taskClient;
    @Autowired
    private HistoryService historyService;

    @Autowired
    private SprintService sprintService;

    private UserStoryDTO convertToDTO(UserStory userStory) {
        return new UserStoryDTO(userStory);
    }

    @RateLimiter(name = "UserStoryServiceLimiter", fallbackMethod = "createUserStoryRateLimiterFallback")
    @Bulkhead(name = "UserStoryServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "createUserStoryBulkheadFallback")
    @Retry(name = "UserStoryServiceRetry", fallbackMethod = "createUserStoryRetryFallback")
    public UserStoryDTO createUserStory(Long projectId, UserStoryRequest request, String token) {
        System.out.println("Début de createUserStory pour projectId: " + projectId);
        System.out.println("Données reçues: " + request);

        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));

        UserStory userStory = new UserStory();
        userStory.setProject(projet);
        userStory.setTitle(request.title());
        userStory.setDescription(request.description());
        userStory.setPriority(Priority.valueOf(request.priority()));
        userStory.setEffortPoints(request.effortPoints());
        userStory.setCreatedBy(userIdStr);
        userStory.setStatus(UserStoryStatus.valueOf(request.status())); // Utiliser la valeur envoyée par le frontend
        userStory.setDependsOn(request.dependsOn() != null ? request.dependsOn() : new ArrayList<>());

        // Gestion des tags
        // Gestion des tags
        if (request.tags() != null) { // Correction ici : tags() au lieu de getTags()
            List<Tag> tags = request.tags().stream()
                    .map(tagName -> tagRepository.findByName(tagName)
                            .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                    .toList();
            userStory.setTags(tags);
        }
        UserStory savedStory = userStoryRepository.save(userStory);

        // Ajout de l'historique de création du sprint
        historyService.addUserStoryHistory(
                savedStory.getId(),
                "CREATE", // Action
                userIdStr, // Auteur
                "User Story  créé : " + savedStory.getTitle()
        );
        return new UserStoryDTO(savedStory);
    }

    public UserStoryDTO createUserStoryRateLimiterFallback(Long projectId, UserStoryRequest request, String token, Throwable t) {
        System.out.println("RateLimiter fallback for createUserStory: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for creating user story");
    }

    public UserStoryDTO createUserStoryBulkheadFallback(Long projectId, UserStoryRequest request, String token, Throwable t) {
        System.out.println("Bulkhead fallback for createUserStory: " + t.getMessage());
        throw new RuntimeException("Too many concurrent user story creation requests");
    }

    public UserStoryDTO createUserStoryRetryFallback(Long projectId, UserStoryRequest request, String token, Throwable t) {
        System.out.println("Retry fallback for createUserStory: " + t.getMessage());
        throw new RuntimeException("Failed to create user story after retries");
    }

    @RateLimiter(name = "UserStoryServiceLimiter", fallbackMethod = "updateUserStoryRateLimiterFallback")
    @Bulkhead(name = "UserStoryServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "updateUserStoryBulkheadFallback")
    @Retry(name = "UserStoryServiceRetry", fallbackMethod = "updateUserStoryRetryFallback")
    public UserStoryDTO updateUserStory(Long projectId, Long userStoryId, UserStoryRequest request, String token) {

        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            System.out.println("Étape 1 échouée: Token invalide ou utilisateur non identifié");
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> {
                    System.out.println("Étape 2 échouée: Projet non trouvé avec l'ID: " + projectId);
                    return new RuntimeException("Projet non trouvé avec l'ID: " + projectId);
                });

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> {
                    System.out.println("Étape 3 échouée: User Story non trouvée avec l'ID: " + userStoryId);
                    return new RuntimeException("User Story non trouvée avec l'ID: " + userStoryId);
                });

        if (!userStory.getProject().getId().equals(projectId)) {
            System.out.println("Étape 4 échouée: La User Story n’appartient pas au projet - Projet attendu: " + projectId + ", Projet trouvé: " + userStory.getProject().getId());
            throw new RuntimeException("La User Story n'appartient pas à ce projet");
        }

        userStory.setTitle(request.title());
        userStory.setDescription(request.description());
        userStory.setPriority(Priority.valueOf(request.priority()));
        userStory.setEffortPoints(request.effortPoints());
        userStory.setDependsOn(request.dependsOn() != null ? request.dependsOn() : userStory.getDependsOn());

        if (request.tags() != null) {
            List<Tag> tags = new ArrayList<>();
            for (String tagName : request.tags()) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            return tagRepository.save(new Tag(tagName));
                        });
                tags.add(tag);
            }
            userStory.setTags(tags);
            System.out.println("Tags mis à jour: " + userStory.getTags());
        }
        try {
            UserStory updatedStory = userStoryRepository.save(userStory);
            historyService.addUserStoryHistory(
                    updatedStory.getId(),
                    "UPDATE", // Action
                    userIdStr,
                    "User Story mise à jour : " + updatedStory.getTitle()
            );
            return new UserStoryDTO(updatedStory);
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Conflit de mise à jour détecté. Veuillez réessayer.", e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la User Story", e);
        }
    }

    public UserStoryDTO updateUserStoryRateLimiterFallback(Long projectId, Long userStoryId, UserStoryRequest request, String token, Throwable t) {
        System.out.println("RateLimiter fallback for updateUserStory: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for updating user story");
    }

    public UserStoryDTO updateUserStoryBulkheadFallback(Long projectId, Long userStoryId, UserStoryRequest request, String token, Throwable t) {
        System.out.println("Bulkhead fallback for updateUserStory: " + t.getMessage());
        throw new RuntimeException("Too many concurrent user story update requests");
    }

    public UserStoryDTO updateUserStoryRetryFallback(Long projectId, Long userStoryId, UserStoryRequest request, String token, Throwable t) {
        System.out.println("Retry fallback for updateUserStory: " + t.getMessage());
        throw new RuntimeException("Failed to update user story after retries");
    }

    @RateLimiter(name = "UserStoryServiceLimiter", fallbackMethod = "getUserStoriesByProjectIdRateLimiterFallback")
    @Bulkhead(name = "UserStoryServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "getUserStoriesByProjectIdBulkheadFallback")
    @Retry(name = "UserStoryServiceRetry", fallbackMethod = "getUserStoriesByProjectIdRetryFallback")
    public List<UserStoryDTO> getUserStoriesByProjectId(Long projectId) {
        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        List<UserStory> userStories = userStoryRepository.findByProject(projet);
        return userStories.stream().map(UserStoryDTO::new).toList();
    }


    public List<UserStoryDTO> getUserStoriesByProjectIdRateLimiterFallback(Long projectId, Throwable t) {
        System.out.println("RateLimiter fallback for getUserStoriesByProjectId: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<UserStoryDTO> getUserStoriesByProjectIdBulkheadFallback(Long projectId, Throwable t) {
        System.out.println("Bulkhead fallback for getUserStoriesByProjectId: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<UserStoryDTO> getUserStoriesByProjectIdRetryFallback(Long projectId, Throwable t) {
        System.out.println("Retry fallback for getUserStoriesByProjectId: " + t.getMessage());
        return new ArrayList<>();
    }

    @RateLimiter(name = "UserStoryServiceLimiter", fallbackMethod = "deleteUserStoryRateLimiterFallback")
    @Bulkhead(name = "UserStoryServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "deleteUserStoryBulkheadFallback")
    @Retry(name = "UserStoryServiceRetry", fallbackMethod = "deleteUserStoryRetryFallback")
    public void deleteUserStory(Long projectId, Long userStoryId) {
        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new RuntimeException("User Story non trouvée avec l'ID: " + userStoryId));
        if (!userStory.getProject().getId().equals(projectId)) {
            throw new RuntimeException("La User Story n'appartient pas à ce projet");
        }
        userStoryRepository.delete(userStory);
        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "DELETE", // Action
                userStory.getCreatedBy(), // Auteur
                "User Story supprimé : " + userStory.getTitle()
        );
    }

    public void deleteUserStoryRateLimiterFallback(Long projectId, Long userStoryId, Throwable t) {
        System.out.println("RateLimiter fallback for deleteUserStory: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for deleting user story");
    }

    public void deleteUserStoryBulkheadFallback(Long projectId, Long userStoryId, Throwable t) {
        System.out.println("Bulkhead fallback for deleteUserStory: " + t.getMessage());
        throw new RuntimeException("Too many concurrent user story deletion requests");
    }

    public void deleteUserStoryRetryFallback(Long projectId, Long userStoryId, Throwable t) {
        System.out.println("Retry fallback for deleteUserStory: " + t.getMessage());
        throw new RuntimeException("Failed to delete user story after retries");
    }

    @RateLimiter(name = "UserStoryServiceLimiter", fallbackMethod = "assignUserStoryToSprintRateLimiterFallback")
    @Bulkhead(name = "UserStoryServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "assignUserStoryToSprintBulkheadFallback")
    @Retry(name = "UserStoryServiceRetry", fallbackMethod = "assignUserStoryToSprintRetryFallback")
    public UserStoryDTO assignUserStoryToSprint(Long projectId, Long userStoryId, Long sprintId, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new RuntimeException("User Story non trouvée avec l'ID: " + userStoryId));

        if (!userStory.getProject().getId().equals(projectId)) {
            throw new RuntimeException("La User Story n'appartient pas à ce projet");
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint non trouvé avec l'ID: " + sprintId));

        if (!sprint.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Le Sprint n'appartient pas à ce projet");
        }

        int currentEffort = sprint.getUserStories().stream()
                .mapToInt(UserStory::getEffortPoints)
                .sum();
        if (currentEffort + userStory.getEffortPoints() > sprint.getCapacity()) {
            throw new RuntimeException("La capacité du sprint est insuffisante pour cette User Story");
        }

        // Vérifier les dépendances avant assignation
        if (!userStory.getDependsOn().isEmpty()) {
            for (Long depId : userStory.getDependsOn()) {
                UserStory depStory = userStoryRepository.findById(depId)
                        .orElseThrow(() -> new RuntimeException("Dépendance non trouvée : " + depId));
                if (!depStory.getStatus().equals(UserStoryStatus.DONE)) {
                    if (sprint.getStatus() == SprintStatus.ACTIVE && (depStory.getSprint() == null || depStory.getSprint().getStatus() != SprintStatus.ACTIVE)) {
                        throw new RuntimeException("La dépendance " + depStory.getTitle() + " doit être dans un sprint actif ou terminée");
                    }
                }
            }
        }
        userStory.setSprint(sprint);
        updateStatusBasedOnDependencies(userStory, token); // Recalculer le statut
        UserStory updatedUserStory = userStoryRepository.save(userStory);

        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "ASSIGN_TO_SPRINT", // Action
                userStory.getCreatedBy(), // Auteur
                "User Story attribuer au sprint : " + userStory.getTitle()
        );
        return new UserStoryDTO(updatedUserStory);
    }

    public UserStoryDTO assignUserStoryToSprintRateLimiterFallback(Long projectId, Long userStoryId, Long sprintId, String token, Throwable t) {
        System.out.println("RateLimiter fallback for assignUserStoryToSprint: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for assigning user story to sprint");
    }

    public UserStoryDTO assignUserStoryToSprintBulkheadFallback(Long projectId, Long userStoryId, Long sprintId, String token, Throwable t) {
        System.out.println("Bulkhead fallback for assignUserStoryToSprint: " + t.getMessage());
        throw new RuntimeException("Too many concurrent user story assignment requests");
    }

    public UserStoryDTO assignUserStoryToSprintRetryFallback(Long projectId, Long userStoryId, Long sprintId, String token, Throwable t) {
        System.out.println("Retry fallback for assignUserStoryToSprint: " + t.getMessage());
        throw new RuntimeException("Failed to assign user story to sprint after retries");
    }

    @RateLimiter(name = "UserStoryServiceLimiter", fallbackMethod = "removeUserStoryFromSprintRateLimiterFallback")
    @Bulkhead(name = "UserStoryServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "removeUserStoryFromSprintBulkheadFallback")
    @Retry(name = "UserStoryServiceRetry", fallbackMethod = "removeUserStoryFromSprintRetryFallback")
    public UserStoryDTO removeUserStoryFromSprint(Long projectId, Long userStoryId, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new RuntimeException("User Story non trouvée avec l'ID: " + userStoryId));

        if (!userStory.getProject().getId().equals(projectId)) {
            throw new RuntimeException("La User Story n'appartient pas à ce projet");
        }

        // Retirer le sprint (mettre à null)
        userStory.setSprint(null);
        userStory.setStatus(UserStoryStatus.BACKLOG); // Remettre à "BACKLOG" lorsqu'elle est retirée
        UserStory updatedUserStory = userStoryRepository.save(userStory);

        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "DELETE", // Action
                userStory.getCreatedBy(), // Auteur
                "User Story  retirer su sprint : " + userStory.getTitle()
        );
        return new UserStoryDTO(updatedUserStory);
    }
    public UserStoryDTO removeUserStoryFromSprintRateLimiterFallback(Long projectId, Long userStoryId, String token, Throwable t) {
        System.out.println("RateLimiter fallback for removeUserStoryFromSprint: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for removing user story from sprint");
    }

    public UserStoryDTO removeUserStoryFromSprintBulkheadFallback(Long projectId, Long userStoryId, String token, Throwable t) {
        System.out.println("Bulkhead fallback for removeUserStoryFromSprint: " + t.getMessage());
        throw new RuntimeException("Too many concurrent user story removal requests");
    }

    public UserStoryDTO removeUserStoryFromSprintRetryFallback(Long projectId, Long userStoryId, String token, Throwable t) {
        System.out.println("Retry fallback for removeUserStoryFromSprint: " + t.getMessage());
        throw new RuntimeException("Failed to remove user story from sprint after retries");
    }

    @RateLimiter(name = "UserStoryServiceLimiter", fallbackMethod = "updateDependenciesRateLimiterFallback")
    @Bulkhead(name = "UserStoryServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "updateDependenciesBulkheadFallback")
    @Retry(name = "UserStoryServiceRetry", fallbackMethod = "updateDependenciesRetryFallback")
    public UserStoryDTO updateDependencies(Long projectId, Long userStoryId, List<Long> newDependsOn, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new RuntimeException("User Story non trouvée avec l'ID: " + userStoryId));

        if (!userStory.getProject().getId().equals(projectId)) {
            throw new RuntimeException("La User Story n'appartient pas à ce projet");
        }

        // Mettre à jour les dépendances (ajout ou suppression)
        userStory.setDependsOn(newDependsOn != null ? newDependsOn : new ArrayList<>());

        // Recalculer le statut en fonction des dépendances
        updateStatusBasedOnDependencies(userStory, token);

        UserStory updatedStory = userStoryRepository.save(userStory);
        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "UPDATE_DEPENCENCIES", // Action
                userStory.getCreatedBy(), // Auteur
                "Modifier dependance entre les Users Stories : " + userStory.getTitle()
        );
        return new UserStoryDTO(updatedStory);
    }

    public UserStoryDTO updateDependenciesRateLimiterFallback(Long projectId, Long userStoryId, List<Long> newDependsOn, String token, Throwable t) {
        System.out.println("RateLimiter fallback for updateDependencies: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for updating dependencies");
    }

    public UserStoryDTO updateDependenciesBulkheadFallback(Long projectId, Long userStoryId, List<Long> newDependsOn, String token, Throwable t) {
        System.out.println("Bulkhead fallback for updateDependencies: " + t.getMessage());
        throw new RuntimeException("Too many concurrent dependency update requests");
    }

    public UserStoryDTO updateDependenciesRetryFallback(Long projectId, Long userStoryId, List<Long> newDependsOn, String token, Throwable t) {
        System.out.println("Retry fallback for updateDependencies: " + t.getMessage());
        throw new RuntimeException("Failed to update dependencies after retries");
    }

    @RateLimiter(name = "UserStoryServiceLimiter", fallbackMethod = "updateUserStoryStatusRateLimiterFallback")
    @Bulkhead(name = "UserStoryServiceBulkhead", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "updateUserStoryStatusBulkheadFallback")
    @Retry(name = "UserStoryServiceRetry", fallbackMethod = "updateUserStoryStatusRetryFallback")
    public UserStoryDTO updateUserStoryStatus(Long projectId, Long userStoryId, String newStatus, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new RuntimeException("User Story non trouvée avec l'ID: " + userStoryId));
        if (!userStory.getProject().getId().equals(projectId)) {
            throw new RuntimeException("La User Story n'appartient pas à ce projet");
        }

        UserStoryStatus status = UserStoryStatus.valueOf(newStatus);
        if (status == UserStoryStatus.BLOCKED && userStory.getDependsOn().isEmpty()) {
            throw new RuntimeException("Une US ne peut être BLOCKED sans dépendances non terminées");
        }
        if (status == UserStoryStatus.IN_PROGRESS && userStory.getSprint() == null) {
            throw new RuntimeException("Une US ne peut être IN_PROGRESS sans être dans un sprint actif");
        }
        userStory.setStatus(status);
        UserStory updatedStory = userStoryRepository.save(userStory);

        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "UPDATE_USER_STORY_STATUS", // Action
                userStory.getCreatedBy(), // Auteur
                "Modifier Status du User Story : " + userStory.getTitle()
        );
        return new UserStoryDTO(updatedStory);
    }
    public UserStoryDTO updateUserStoryStatusRateLimiterFallback(Long projectId, Long userStoryId, String newStatus, String token, Throwable t) {
        System.out.println("RateLimiter fallback for updateUserStoryStatus: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for updating user story status");
    }

    public UserStoryDTO updateUserStoryStatusBulkheadFallback(Long projectId, Long userStoryId, String newStatus, String token, Throwable t) {
        System.out.println("Bulkhead fallback for updateUserStoryStatus: " + t.getMessage());
        throw new RuntimeException("Too many concurrent user story status update requests");
    }

    public UserStoryDTO updateUserStoryStatusRetryFallback(Long projectId, Long userStoryId, String newStatus, String token, Throwable t) {
        System.out.println("Retry fallback for updateUserStoryStatus: " + t.getMessage());
        throw new RuntimeException("Failed to update user story status after retries");
    }

    public void updateStatusBasedOnDependencies(UserStory userStory, String token) {
        // Vérifier si toutes les tâches sont DONE
        List<TaskDTO> tasks = taskClient.getTasksByProjectAndUserStory(userStory.getProject().getId(), userStory.getId(), token);
        boolean allTasksDone = tasks.stream().allMatch(task -> task.getStatus() == WorkItemStatus.DONE);

        // Si toutes les tâches sont DONE, conserver DONE ou définir à DONE
        if (allTasksDone) {
            userStory.setStatus(UserStoryStatus.DONE);
            return;
        }

        // Si le statut actuel est DONE, ne pas le modifier
        if (userStory.getStatus() == UserStoryStatus.DONE) {
            return;
        }

        if (userStory.getDependsOn().isEmpty()) {
            if (userStory.getSprint() != null) {
                userStory.setStatus(userStory.getSprint().getStatus() == SprintStatus.ACTIVE ? UserStoryStatus.IN_PROGRESS : UserStoryStatus.SELECTED_FOR_SPRINT);
            } else if (!userStory.getStatus().equals(UserStoryStatus.CANCELED)) {
                userStory.setStatus(UserStoryStatus.BACKLOG);
            }
            return;
        }

        boolean allDependenciesDone = true;
        boolean allDependenciesInActiveSprint = true;

        for (Long depId : userStory.getDependsOn()) {
            UserStory depStory = userStoryRepository.findById(depId)
                    .orElseThrow(() -> new RuntimeException("Dépendance non trouvée : " + depId));
            if (!depStory.getStatus().equals(UserStoryStatus.DONE)) {
                allDependenciesDone = false;
                if (depStory.getSprint() == null || depStory.getSprint().getStatus() != SprintStatus.ACTIVE) {
                    allDependenciesInActiveSprint = false;
                }
            }
        }

        if (userStory.getSprint() == null) {
            userStory.setStatus(UserStoryStatus.BACKLOG);
        } else if (userStory.getSprint().getStatus() == SprintStatus.PLANNED) {
            if (allDependenciesDone) {
                userStory.setStatus(UserStoryStatus.SELECTED_FOR_SPRINT);
            } else {
                userStory.setStatus(UserStoryStatus.ON_HOLD);
            }
        } else if (userStory.getSprint().getStatus() == SprintStatus.ACTIVE) {
            if (allDependenciesDone) {
                userStory.setStatus(UserStoryStatus.IN_PROGRESS);
            } else if (allDependenciesInActiveSprint) {
                userStory.setStatus(UserStoryStatus.BLOCKED);
            } else {
                userStory.setStatus(UserStoryStatus.ON_HOLD);
            }
        }
    }


    public UserStoryDTO updateTags(Long projectId, Long userStoryId, List<String> tagNames, String authorizationHeader) {
        // Vérifiez l'autorisation ici si nécessaire...

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new RuntimeException("User story not found"));

        if (!userStory.getProject().getId().equals(projectId)) {
            throw new RuntimeException("User story does not belong to this project");
        }

        // Gestion des tags
        List<Tag> tags = new ArrayList<>();
        if (tagNames != null) {
            for (String tagName : tagNames) {
                Tag tag = tagRepository.findByName(tagName)
                        .orElseGet(() -> {
                            Tag newTag = new Tag(tagName);
                            return tagRepository.save(newTag);
                        });
                tags.add(tag);
            }
        }

        userStory.setTags(tags);
        UserStory updatedUserStory = userStoryRepository.save(userStory);

        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "UPDATE_TAG", // Action
                userStory.getCreatedBy(), // Auteur
                "Modifier le tag : " + userStory.getTitle()
        );
        // Conversion en DTO
        return convertToDTO(updatedUserStory);
    }

    public List<Long> getUserStoryIdsOfActiveSprint(Long projectId) {
        Sprint sprintActif = sprintRepository.findByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);
        if (sprintActif == null) return new ArrayList<>();

        return userStoryRepository.findBySprintId(sprintActif.getId())
                .stream()
                .map(UserStory::getId)
                .collect(Collectors.toList());
    }


    @Scheduled(fixedRate = 120000) // Toutes les minutes
    @Transactional
    public void checkStaleUserStories() {
        logger.info("Checking for stale UserStories");
        List<UserStory> inProgressStories = userStoryRepository.findByStatus(UserStoryStatus.IN_PROGRESS);
        for (UserStory story : inProgressStories) {
            logger.info("Checking UserStory {} in Project {}", story.getId(), story.getProject().getId());
            try {
                checkAndUpdateUserStoryStatus(story.getProject().getId(), story.getId(), null); // Pas de token pour tâche planifiée
            } catch (Exception e) {
                logger.error("Failed to check UserStory {}: {}", story.getId(), e.getMessage(), e);
            }
        }
    }

    public UserStoryDTO checkAndUpdateUserStoryStatus(Long projectId, Long userStoryId, String token) {
        logger.info("Starting checkAndUpdateUserStoryStatus for UserStory {} in Project {}", userStoryId, projectId);

        String userIdStr = token != null ? authClient.decodeToken(token) : "system-task"; // Utiliser system-task si pas de token
        if (userIdStr == null && token != null) {
            logger.error("Invalid token: unable to extract user ID");
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }
        logger.debug("Decoded user ID: {}", userIdStr);

        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> {
                    logger.error("User Story not found with ID: {}", userStoryId);
                    return new RuntimeException("User Story non trouvée avec l'ID: " + userStoryId);
                });
        logger.debug("Found UserStory: {} with status: {}", userStory.getTitle(), userStory.getStatus());

        if (!userStory.getProject().getId().equals(projectId)) {
            logger.error("User Story {} does not belong to Project {}", userStoryId, projectId);
            throw new RuntimeException("La User Story n'appartient pas à ce projet");
        }

        // Récupérer les tâches avec réessais
        List<TaskDTO> tasks = fetchTasksWithRetry(projectId, userStoryId, token);
        logger.info("Retrieved {} tasks for UserStory {}", tasks.size(), userStoryId);
        tasks.forEach(task -> logger.info("TaskDTO id={} status={} (name={})",
                task.getId(), task.getStatus(),
                task.getStatus() != null ? task.getStatus().name() : "null"));

        // Vérifier si toutes les tâches sont DONE
        boolean allTasksDone = tasks.stream()
                .allMatch(task -> task.getStatus() != null && task.getStatus() == WorkItemStatus.DONE);
        logger.info("All tasks done for UserStory {}: {}", userStoryId, allTasksDone);

        // Mettre à jour le statut si toutes les tâches sont DONE
        if (allTasksDone && !userStory.getStatus().equals(UserStoryStatus.DONE)) {
            logger.info("Updating UserStory {} status to DONE", userStoryId);
            userStory.setStatus(UserStoryStatus.DONE);
            UserStory updatedStory = userStoryRepository.save(userStory);
            logger.info("Saved UserStory {} with status DONE", userStoryId);

            historyService.addUserStoryHistory(
                    updatedStory.getId(),
                    "UPDATE_STATUS",
                    userIdStr,
                    "User Story marquée comme DONE : " + updatedStory.getTitle()
            );

            if (updatedStory.getSprint() != null) {
                logger.info("Checking sprint status for Sprint {} in Project {}", updatedStory.getSprint().getId(), projectId);
                sprintService.checkAndUpdateSprintStatus(projectId, updatedStory.getSprint().getId(), token != null ? token : null);
            }
            return new UserStoryDTO(updatedStory);
        } else {
            logger.info("No status update for UserStory {}: allTasksDone={}, currentStatus={}",
                    userStoryId, allTasksDone, userStory.getStatus());
        }

        return new UserStoryDTO(userStory);
    }

    private List<TaskDTO> fetchTasksWithRetry(Long projectId, Long userStoryId, String token) {
        int maxRetries = 3;
        long delayMs = 500;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("Fetching tasks for UserStory {} (attempt {}/{})", userStoryId, attempt, maxRetries);
                if (token == null) {
                    return taskClient.getTasksByProjectAndUserStoryInternal(projectId, userStoryId);
                } else {
                    return taskClient.getTasksByProjectAndUserStory(projectId, userStoryId, token);
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch tasks (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("Échec après " + maxRetries + " tentatives", e);
                }
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interruption pendant la tentative", ie);
                }
            }
        }
        return List.of();
    }
}