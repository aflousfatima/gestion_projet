package com.auth.authentification_service.unit.Service;

import com.auth.authentification_service.DTO.InvitationRequest;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Repository.InvitationRepository;
import com.auth.authentification_service.Service.EmailService;
import com.auth.authentification_service.Service.InvitationService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private InvitationService invitationService;

    private InvitationRequest invitationRequest;
    private static final long EXPIRATION_TIME_MS = 48 * 60 * 60 * 1000; // 48 hours in milliseconds

    @BeforeEach
    void setUp() {
        invitationRequest = new InvitationRequest();
        invitationRequest.setEmail("test@example.com");
        invitationRequest.setRole("USER");
        invitationRequest.setEntreprise("EntrepriseX");
        invitationRequest.setProjectId(123L);
    }

    @Test
    @DisplayName("Should create and send invitation successfully")
    void createAndSendInvitation_success() throws MessagingException {

        ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
        invitationService.createAndSendInvitation(invitationRequest);

        verify(invitationRepository).save(invitationCaptor.capture());
        Invitation savedInvitation = invitationCaptor.getValue();
        assertEquals(invitationRequest.getEmail(), savedInvitation.getEmail());
        assertEquals(invitationRequest.getRole(), savedInvitation.getRole());
        assertEquals(invitationRequest.getEntreprise(), savedInvitation.getEntreprise());
        assertEquals(invitationRequest.getProjectId(), savedInvitation.getProjectId());
        assertNotNull(savedInvitation.getToken());
        assertTrue(savedInvitation.getExpiresAt() > System.currentTimeMillis());
        assertTrue(savedInvitation.getExpiresAt() <= System.currentTimeMillis() + EXPIRATION_TIME_MS);

        verify(emailService).sendInvitationEmail(
                eq("test@example.com"),
                eq("USER"),
                eq("EntrepriseX"),
                eq(123L),
                anyString()
        );
        verifyNoMoreInteractions(invitationRepository, emailService);
    }

    @Test
    @DisplayName("Should throw MessagingException when email sending fails")
    void createAndSendInvitation_emailFailure_throwsMessagingException() throws MessagingException {
        // Arrange
        doThrow(new MessagingException("Failed to send email"))
                .when(emailService)
                .sendInvitationEmail(anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act & Assert
        MessagingException exception = assertThrows(MessagingException.class, () ->
                invitationService.createAndSendInvitation(invitationRequest));
        assertEquals("Failed to send email", exception.getMessage());

        // Verify that the invitation is still saved even if email fails
        verify(invitationRepository).save(any(Invitation.class));
        verify(emailService).sendInvitationEmail(anyString(), anyString(), anyString(), anyLong(), anyString());
        verifyNoMoreInteractions(invitationRepository, emailService);
    }
    @Test
    @DisplayName("Should return invitation for valid token")
    void verifyInvitation_validToken_success() {
        // Arrange
        String token = UUID.randomUUID().toString();
        Invitation invitation = new Invitation(
                invitationRequest.getEmail(),
                invitationRequest.getRole(),
                invitationRequest.getEntreprise(),
                invitationRequest.getProjectId(),
                token,
                System.currentTimeMillis() + EXPIRATION_TIME_MS
        );
        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        // Act
        Invitation result = invitationService.verifyInvitation(token);

        // Assert
        assertNotNull(result);
        assertEquals(invitation, result);
        verify(invitationRepository).findByToken(token);
        verifyNoMoreInteractions(invitationRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should throw RuntimeException for invalid token")
    void verifyInvitation_invalidToken_throwsException() {
        // Arrange
        String token = UUID.randomUUID().toString();
        when(invitationRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                invitationService.verifyInvitation(token));
        assertEquals("Jeton d'invitation invalide", exception.getMessage());
        verify(invitationRepository).findByToken(token);
        verifyNoMoreInteractions(invitationRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should throw RuntimeException for expired token")
    void verifyInvitation_expiredToken_throwsException() {
        // Arrange
        String token = UUID.randomUUID().toString();
        Invitation invitation = new Invitation(
                invitationRequest.getEmail(),
                invitationRequest.getRole(),
                invitationRequest.getEntreprise(),
                invitationRequest.getProjectId(),
                token,
                System.currentTimeMillis() - 1000 // Expired 1 second ago
        );
        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                invitationService.verifyInvitation(token));
        assertEquals("Lien d'invitation expiré", exception.getMessage());
        verify(invitationRepository).findByToken(token);
        verifyNoMoreInteractions(invitationRepository);
        verifyNoInteractions(emailService);
    }
}