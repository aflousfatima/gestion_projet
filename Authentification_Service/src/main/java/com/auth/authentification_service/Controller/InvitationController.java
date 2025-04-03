package com.auth.authentification_service.Controller;

import com.auth.authentification_service.DTO.InvitationRequest;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Service.InvitationService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
@OpenAPIDefinition(info = @Info(
        title = "API des invitations",
        version = "1.0",
        description = "Cette API permet de gérer les invitations envoyées aux utilisateurs."
))
public class InvitationController {


    @Autowired
    private InvitationService invitationService;

    @Operation(summary = "Envoyer une invitation",
            description = "Cette méthode permet à un manager d'envoyer une invitation à un utilisateur via email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation envoyée avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur lors de l'envoi de l'email ou de l'invitation")
    })
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

    @Operation(summary = "Vérifier une invitation",
            description = "Cette méthode permet de vérifier si une invitation est valide en utilisant un token d'invitation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation vérifiée avec succès"),
            @ApiResponse(responseCode = "400", description = "Invitation invalide ou token incorrect")
    })
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