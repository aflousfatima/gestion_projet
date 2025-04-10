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
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public List<SprintDTO> getSprintsByProjectId(Long projectId) {
        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        List<Sprint> sprints = sprintRepository.findByProject(projet);
        return sprints.stream().map(SprintDTO::new).collect(Collectors.toList());
    }

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
}