package com.project.project_service.Service;

import com.project.project_service.DTO.SprintHistoryDto;
import com.project.project_service.DTO.UserStoryHistoryDto;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.SprintHistory;
import com.project.project_service.Entity.UserStoryHistory;
import com.project.project_service.Repository.SprintHistoryRepository;
import com.project.project_service.Repository.UserStoryHistoryRepository;
import com.project.project_service.config.AuthClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoryService {
@Autowired
    private final UserStoryHistoryRepository userStoryHistoryRepo;
@Autowired
    private final SprintHistoryRepository sprintHistoryRepo;

    @Autowired
    private AuthClient authClient;


    public HistoryService(UserStoryHistoryRepository userStoryHistoryRepo, SprintHistoryRepository sprintHistoryRepo) {
        this.userStoryHistoryRepo = userStoryHistoryRepo;
        this.sprintHistoryRepo = sprintHistoryRepo;
    }

    // ➕ Ajouter une action pour User Story
    public void addUserStoryHistory(Long userStoryId, String action, String author, String description) {
        UserStoryHistory history = new UserStoryHistory();
        history.setUserStoryId(userStoryId);
        history.setAction(action);
        history.setAuthor(author);
        history.setDate(LocalDateTime.now());
        history.setDescription(description);
        userStoryHistoryRepo.save(history);
    }

    // ➕ Ajouter une action pour Sprint
    public void addSprintHistory(Long sprintId, String action, String author, String description) {
        SprintHistory history = new SprintHistory();
        history.setSprintId(sprintId);
        history.setAction(action);
        history.setAuthor(author);
        history.setDate(LocalDateTime.now());
        history.setDescription(description);
        sprintHistoryRepo.save(history);
    }

    // 🔍 Récupérer l’historique d’une User Story
    public List<UserStoryHistory> getUserStoryHistory(Long userStoryId) {
        return userStoryHistoryRepo.findByUserStoryIdOrderByDateDesc(userStoryId);
    }

    // 🔍 Récupérer l’historique d’un Sprint
    public List<SprintHistory> getSprintHistory(Long sprintId) {
        return sprintHistoryRepo.findBySprintIdOrderByDateDesc(sprintId);
    }



    public List<UserStoryHistoryDto> getUserStoryHistoryWithAuthorNames(Long userStoryId, String userToken) {
        List<UserStoryHistory> history = userStoryHistoryRepo.findByUserStoryIdOrderByDateDesc(userStoryId);

        String bearerToken = "Bearer " + userToken;

        // Pour éviter de faire plusieurs appels vers auth-service pour le même ID
        Map<String, Map<String, Object>> userCache = new HashMap<>();

        return history.stream().map(h -> {
            String authorId = h.getAuthor();
            Map<String, Object> userDetails = userCache.computeIfAbsent(authorId,
                    id -> authClient.getUserDetailsByAuthId(id, bearerToken)
            );

            String firstName = (String) userDetails.getOrDefault("firstName", "Inconnu");
            String lastName = (String) userDetails.getOrDefault("lastName", "Inconnu");
            String fullName = firstName + " " + lastName;

            return new UserStoryHistoryDto(
                    h.getAction(),
                    h.getDate(),
                    fullName,
                    h.getDescription()
            );
        }).toList();
    }




    public List<SprintHistoryDto> getSprintHistoryWithAuthorNames(Long userStoryId, String userToken) {
        List<SprintHistory> history = sprintHistoryRepo.findBySprintIdOrderByDateDesc(userStoryId);

        String bearerToken = "Bearer " + userToken;

        // Pour éviter de faire plusieurs appels vers auth-service pour le même ID
        Map<String, Map<String, Object>> userCache = new HashMap<>();

        return history.stream().map(h -> {
            String authorId = h.getAuthor();
            Map<String, Object> userDetails = userCache.computeIfAbsent(authorId,
                    id -> authClient.getUserDetailsByAuthId(id, bearerToken)
            );

            String firstName = (String) userDetails.getOrDefault("firstName", "Inconnu");
            String lastName = (String) userDetails.getOrDefault("lastName", "Inconnu");
            String fullName = firstName + " " + lastName;

            return new SprintHistoryDto(
                    h.getAction(),
                    h.getDate(),
                    fullName,
                    h.getDescription()
            );
        }).toList();
    }

}
