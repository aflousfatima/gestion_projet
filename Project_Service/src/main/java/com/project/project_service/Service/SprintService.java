package com.project.project_service.Service;

import com.project.project_service.DTO.SprintDTO;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.UserStoryStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.Repository.UserStoryRepository;
import com.project.project_service.config.AuthClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SprintService {
    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private HistoryService historyService;
    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "createSprintRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "createSprintBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "createSprintRetryFallback")
    public SprintDTO createSprint(Long projectId, SprintDTO request, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));

        Sprint sprint = new Sprint();
        sprint.setProject(projet);
        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setGoal(request.getGoal());
        sprint.setCapacity(request.getCapacity());
        sprint.setCreatedBy(userIdStr);
        sprint.setCreatedAt(LocalDateTime.now());
        sprint.setStatus(SprintStatus.valueOf(request.getStatus())); // Utiliser la valeur envoyée par le frontend

        Sprint savedSprint = sprintRepository.save(sprint);

        // Ajout de l'historique de création du sprint
        historyService.addSprintHistory(
                savedSprint.getId(),
                "CREATE", // Action
                userIdStr, // Auteur
                "Sprint créé : " + savedSprint.getName()
        );
        return new SprintDTO(savedSprint);
    }

    public SprintDTO createSprintRateLimiterFallback(Long projectId, SprintDTO request, String token, Throwable t) {
        System.out.println("RateLimiter fallback for createSprint: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for sprint creation");
    }

    public SprintDTO createSprintBulkheadFallback(Long projectId, SprintDTO request, String token, Throwable t) {
        System.out.println("Bulkhead fallback for createSprint: " + t.getMessage());
        throw new RuntimeException("Too many concurrent sprint creation requests");
    }

    public SprintDTO createSprintRetryFallback(Long projectId, SprintDTO request, String token, Throwable t) {
        System.out.println("Retry fallback for createSprint: " + t.getMessage());
        throw new RuntimeException("Failed to create sprint after retries");
    }

    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "getSprintsByProjectIdRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "getSprintsByProjectIdBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "getSprintsByProjectIdRetryFallback")
    public List<SprintDTO> getSprintsByProjectId(Long projectId) {
        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        List<Sprint> sprints = sprintRepository.findByProject(projet);
        return sprints.stream().map(SprintDTO::new).collect(Collectors.toList());
    }
    public List<SprintDTO> getSprintsByProjectIdRateLimiterFallback(Long projectId, Throwable t) {
        System.out.println("RateLimiter fallback for getSprintsByProjectId: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<SprintDTO> getSprintsByProjectIdBulkheadFallback(Long projectId, Throwable t) {
        System.out.println("Bulkhead fallback for getSprintsByProjectId: " + t.getMessage());
        return new ArrayList<>();
    }

    public List<SprintDTO> getSprintsByProjectIdRetryFallback(Long projectId, Throwable t) {
        System.out.println("Retry fallback for getSprintsByProjectId: " + t.getMessage());
        return new ArrayList<>();
    }

    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "updateSprintRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "updateSprintBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "updateSprintRetryFallback")
    public SprintDTO updateSprint(Long projectId, Long sprintId, SprintDTO request, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint non trouvé avec l'ID: " + sprintId));

        if (!sprint.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Le Sprint n'appartient pas à ce projet");
        }

        sprint.setName(request.getName());
        sprint.setStartDate(request.getStartDate());
        sprint.setEndDate(request.getEndDate());
        sprint.setGoal(request.getGoal());
        sprint.setCapacity(request.getCapacity());
        try {
            Sprint updatedSprint = sprintRepository.save(sprint);

            // Ajout de l'historique
            historyService.addSprintHistory(
                    updatedSprint.getId(),
                    "UPDATE", // Action
                    userIdStr, // Auteur
                    "Sprint mis à jour : " + updatedSprint.getName()
            );

            return new SprintDTO(updatedSprint);
        } catch (OptimisticLockException e) {
            throw new RuntimeException("Conflit de mise à jour détecté. Veuillez réessayer.", e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du Sprint", e);
        }
    }

    public SprintDTO updateSprintRateLimiterFallback(Long projectId, Long sprintId, SprintDTO request, String token, Throwable t) {
        System.out.println("RateLimiter fallback for updateSprint: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for sprint update");
    }

    public SprintDTO updateSprintBulkheadFallback(Long projectId, Long sprintId, SprintDTO request, String token, Throwable t) {
        System.out.println("Bulkhead fallback for updateSprint: " + t.getMessage());
        throw new RuntimeException("Too many concurrent sprint update requests");
    }

    public SprintDTO updateSprintRetryFallback(Long projectId, Long sprintId, SprintDTO request, String token, Throwable t) {
        System.out.println("Retry fallback for updateSprint: " + t.getMessage());
        throw new RuntimeException("Failed to update sprint after retries");
    }

    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "deleteSprintRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "deleteSprintBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "deleteSprintRetryFallback")
    public void deleteSprint(Long projectId, Long sprintId) {
        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint non trouvé avec l'ID: " + sprintId));

        if (!sprint.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Le Sprint n'appartient pas à ce projet");
        }

        sprintRepository.delete(sprint);

        // Ajout de l'historique de suppression
        historyService.addSprintHistory(
                sprint.getId(),
                "DELETE", // Action
                sprint.getCreatedBy(), // Auteur
                "Sprint supprimé : " + sprint.getName()
        );
    }

    public void deleteSprintRateLimiterFallback(Long projectId, Long sprintId, Throwable t) {
        System.out.println("RateLimiter fallback for deleteSprint: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for sprint deletion");
    }

    public void deleteSprintBulkheadFallback(Long projectId, Long sprintId, Throwable t) {
        System.out.println("Bulkhead fallback for deleteSprint: " + t.getMessage());
        throw new RuntimeException("Too many concurrent sprint deletion requests");
    }

    public void deleteSprintRetryFallback(Long projectId, Long sprintId, Throwable t) {
        System.out.println("Retry fallback for deleteSprint: " + t.getMessage());
        throw new RuntimeException("Failed to delete sprint after retries");
    }

    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "updateSprintStatusRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "updateSprintStatusBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "updateSprintStatusRetryFallback")
    public SprintDTO updateSprintStatus(Long projectId, Long sprintId, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint non trouvé avec l'ID: " + sprintId));
        if (!sprint.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Le Sprint n'appartient pas à ce projet");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDateTime = sprint.getEndDate().atTime(23, 59);
        if (sprint.getStatus() == SprintStatus.ACTIVE && now.isAfter(endDateTime)) {
            sprint.setStatus(SprintStatus.COMPLETED);
            sprint.getUserStories().forEach(us -> {
                if (!us.getStatus().equals(UserStoryStatus.DONE)) {
                    us.setSprint(null);
                    us.setStatus(UserStoryStatus.BACKLOG);
                    userStoryRepository.save(us);
                }
            });
            sprintRepository.save(sprint);
            historyService.addSprintHistory(
                    sprint.getId(),
                    "UPDATE_STATUS", // Action
                    userIdStr, // Auteur
                    "Sprint complété : " + sprint.getName()
            );
        }
        return new SprintDTO(sprint); // Toujours renvoyer le sprint, même sans changement
    }
    public SprintDTO updateSprintStatusRateLimiterFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("RateLimiter fallback for updateSprintStatus: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for updating sprint status");
    }

    public SprintDTO updateSprintStatusBulkheadFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Bulkhead fallback for updateSprintStatus: " + t.getMessage());
        throw new RuntimeException("Too many concurrent sprint status update requests");
    }

    public SprintDTO updateSprintStatusRetryFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Retry fallback for updateSprintStatus: " + t.getMessage());
        throw new RuntimeException("Failed to update sprint status after retries");
    }

    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "cancelSprintRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "cancelSprintBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "cancelSprintRetryFallback")
    public SprintDTO cancelSprint(Long projectId, Long sprintId, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint non trouvé avec l'ID: " + sprintId));
        if (!sprint.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Le Sprint n'appartient pas à ce projet");
        }

        if (sprint.getStatus() == SprintStatus.ACTIVE || sprint.getStatus() == SprintStatus.PLANNED) {
            sprint.setStatus(SprintStatus.CANCELED);
            sprint.getUserStories().forEach(us -> {
                us.setSprint(null);
                us.setStatus(UserStoryStatus.BACKLOG);
                userStoryRepository.save(us);
            });
            Sprint updatedSprint = sprintRepository.save(sprint);
            // Ajout de l'historique d'annulation
            historyService.addSprintHistory(
                    updatedSprint.getId(),
                    "CANCEL", // Action
                    userIdStr, // Auteur
                    "Sprint annulé : " + updatedSprint.getName()
            );
            return new SprintDTO(updatedSprint);
        }
        throw new RuntimeException("Impossible d'annuler un sprint déjà terminé ou archivé");
    }
    public SprintDTO cancelSprintRateLimiterFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("RateLimiter fallback for cancelSprint: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for canceling sprint");
    }

    public SprintDTO cancelSprintBulkheadFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Bulkhead fallback for cancelSprint: " + t.getMessage());
        throw new RuntimeException("Too many concurrent sprint cancellation requests");
    }

    public SprintDTO cancelSprintRetryFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Retry fallback for cancelSprint: " + t.getMessage());
        throw new RuntimeException("Failed to cancel sprint after retries");
    }

    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "archiveSprintRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "archiveSprintBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "archiveSprintRetryFallback")
    public SprintDTO archiveSprint(Long projectId, Long sprintId, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint non trouvé avec l'ID: " + sprintId));
        if (!sprint.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Le Sprint n'appartient pas à ce projet");
        }

        if (sprint.getStatus() == SprintStatus.COMPLETED || sprint.getStatus() == SprintStatus.CANCELED) {
            sprint.setStatus(SprintStatus.ARCHIVED);
            Sprint updatedSprint = sprintRepository.save(sprint);

            // Ajout de l'historique d'archivage
            historyService.addSprintHistory(
                    updatedSprint.getId(),
                    "ARCHIVE", // Action
                    userIdStr, // Auteur
                    "Sprint archivé : " + updatedSprint.getName()
            );
            return new SprintDTO(updatedSprint);
        }
        throw new RuntimeException("Seuls les sprints terminés ou annulés peuvent être archivés");
    }

    public SprintDTO archiveSprintRateLimiterFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("RateLimiter fallback for archiveSprint: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for archiving sprint");
    }

    public SprintDTO archiveSprintBulkheadFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Bulkhead fallback for archiveSprint: " + t.getMessage());
        throw new RuntimeException("Too many concurrent sprint archive requests");
    }

    public SprintDTO archiveSprintRetryFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Retry fallback for archiveSprint: " + t.getMessage());
        throw new RuntimeException("Failed to archive sprint after retries");
    }

    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "activateSprintRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "activateSprintBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "activateSprintRetryFallback")
    public SprintDTO activateSprint(Long projectId, Long sprintId, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint non trouvé avec l'ID: " + sprintId));

        if (!sprint.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Le Sprint n'appartient pas à ce projet");
        }

        if (sprint.getStatus() != SprintStatus.PLANNED) {
            throw new RuntimeException("Seul un sprint planifié (PLANNED) peut être activé");
        }

        sprint.setStatus(SprintStatus.ACTIVE);

        // Mettre à jour les user stories en fonction des dépendances
        sprint.getUserStories().forEach(us -> {
            if (us.getStatus() != UserStoryStatus.DONE && us.getStatus() != UserStoryStatus.CANCELED) {
                updateStatusBasedOnDependencies(us); // Méthode existante dans UserStoryService
                userStoryRepository.save(us);
            }
        });

        Sprint updatedSprint = sprintRepository.save(sprint);

        // Ajout de l'historique d'activation
        historyService.addSprintHistory(
                updatedSprint.getId(),
                "ACTIVATE", // Action
                userIdStr, // Auteur
                "Sprint activé : " + updatedSprint.getName()
        );
        return new SprintDTO(updatedSprint);
    }

    public SprintDTO activateSprintRateLimiterFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("RateLimiter fallback for activateSprint: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for activating sprint");
    }

    public SprintDTO activateSprintBulkheadFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Bulkhead fallback for activateSprint: " + t.getMessage());
        throw new RuntimeException("Too many concurrent sprint activation requests");
    }

    public SprintDTO activateSprintRetryFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Retry fallback for activateSprint: " + t.getMessage());
        throw new RuntimeException("Failed to activate sprint after retries");
    }

    public void updateStatusBasedOnDependencies(UserStory userStory) {
        if (userStory.getDependsOn().isEmpty()) {
            if (userStory.getSprint() != null) {
                userStory.setStatus(userStory.getSprint().getStatus() == SprintStatus.ACTIVE ? UserStoryStatus.IN_PROGRESS : UserStoryStatus.SELECTED_FOR_SPRINT);
            } else if (!userStory.getStatus().equals(UserStoryStatus.DONE) && !userStory.getStatus().equals(UserStoryStatus.CANCELED)) {
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
            userStory.setStatus(UserStoryStatus.BACKLOG); // Sans sprint, toujours BACKLOG si pas DONE/CANCELED
        } else if (userStory.getSprint().getStatus() == SprintStatus.PLANNED) {
            if (allDependenciesDone) {
                userStory.setStatus(UserStoryStatus.SELECTED_FOR_SPRINT);
            } else {
                userStory.setStatus(UserStoryStatus.ON_HOLD); // En attente car dépendances non terminées
            }
        } else if (userStory.getSprint().getStatus() == SprintStatus.ACTIVE) {
            if (allDependenciesDone) {
                userStory.setStatus(UserStoryStatus.IN_PROGRESS);
            } else if (allDependenciesInActiveSprint) {
                userStory.setStatus(UserStoryStatus.BLOCKED); // Dépendances dans sprint actif mais pas DONE
            } else {
                userStory.setStatus(UserStoryStatus.ON_HOLD); // Dépendances pas dans sprint actif
            }
        }
    }


    @RateLimiter(name = "SprintServiceLimiter", fallbackMethod = "checkAndUpdateSprintStatusRateLimiterFallback")
    @Bulkhead(name = "SprintServiceBulkhead", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "checkAndUpdateSprintStatusBulkheadFallback")
    @Retry(name = "SprintServiceRetry", fallbackMethod = "checkAndUpdateSprintStatusRetryFallback")
    public SprintDTO checkAndUpdateSprintStatus(Long projectId, Long sprintId, String token) {
        String userIdStr = authClient.decodeToken(token);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint non trouvé avec l'ID: " + sprintId));

        if (!sprint.getProject().getId().equals(projectId)) {
            throw new RuntimeException("Le Sprint n'appartient pas à ce projet");
        }

        // Vérifier si toutes les User Stories sont DONE
        boolean allUserStoriesDone = sprint.getUserStories().stream()
                .allMatch(us -> us.getStatus().equals(UserStoryStatus.DONE));

        // Mettre à jour le statut si toutes les User Stories sont DONE
        if (allUserStoriesDone && sprint.getStatus() != SprintStatus.COMPLETED) {
            sprint.setStatus(SprintStatus.COMPLETED);
            Sprint updatedSprint = sprintRepository.save(sprint);
            historyService.addSprintHistory(
                    updatedSprint.getId(),
                    "UPDATE_STATUS",
                    userIdStr,
                    "Sprint complété : " + updatedSprint.getName()
            );
            return new SprintDTO(updatedSprint);
        }

        return new SprintDTO(sprint);
    }


    public SprintDTO checkAndUpdateSprintStatusRateLimiterFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("RateLimiter fallback for checkAndUpdateSprintStatus: " + t.getMessage());
        throw new RuntimeException("Rate limit exceeded for checking sprint status");
    }

    public SprintDTO checkAndUpdateSprintStatusBulkheadFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Bulkhead fallback for checkAndUpdateSprintStatus: " + t.getMessage());
        throw new RuntimeException("Too many concurrent sprint status check requests");
    }

    public SprintDTO checkAndUpdateSprintStatusRetryFallback(Long projectId, Long sprintId, String token, Throwable t) {
        System.out.println("Retry fallback for checkAndUpdateSprintStatus: " + t.getMessage());
        throw new RuntimeException("Failed to check sprint status after retries");
    }
}