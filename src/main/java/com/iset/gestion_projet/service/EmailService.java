package com.iset.gestion_projet.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:noreply@example.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAccountAcceptedEmail(String to, String nom,
                                         String prenom, String password) {
        log.info("📧 sendAccountAcceptedEmail called → to={}, mailEnabled={}",
                to, mailEnabled);

        if (!mailEnabled) {
            log.info("📧 [MAIL DÉSACTIVÉ] Email acceptation → {} ({} {})",
                    to, prenom, nom);
            return;
        }

        String subject = "Bienvenue sur la plateforme TicketFlow !";
        String body =
                "Bonjour " + prenom + " " + nom + ",\n\n" +
                        "Votre demande de compte a été acceptée.\n\n" +
                        "Vos identifiants de connexion :\n" +
                        "  Email        : " + to + "\n" +
                        "  Mot de passe : " + password + "\n\n" +
                        "Nous vous conseillons de changer votre mot de passe " +
                        "après votre première connexion.\n\n" +
                        "Cordialement,\nL'équipe TicketFlow";

        sendEmail(to, subject, body);
    }

    public void sendAccountRefusedEmail(String to, String prenom, String nom) {
        log.info("📧 sendAccountRefusedEmail called → to={}, mailEnabled={}",
                to, mailEnabled);

        if (!mailEnabled) {
            log.info("📧 [MAIL DÉSACTIVÉ] Email refus → {} ({} {})",
                    to, prenom, nom);
            return;
        }

        String subject = "Votre demande de compte — TicketFlow";
        String body =
                "Bonjour " + prenom + " " + nom + ",\n\n" +
                        "Nous avons examiné votre demande de création de compte.\n" +
                        "Malheureusement, celle-ci n'a pas pu être acceptée.\n\n" +
                        "Pour toute question, contactez l'administrateur.\n\n" +
                        "Cordialement,\nL'équipe TicketFlow";

        sendEmail(to, subject, body);
    }

    public void sendResetRequestNotification(String userEmail,
                                             String nom, String prenom) {
        log.info("📧 sendResetRequestNotification called → from={}", userEmail);

        if (!mailEnabled) {
            log.info("📧 [MAIL DÉSACTIVÉ] Reset request from: {}", userEmail);
            return;
        }

        String subject = "Demande de réinitialisation de mot de passe";
        String body =
                "Bonjour Admin,\n\n" +
                        "L'utilisateur " + prenom + " " + nom +
                        " (" + userEmail + ")\n" +
                        "a demandé une réinitialisation de son mot de passe.\n\n" +
                        "Veuillez vous connecter à la plateforme pour traiter " +
                        "cette demande.\n\n" +
                        "Cordialement,\nLe système TicketFlow";

        sendEmail(fromEmail, subject, body); // send to admin
    }

    private void sendEmail(String to, String subject, String body) {
        log.info("📤 Tentative envoi email → to={}, subject={}", to, subject);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(message);
            log.info("✅ Email envoyé avec succès à : {}", to);

        } catch (Exception e) {
            log.error("❌ Échec envoi email à {} : {}", to, e.getMessage());
            log.error("❌ Stack trace : ", e);
        }
    }
}