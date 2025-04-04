package com.project.project_service.Service;

import com.project.project_service.DTO.UserStoryRequest;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.Priority;
import com.project.project_service.Enumeration.Status;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.UserStoryRepository;
import com.project.project_service.config.AuthClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserStoryService {
    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private AuthClient authClient;

    public UserStory createUserStory(Long projectId, UserStoryRequest request, String token) {
        System.out.println("Début de createUserStory pour projectId: " + projectId);
        System.out.println("Données reçues: " + request);

        String userIdStr = authClient.decodeToken(token);
        System.out.println("UserId extrait du token: " + userIdStr);
        if (userIdStr == null) {
            throw new IllegalArgumentException("Token invalide ou utilisateur non identifié");
        }

        Projet projet = projetRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        System.out.println("Projet trouvé: " + projet.getName());

        UserStory userStory = new UserStory();
        userStory.setProject(projet);
        userStory.setTitle(request.title());
        userStory.setDescription(request.description());
        try {
            userStory.setPriority(Priority.valueOf(request.priority()));
        } catch (IllegalArgumentException e) {
            System.out.println("Erreur: Priorité invalide - " + request.priority());
            throw e;
        }
        userStory.setEffortPoints(request.effortPoints());
        userStory.setCreatedBy(userIdStr);
        userStory.setStatus(Status.BACKLOG);

        UserStory savedStory = userStoryRepository.save(userStory);
        System.out.println("User Story sauvegardée: " + savedStory.getId());
        return savedStory;
    }
}