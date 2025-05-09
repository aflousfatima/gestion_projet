package com.auth.authentification_service.unit.Controller;

import com.auth.authentification_service.Controller.InvitationController;
import com.auth.authentification_service.DTO.InvitationRequest;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Service.InvitationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = InvitationController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "MANAGER")
    public void whenSendInvitation_thenReturnSuccess() throws Exception {
        // Given
        InvitationRequest request = new InvitationRequest();
        request.setProjectId(1L);
        request.setEmail("test@example.com");
        request.setEntreprise("Test Entreprise");
        request.setRole("USER");

        doNothing().when(invitationService).createAndSendInvitation(any(InvitationRequest.class));

        // When & Then
        mockMvc.perform(post("/api/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Invitation envoyée avec succès"));

        verify(invitationService).createAndSendInvitation(any(InvitationRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void whenSendInvitationWithMessagingException_thenReturnError() throws Exception {
        // Given
        InvitationRequest request = new InvitationRequest();
        request.setProjectId(1L);
        request.setEmail("test@example.com");
        request.setEntreprise("Test Entreprise");
        request.setRole("USER");

        doThrow(new MessagingException("Erreur d'envoi")).when(invitationService).createAndSendInvitation(any(InvitationRequest.class));

        // When & Then
        mockMvc.perform(post("/api/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur lors de l'envoi de l'email : Erreur d'envoi"));

        verify(invitationService).createAndSendInvitation(any(InvitationRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void whenSendInvitationWithGenericException_thenReturnError() throws Exception {
        // Given
        InvitationRequest request = new InvitationRequest();
        request.setProjectId(1L);
        request.setEmail("test@example.com");
        request.setEntreprise("Test Entreprise");
        request.setRole("USER");

        doThrow(new RuntimeException("Erreur générique")).when(invitationService).createAndSendInvitation(any(InvitationRequest.class));

        // When & Then
        mockMvc.perform(post("/api/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Erreur lors de l'envoi de l'invitation : Erreur générique"));

        verify(invitationService).createAndSendInvitation(any(InvitationRequest.class));
    }

    @Test
    public void whenVerifyInvitationValidToken_thenReturnInvitation() throws Exception {
        // Given
        Invitation invitation = new Invitation();
        invitation.setToken("test-token");
        invitation.setProjectId(1L);
        invitation.setEmail("test@example.com");
        invitation.setEntreprise("Test Entreprise");
        invitation.setRole("USER");
        invitation.setExpiresAt(2l);
        invitation.setUsed(false);

        when(invitationService.verifyInvitation("test-token")).thenReturn(invitation);

        // When & Then
        mockMvc.perform(get("/api/invitations/verify")
                        .param("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.projectId").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.entreprise").value("Test Entreprise"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.used").value(false));

        verify(invitationService).verifyInvitation("test-token");
    }

    @Test
    public void whenVerifyInvitationInvalidToken_thenReturnBadRequest() throws Exception {
        // Given
        when(invitationService.verifyInvitation("invalid-token")).thenThrow(new RuntimeException("Token invalide"));

        // When & Then
        mockMvc.perform(get("/api/invitations/verify")
                        .param("token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));

        verify(invitationService).verifyInvitation("invalid-token");
    }


}
