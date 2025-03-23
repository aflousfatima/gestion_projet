package com.auth.authentification_service.Service;

import com.auth.authentification_service.DTO.InvitationRequest;
import com.auth.authentification_service.Entity.Invitation;
import com.auth.authentification_service.Repository.InvitationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.UUID;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private InvitationRepository invitationRepository;


    public void createAndSendInvitation(InvitationRequest request) throws MessagingException {
        // Générer un jeton unique
        String token = UUID.randomUUID().toString();

        // Enregistrer l'invitation dans la base de données
        Invitation invitation = new Invitation(
                request.getEmail(),
                request.getRole(),
                request.getEntrepriseId(),
                request.getProject(),
                token,
                System.currentTimeMillis() + 48 * 60 * 60 * 1000 // Expire dans 48h
        );
        invitationRepository.save(invitation);


    }
    public void sendInvitationEmail(String toEmail, String role, String entreprise,String project, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom("noreply@agilia.com");
        helper.setTo(toEmail);
        helper.setSubject("Invitation à rejoindre " + entreprise);
        String link = "http://localhost:3000/authentification/signup?token=" + token;
        helper.setText(
                "<p>Vous avez été invité à rejoindre <strong>" + entreprise + "affecter au projet"+ project + "</strong> en tant que <strong>" + role + "</strong>.</p>" +
                        "<p><a href=\"" + link + "\">Créer votre compte</a></p>",
                true
        );

        mailSender.send(message);
        System.out.println("Email envoyé à " + toEmail);
    }
}