package com.project.project_service.Service;

import com.project.project_service.DTO.SprintDTO;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
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
}