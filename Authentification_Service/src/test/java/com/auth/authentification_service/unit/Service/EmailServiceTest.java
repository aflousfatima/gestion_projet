package com.auth.authentification_service.unit.Service;

import com.auth.authentification_service.Repository.InvitationRepository;
import com.auth.authentification_service.Service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testSendInvitationEmail() throws MessagingException {
        // Arrange
        String toEmail = "test@example.com";
        String role = "Admin";
        String entreprise = "OpenAI";
        Long projectId = 1L;
        String token = "abcd1234";

        // Act
        emailService.sendInvitationEmail(toEmail, role, entreprise, projectId, token);

        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendInvitationEmail_nullEmail_shouldThrowException() {
        String toEmail = null;
        String role = "User";
        String entreprise = "OpenAI";
        Long projectId = 2L;
        String token = "tokenXYZ";

        assertThrows(IllegalArgumentException.class, () -> {
            emailService.sendInvitationEmail(toEmail, role, entreprise, projectId, token);
        });
    }
}