package com.iset.gestion_projet.bootstrap;

import com.iset.gestion_projet.entity.Role;
import com.iset.gestion_projet.entity.StatutCompte;
import com.iset.gestion_projet.entity.User;
import com.iset.gestion_projet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@tt.tn}")
    private String adminEmail;

    @Value("${app.admin.password:Admin2026!}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        // ✅ Ne crée rien si un ADMIN existe déjà
        if (userRepository.findByRole(Role.ADMIN).size() > 0) {
            log.info("✅ Admin existe déjà.");
            return;
        }

        if (adminEmail == null || adminEmail.isBlank() ||
                adminPassword == null || adminPassword.isBlank()) {
            log.warn("⚠️ Email/password admin vides — aucun admin créé.");
            return;
        }

        if (userRepository.findByEmail(adminEmail.trim()).isPresent()) {
            log.warn("⚠️ Email {} déjà utilisé.", adminEmail);
            return;
        }

        User admin = User.builder()
                .nom("Admin")
                .prenom("System")
                .email(adminEmail.trim())
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .statut(StatutCompte.ACCEPTE)
                .build();

        userRepository.save(admin);
        log.info("🎉 Compte ADMIN créé : {} | mot de passe : {}",
                adminEmail, adminPassword);
        log.warn("⚠️  Changez le mot de passe en production !");
    }
}
