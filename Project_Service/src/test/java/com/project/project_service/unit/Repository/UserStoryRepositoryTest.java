package com.project.project_service.unit.Repository;

import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Entity.UserStory;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Enumeration.UserStoryStatus;
import com.project.project_service.Enumeration.Priority;
import com.project.project_service.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class UserStoryRepositoryTest {

    @Autowired
    private UserStoryRepository userStoryRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    private Projet projet1;
    private Projet projet2;
    private Sprint sprint1;
    private UserStory userStory1;
    private UserStory userStory2;
    private UserStory userStory3;

    @BeforeEach
    void setUp() {
        // Persist Client
        Client client = new Client();
        client.setAuthId("manager1");
        clientRepository.save(client);

        // Persist Entreprise
        Entreprise entreprise = new Entreprise();
        entreprise.setName("Company A");
        entrepriseRepository.save(entreprise);

        // Persist Projet entities
        projet1 = new Projet();
        projet1.setName("Project A");
        projet1.setCompany(entreprise);
        projet1.setManager(client);
        projet1.setCreationDate(LocalDate.now());
        projetRepository.save(projet1);

        projet2 = new Projet();
        projet2.setName("Project B");
        projet2.setCompany(entreprise);
        projet2.setManager(client);
        projet2.setCreationDate(LocalDate.now());
        projetRepository.save(projet2);

        // Persist Sprint
        sprint1 = new Sprint();
        sprint1.setProject(projet1);
        sprint1.setName("Sprint 1");
        sprint1.setStartDate(LocalDate.now());
        sprint1.setEndDate(LocalDate.now().plusDays(14));
        sprint1.setStatus(SprintStatus.ACTIVE);
        sprint1.setCreatedBy("user1");
        sprint1.setCreatedAt(LocalDateTime.now());
        sprint1.setCapacity(100);
        sprintRepository.save(sprint1);

        // Persist UserStory entities
        userStory1 = new UserStory();
        userStory1.setProject(projet1);
        userStory1.setTitle("User Story 1");
        userStory1.setPriority(Priority.HIGH);
        userStory1.setStatus(UserStoryStatus.BACKLOG);
        userStory1.setCreatedBy("user1");
        userStory1.setSprint(sprint1);
        userStoryRepository.save(userStory1);

        userStory2 = new UserStory();
        userStory2.setProject(projet1);
        userStory2.setTitle("User Story 2");
        userStory2.setPriority(Priority.MEDIUM);
        userStory2.setStatus(UserStoryStatus.IN_PROGRESS);
        userStory2.setCreatedBy("user1");
        userStory2.setSprint(sprint1);
        userStoryRepository.save(userStory2);

        userStory3 = new UserStory();
        userStory3.setProject(projet2);
        userStory3.setTitle("User Story 3");
        userStory3.setPriority(Priority.LOW);
        userStory3.setStatus(UserStoryStatus.DONE);
        userStory3.setCreatedBy("user2");
        // No sprint assigned to userStory3
        userStoryRepository.save(userStory3);
    }

    @Test
    void findByProject_shouldReturnUserStoriesForProject() {
        // Act
        List<UserStory> userStories = userStoryRepository.findByProject(projet1);

        // Assert
        assertThat(userStories).hasSize(2);
        assertThat(userStories).extracting(UserStory::getTitle)
                .containsExactlyInAnyOrder("User Story 1", "User Story 2");
        assertThat(userStories).extracting(story -> story.getProject().getId())
                .containsOnly(projet1.getId());
    }

    @Test
    void findByProject_shouldReturnEmptyListForNonExistentProject() {
        // Arrange
        Projet nonExistentProject = new Projet();
        nonExistentProject.setId(999L);

        // Act
        List<UserStory> userStories = userStoryRepository.findByProject(nonExistentProject);

        // Assert
        assertThat(userStories).isEmpty();
    }

    @Test
    void findByStatus_shouldReturnUserStoriesWithStatus() {
        // Act
        List<UserStory> userStories = userStoryRepository.findByStatus(UserStoryStatus.BACKLOG);

        // Assert
        assertThat(userStories).hasSize(1);
        assertThat(userStories.get(0).getTitle()).isEqualTo("User Story 1");
        assertThat(userStories.get(0).getStatus()).isEqualTo(UserStoryStatus.BACKLOG);
    }

    @Test
    void findByStatus_shouldReturnEmptyListForNonExistentStatus() {
        // Act
        List<UserStory> userStories = userStoryRepository.findByStatus(UserStoryStatus.IN_PROGRESS);

        // Assert
        assertThat(userStories).hasSize(1);
        assertThat(userStories.get(0).getTitle()).isEqualTo("User Story 2");
    }

    @Test
    void findBySprintId_shouldReturnUserStoriesForSprint() {
        // Act
        List<UserStory> userStories = userStoryRepository.findBySprintId(sprint1.getId());

        // Assert
        assertThat(userStories).hasSize(2);
        assertThat(userStories).extracting(UserStory::getTitle)
                .containsExactlyInAnyOrder("User Story 1", "User Story 2");
        assertThat(userStories).extracting(story -> story.getSprint().getId())
                .containsOnly(sprint1.getId());
    }

    @Test
    void findBySprintId_shouldReturnEmptyListForNonExistentSprint() {
        // Act
        List<UserStory> userStories = userStoryRepository.findBySprintId(999L);

        // Assert
        assertThat(userStories).isEmpty();
    }
}