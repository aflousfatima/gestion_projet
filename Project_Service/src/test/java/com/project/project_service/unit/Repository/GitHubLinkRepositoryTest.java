package com.project.project_service.unit.Repository;
import com.project.project_service.Entity.Client;
import com.project.project_service.Entity.Entreprise;
import com.project.project_service.Entity.GitHubLink;
import com.project.project_service.Entity.Projet;
import com.project.project_service.Repository.ClientRepository;
import com.project.project_service.Repository.EntrepriseRepository;
import com.project.project_service.Repository.GitHubLinkRepository;
import com.project.project_service.Repository.ProjetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class GitHubLinkRepositoryTest {

    @Autowired
    private GitHubLinkRepository gitHubLinkRepository;

    @Autowired
    private ProjetRepository projetRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    private Projet projet;
    private GitHubLink gitHubLink;

    @BeforeEach
    void setUp() {
        // Persist Client
        Client client = new Client();
        client.setAuthId("manager123");
        clientRepository.save(client);

        // Persist Entreprise
        Entreprise entreprise = new Entreprise();
        entreprise.setName("Test Company");
        entrepriseRepository.save(entreprise);

        // Persist Projet
        projet = new Projet();
        projet.setName("Test Project");
        projet.setManager(client);
        projet.setCompany(entreprise);
        projet.setCreationDate(LocalDate.now());
        projetRepository.save(projet);

        // Persist GitHubLink
        gitHubLink = new GitHubLink(
                "https://github.com/test/repo",
                projet,
                "user123"
        );
        gitHubLinkRepository.save(gitHubLink);
    }

    @Test
    void findByProjetId_shouldReturnGitHubLink_whenProjectIdExists() {
        GitHubLink foundLink = gitHubLinkRepository.findByProjetId(projet.getId());

        assertThat(foundLink).isNotNull();
        assertThat(foundLink.getRepositoryUrl()).isEqualTo("https://github.com/test/repo");
        assertThat(foundLink.getProjet().getId()).isEqualTo(projet.getId());
        assertThat(foundLink.getUserId()).isEqualTo("user123");
    }

    @Test
    void findByProjetId_shouldReturnNull_whenProjectIdDoesNotExist() {
        GitHubLink foundLink = gitHubLinkRepository.findByProjetId(999L);

        assertThat(foundLink).isNull();
    }

    @Test
    void findByProjectId_shouldReturnOptionalGitHubLink_whenProjectIdExists() {
        Optional<GitHubLink> foundLink = gitHubLinkRepository.findByProjectId(projet.getId());

        assertThat(foundLink).isPresent();
        assertThat(foundLink.get().getRepositoryUrl()).isEqualTo("https://github.com/test/repo");
        assertThat(foundLink.get().getProjet().getId()).isEqualTo(projet.getId());
        assertThat(foundLink.get().getUserId()).isEqualTo("user123");
    }

    @Test
    void findByProjectId_shouldReturnEmptyOptional_whenProjectIdDoesNotExist() {
        Optional<GitHubLink> foundLink = gitHubLinkRepository.findByProjectId(999L);

        assertThat(foundLink).isEmpty();
    }
}