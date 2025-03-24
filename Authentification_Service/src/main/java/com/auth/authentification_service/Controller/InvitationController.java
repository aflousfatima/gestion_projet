package com.auth.authentification_service.Controller;

import com.auth.authentification_service.DTO.InvitationRequest;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Service.InvitationService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {


    @Autowired
    private InvitationService invitationService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> sendInvitation(@RequestBody InvitationRequest request) {
        try {
            invitationService.createAndSendInvitation(request);
            return ResponseEntity.ok("Invitation envoyée avec succès");
        } catch (MessagingException e) {
            return ResponseEntity.status(500).body("Erreur lors de l'envoi de l'email : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors de l'envoi de l'invitation : " + e.getMessage());
        }

    }

    @GetMapping("/verify")
    public ResponseEntity<Invitation> verifyInvitation(@RequestParam String token) {
        try {
            Invitation invitation = invitationService.verifyInvitation(token);
            return ResponseEntity.ok(invitation);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(null);
        }
    }

}