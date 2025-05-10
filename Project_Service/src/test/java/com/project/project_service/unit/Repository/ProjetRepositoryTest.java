package com.project.project_service.unit.Repository;

import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Repository.ClientRepository;
import com.project.project_service.Repository.EntrepriseRepository;
import com.project.project_service.Repository.ProjetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class ProjetRepositoryTest {

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    private Client client1;
    private Client client2;
    private Entreprise entreprise1;
    private Entreprise entreprise2;
    private Projet projet1;
    private Projet projet2;
    private Projet projet3;

    @BeforeEach
    void setUp() {
        // Persist Client entities
        client1 = new Client();
        client1.setAuthId("manager1");
        clientRepository.save(client1);

        client2 = new Client();
        client2.setAuthId("manager2");
        clientRepository.save(client2);

        // Persist Entreprise entities
        entreprise1 = new Entreprise();
        entreprise1.setName("Company A");
        entrepriseRepository.save(entreprise1);

        entreprise2 = new Entreprise();
        entreprise2.setName("Company B");
        entrepriseRepository.save(entreprise2);

        // Persist Projet entities
        projet1 = new Projet();
        projet1.setName("Project A");
        projet1.setCompany(entreprise1);
        projet1.setManager(client1);
        projet1.setCreationDate(LocalDate.now());
        projetRepository.save(projet1);

        projet2 = new Projet();
        projet2.setName("Project B");
        projet2.setCompany(entreprise1);
        projet2.setManager(client1);
        projet2.setCreationDate(LocalDate.now());
        projetRepository.save(projet2);

        projet3 = new Projet();
        projet3.setName("Project C");
        projet3.setCompany(entreprise2);
        projet3.setManager(client2);
        projet3.setCreationDate(LocalDate.now());
        projetRepository.save(projet3);
    }

    @Test
    void findByCompany_shouldReturnProjectsForCompany() {
        // Act
        List<Projet> projects = projetRepository.findByCompany(entreprise1);

        // Assert
        assertThat(projects).hasSize(2);
        assertThat(projects).extracting(Projet::getName)
                .containsExactlyInAnyOrder("Project A", "Project B");
        assertThat(projects).extracting(projet -> projet.getCompany().getId())
                .containsOnly(entreprise1.getId());
    }

    @Test
    void findByCompany_shouldReturnEmptyListForNonExistentCompany() {
        // Arrange
        Entreprise nonExistentCompany = new Entreprise();
        nonExistentCompany.setId(999L);

        // Act
        List<Projet> projects = projetRepository.findByCompany(nonExistentCompany);

        // Assert
        assertThat(projects).isEmpty();
    }

    @Test
    void findByNameAndCompany_shouldReturnProjectWhenExists() {
        // Act
        Projet foundProject = projetRepository.findByNameAndCompany("Project A", entreprise1);

        // Assert
        assertThat(foundProject).isNotNull();
        assertThat(foundProject.getName()).isEqualTo("Project A");
        assertThat(foundProject.getCompany().getId()).isEqualTo(entreprise1.getId());
    }

    @Test
    void findByNameAndCompany_shouldReturnNullWhenNotExists() {
        // Act
        Projet foundProject = projetRepository.findByNameAndCompany("NonExistent", entreprise1);

        // Assert
        assertThat(foundProject).isNull();
    }

    @Test
    void findByManagerAuthId_shouldReturnProjectsForManager() {
        // Act
        List<Projet> projects = projetRepository.findByManagerAuthId("manager1");

        // Assert
        assertThat(projects).hasSize(2);
        assertThat(projects).extracting(Projet::getName)
                .containsExactlyInAnyOrder("Project A", "Project B");
        assertThat(projects).extracting(projet -> projet.getManager().getAuthId())
                .containsOnly("manager1");
    }

    @Test
    void findByManagerAuthId_shouldReturnEmptyListForNonExistentAuthId() {
        // Act
        List<Projet> projects = projetRepository.findByManagerAuthId("nonexistent");

        // Assert
        assertThat(projects).isEmpty();
    }
}