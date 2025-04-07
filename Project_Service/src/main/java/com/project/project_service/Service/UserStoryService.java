package com.project.project_service.Service;

import com.project.project_service.DTO.SprintDTO;
import com.project.project_service.DTO.UserStoryDTO;
import com.project.project_service.DTO.UserStoryRequest;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.Priority;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.UserStoryStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.Repository.UserStoryRepository;
import com.project.project_service.config.AuthClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserStoryService {
    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private AuthClient authClient;

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
        UserStory savedStory = userStoryRepository.save(userStory);
        return new UserStoryDTO(savedStory);
    }
    // New method to update a user story
    public UserStoryDTO updateUserStory(Long projectId, Long userStoryId, UserStoryRequest request, String token) {
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

        userStory.setTitle(request.title());
        userStory.setDescription(request.description());
        userStory.setPriority(Priority.valueOf(request.priority()));
        userStory.setEffortPoints(request.effortPoints());
        // Ne pas modifier status ici
        userStory.setDependsOn(request.dependsOn() != null ? request.dependsOn() : userStory.getDependsOn());
        UserStory updatedStory = userStoryRepository.save(userStory);
        return new UserStoryDTO(updatedStory);
    }
    // New method to fetch all user stories for a project
    public List<UserStoryDTO> getUserStoriesByProjectId(Long projectId) {
        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        List<UserStory> userStories = userStoryRepository.findByProject(projet);
        return userStories.stream().map(UserStoryDTO::new).toList();
    }

    // New method to delete a user story
    public void deleteUserStory(Long projectId, Long userStoryId) {
        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        UserStory userStory = userStoryRepository.findById(userStoryId)
                .orElseThrow(() -> new RuntimeException("User Story non trouvée avec l'ID: " + userStoryId));
        if (!userStory.getProject().getId().equals(projectId)) {
            throw new RuntimeException("La User Story n'appartient pas à ce projet");
        }
        userStoryRepository.delete(userStory);
    }

    // Nouvelle méthode pour assigner une user story à un sprint
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

        // Vérifier les dépendances
        if (!userStory.getDependsOn().isEmpty()) {
            for (Long depId : userStory.getDependsOn()) {
                UserStory depStory = userStoryRepository.findById(depId)
                        .orElseThrow(() -> new RuntimeException("Dépendance non trouvée : " + depId));
                if (!depStory.getStatus().equals(UserStoryStatus.DONE)) {
                    userStory.setStatus(UserStoryStatus.BLOCKED);
                    userStoryRepository.save(userStory);
                    throw new RuntimeException("Dépendance non terminée : " + depStory.getTitle());
                }
            }
        }

        // Assigner la user story au sprint
        userStory.setSprint(sprint);

        // Définir le statut en fonction du statut du sprint
        if (sprint.getStatus() == SprintStatus.ACTIVE) {
            userStory.setStatus(UserStoryStatus.IN_PROGRESS);
        } else {
            userStory.setStatus(UserStoryStatus.SELECTED_FOR_SPRINT);
        }

        UserStory updatedUserStory = userStoryRepository.save(userStory);
        return new UserStoryDTO(updatedUserStory);
    }
    // Nouvelle méthode pour retirer une user story d'un sprint
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
        return new UserStoryDTO(updatedUserStory);
    }

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
        updateStatusBasedOnDependencies(userStory);

        UserStory updatedStory = userStoryRepository.save(userStory);
        return new UserStoryDTO(updatedStory);
    }

    // Méthode utilitaire pour recalculer le statut
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
        return new UserStoryDTO(updatedStory);
    }
    private void updateStatusBasedOnDependencies(UserStory userStory) {
        if (userStory.getDependsOn().isEmpty()) {
            if (userStory.getSprint() != null) {
                userStory.setStatus(userStory.getSprint().getStatus() == SprintStatus.ACTIVE ? UserStoryStatus.IN_PROGRESS : UserStoryStatus.SELECTED_FOR_SPRINT);
            } else if (!userStory.getStatus().equals(UserStoryStatus.DONE) && !userStory.getStatus().equals(UserStoryStatus.CANCELED)) {
                userStory.setStatus(UserStoryStatus.BACKLOG);
            }
            return;
        }

        boolean allDependenciesDone = true;
        for (Long depId : userStory.getDependsOn()) {
            UserStory depStory = userStoryRepository.findById(depId)
                    .orElseThrow(() -> new RuntimeException("Dépendance non trouvée : " + depId));
            if (!depStory.getStatus().equals(UserStoryStatus.DONE)) {
                allDependenciesDone = false;
                break;
            }
        }

        if (!allDependenciesDone) {
            userStory.setStatus(UserStoryStatus.BLOCKED);
        } else if (userStory.getSprint() != null) {
            userStory.setStatus(userStory.getSprint().getStatus() == SprintStatus.ACTIVE ? UserStoryStatus.IN_PROGRESS : UserStoryStatus.SELECTED_FOR_SPRINT);
        } else if (!userStory.getStatus().equals(UserStoryStatus.DONE) && !userStory.getStatus().equals(UserStoryStatus.CANCELED)) {
            userStory.setStatus(UserStoryStatus.BACKLOG);
        }
    }


}