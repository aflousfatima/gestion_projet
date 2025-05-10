package com.project.project_service.unit.Repository;

import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Entity.Sprint;
import com.project.project_service.Enumeration.SprintStatus;
import com.project.project_service.Repository.ClientRepository;
import com.project.project_service.Repository.EntrepriseRepository;
import com.project.project_service.Repository.ProjetRepository;
import com.project.project_service.Repository.SprintRepository;
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
public class SprintRepositoryTest {

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    private Projet projet1;
    private Projet projet2;
    private Sprint sprint1;
    private Sprint sprint2;
    private Sprint sprint3;

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

        // Persist Sprint entities
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

        sprint2 = new Sprint();
        sprint2.setProject(projet1);
        sprint2.setName("Sprint 2");
        sprint2.setStartDate(LocalDate.now().plusDays(15));
        sprint2.setEndDate(LocalDate.now().plusDays(29));
        sprint2.setStatus(SprintStatus.PLANNED);
        sprint2.setCreatedBy("user1");
        sprint2.setCreatedAt(LocalDateTime.now());
        sprint2.setCapacity(80);
        sprintRepository.save(sprint2);

        sprint3 = new Sprint();
        sprint3.setProject(projet2);
        sprint3.setName("Sprint 3");
        sprint3.setStartDate(LocalDate.now());
        sprint3.setEndDate(LocalDate.now().plusDays(14));
        sprint3.setStatus(SprintStatus.COMPLETED);
        sprint3.setCreatedBy("user2");
        sprint3.setCreatedAt(LocalDateTime.now());
        sprint3.setCapacity(90);
        sprintRepository.save(sprint3);
    }

    @Test
    void findByProject_shouldReturnSprintsForProject() {
        // Act
        List<Sprint> sprints = sprintRepository.findByProject(projet1);

        // Assert
        assertThat(sprints).hasSize(2);
        assertThat(sprints).extracting(Sprint::getName)
                .containsExactlyInAnyOrder("Sprint 1", "Sprint 2");
        assertThat(sprints).extracting(sprint -> sprint.getProject().getId())
                .containsOnly(projet1.getId());
    }

    @Test
    void findByProject_shouldReturnEmptyListForNonExistentProject() {
        // Arrange
        Projet nonExistentProject = new Projet();
        nonExistentProject.setId(999L);

        // Act
        List<Sprint> sprints = sprintRepository.findByProject(nonExistentProject);

        // Assert
        assertThat(sprints).isEmpty();
    }

    @Test
    void findByProjectIdAndStatus_shouldReturnSprintWhenExists() {
        // Act
        Sprint foundSprint = sprintRepository.findByProjectIdAndStatus(projet1.getId(), SprintStatus.ACTIVE);

        // Assert
        assertThat(foundSprint).isNotNull();
        assertThat(foundSprint.getName()).isEqualTo("Sprint 1");
        assertThat(foundSprint.getProject().getId()).isEqualTo(projet1.getId());
        assertThat(foundSprint.getStatus()).isEqualTo(SprintStatus.ACTIVE);
    }

    @Test
    void findByProjectIdAndStatus_shouldReturnNullWhenNotExists() {
        // Act
        Sprint foundSprint = sprintRepository.findByProjectIdAndStatus(projet1.getId(), SprintStatus.COMPLETED);

        // Assert
        assertThat(foundSprint).isNull();
    }
}