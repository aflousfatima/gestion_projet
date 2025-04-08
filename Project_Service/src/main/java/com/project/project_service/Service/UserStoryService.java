package com.project.project_service.Service;

import com.project.project_service.DTO.UserStoryDTO;
import com.project.project_service.DTO.UserStoryRequest;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.Tag;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.Priority;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.UserStoryStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.Repository.TagRepository;
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
    private TagRepository tagRepository;
    @Autowired
    private AuthClient authClient;
    @Autowired
    private HistoryService historyService;

    private UserStoryDTO convertToDTO(UserStory userStory) {
        return new UserStoryDTO(userStory);
    }
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
    // New method to update a user story
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

            // Ajout de l'historique
            historyService.addUserStoryHistory(
                    updatedStory.getId(),
                    "UPDATE", // Action
                    userIdStr, // Auteur (ou id utilisateur décrypté)
                    "User Story mise à jour : " + updatedStory.getTitle()
            );

            return new UserStoryDTO(updatedStory);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la User Story", e);
        }
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
        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "DELETE", // Action
                userStory.getCreatedBy(), // Auteur
                "User Story supprimé : " + userStory.getTitle()
        );
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
        updateStatusBasedOnDependencies(userStory); // Recalculer le statut
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

        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "DELETE", // Action
                userStory.getCreatedBy(), // Auteur
                "User Story  retirer su sprint : " + userStory.getTitle()
        );
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
        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "UPDATE_DEPENCENCIES", // Action
                userStory.getCreatedBy(), // Auteur
                "Modifier dependance entre les Users Stories : " + userStory.getTitle()
        );
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

        // Ajout de l'historique de suppression
        historyService.addUserStoryHistory(
                userStory.getId(),
                "UPDATE_USER_STORY_STATUS", // Action
                userStory.getCreatedBy(), // Auteur
                "Modifier Status du User Story : " + userStory.getTitle()
        );
        return new UserStoryDTO(updatedStory);
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

}