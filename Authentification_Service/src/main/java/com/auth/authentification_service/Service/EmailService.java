package com.auth.authentification_service.Service;

import com.auth.authentification_service.Repository.InvitationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private InvitationRepository invitationRepository;

    public void sendInvitationEmail(String toEmail, String role, String entreprise,Long projectId, String token) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom("noreply@agilia.com");
        helper.setTo(toEmail);
        helper.setSubject("Invitation à rejoindre " + entreprise);
        String link = "http://localhost:3000/authentification/signup?token=" + token;
        helper.setText(
                "<p>Vous avez été invité à rejoindre <strong>" + entreprise +  "</strong> en tant que <strong>" + role + "</strong>.</p>" +
                        "<p><a href=\"" + link + "\">Créer votre compte</a></p>",
                true
        );
        mailSender.send(message);
        System.out.println("Email envoyé à " + toEmail);
    }
}