package com.auth.authentification_service.integration;

import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Repository.InvitationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class InvitationRepositoryTest {

    @Autowired
    private InvitationRepository invitationRepository;

    @Test
    void whenFindByToken_thenReturnInvitation() {
        // Given
        Invitation invitation = new Invitation();
        invitation.setToken("test-token");
        invitation.setProjectId(1L);
        invitation.setEmail("test@example.com");
        invitation.setEntreprise("Test Entreprise");
        invitation.setExpiresAt(1742920985018L);
        invitation.setRole("USER");
        invitation.setUsed(false);
        invitationRepository.save(invitation);

        // When
        Optional<Invitation> found = invitationRepository.findByToken("test-token");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("test-token");
    }

    @Test
    void whenFindByInvalidToken_thenReturnEmpty() {
        // When
        Optional<Invitation> found = invitationRepository.findByToken("invalid-token");

        // Then
        assertThat(found).isNotPresent();
    }
}