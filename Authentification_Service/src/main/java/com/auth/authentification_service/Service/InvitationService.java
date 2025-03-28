package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.InvitationRequest;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Repository.InvitationRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InvitationService {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private EmailService emailService;

    public void createAndSendInvitation(InvitationRequest request) throws MessagingException {
        // Générer un jeton unique
        String token = UUID.randomUUID().toString();

        // Enregistrer l'invitation dans la base de données
        Invitation invitation = new Invitation(
                request.getEmail(),
                request.getRole(),
                request.getEntreprise(),
                request.getProjectId() ,
                token,
                System.currentTimeMillis() + 48 * 60 * 60 * 1000 // Expire dans 48h
        );
        invitationRepository.save(invitation);

        // Envoyer l'email
        emailService.sendInvitationEmail(
                request.getEmail(),
                request.getRole(),
                request.getEntreprise(), // À remplacer par une récupération dynamique plus tard
                request.getProjectId() ,
                token
        );
    }

    public Invitation verifyInvitation(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Jeton d'invitation invalide"));

        if (invitation.getExpiresAt() < System.currentTimeMillis()) {
            throw new RuntimeException("Lien d'invitation expiré");
        }

        return invitation;
    }
}