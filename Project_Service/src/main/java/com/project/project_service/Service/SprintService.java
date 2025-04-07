package com.project.project_service.Service;

import com.project.project_service.DTO.SprintDTO;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.UserStoryStatus;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
import com.project.project_service.Repository.UserStoryRepository;
import com.project.project_service.config.AuthClient;
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
        Sprint updatedSprint = sprintRepository.save(sprint);
        return new SprintDTO(updatedSprint);
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
    }

    public void updateSprintStatus(Long projectId, Long sprintId, String token) {
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
        LocalDateTime endDateTime = sprint.getEndDate().atTime(23, 59); // fin de la journée
        if (sprint.getStatus() == SprintStatus.ACTIVE && now.isAfter(endDateTime)) {
            // Sprint atteint la date de fin
            sprint.setStatus(SprintStatus.COMPLETED);
            // Remettre les US incomplètes dans le backlog
            sprint.getUserStories().forEach(us -> {
                if (!us.getStatus().equals(UserStoryStatus.DONE)) {
                    us.setSprint(null);
                    us.setStatus(UserStoryStatus.BACKLOG);
                    userStoryRepository.save(us);
                }
            });
            sprintRepository.save(sprint);
        }
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

        // Changer le statut du sprint à ACTIVE
        sprint.setStatus(SprintStatus.ACTIVE);

        // Mettre à jour les user stories existantes
        sprint.getUserStories().forEach(us -> {
            if (us.getStatus() != UserStoryStatus.DONE && us.getStatus() != UserStoryStatus.CANCELED) {
                us.setStatus(UserStoryStatus.IN_PROGRESS);
                // Optionnel : vérifier les dépendances si nécessaire
                // updateStatusBasedOnDependencies(us);
                userStoryRepository.save(us);
            }
        });

        Sprint updatedSprint = sprintRepository.save(sprint);
        return new SprintDTO(updatedSprint);
    }
}