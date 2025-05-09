package com.auth.authentification_service.Repository;

import com.auth.authentification_service.Entity.Invitation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") // Charge application-test.properties
public class InvitationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InvitationRepository invitationRepository;

    @Test
    public void whenFindByToken_thenReturnInvitation() {
        // Given
        Invitation invitation = new Invitation();
        invitation.setToken("test-token");
        invitation.setProjectId(1L); // Fournir une valeur pour projectId
        invitation.setEmail("test@example.com"); // Exemple pour email
        invitation.setEntreprise("Test Entreprise"); // Exemple pour entreprise
        invitation.setExpiresAt(2l); // Exemple pour expiresAt
        invitation.setRole("USER"); // Exemple pour role
        invitation.setUsed(false); // Exemple pour used
        entityManager.persist(invitation);
        entityManager.flush();

        // When
        Optional<Invitation> found = invitationRepository.findByToken("test-token");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("test-token");
    }

    @Test
    public void whenFindByInvalidToken_thenReturnEmpty() {
        // When
        Optional<Invitation> found = invitationRepository.findByToken("invalid-token");

        // Then
        assertThat(found).isNotPresent();
    }
}