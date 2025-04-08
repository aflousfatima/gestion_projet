package com.project.project_service.Service;

import com.project.project_service.Entity.SprintHistory;
import com.project.project_service.Entity.UserStoryHistory;
import com.project.project_service.Repository.SprintHistoryRepository;
import com.project.project_service.Repository.UserStoryHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class HistoryService {
@Autowired
    private final UserStoryHistoryRepository userStoryHistoryRepo;
@Autowired
    private final SprintHistoryRepository sprintHistoryRepo;

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
        return userStoryHistoryRepo.findByUserStoryId(userStoryId);
    }

    // 🔍 Récupérer l’historique d’un Sprint
    public List<SprintHistory> getSprintHistory(Long sprintId) {
        return sprintHistoryRepo.findBySprintId(sprintId);
    }
}
