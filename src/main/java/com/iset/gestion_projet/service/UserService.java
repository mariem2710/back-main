package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.UserResponse;
import com.iset.gestion_projet.Request.UserRequest;
import com.iset.gestion_projet.entity.*;
import com.iset.gestion_projet.repository.EquipeRepository;
import com.iset.gestion_projet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository   userRepository;
    private final EmailService     emailService;
    private final PasswordEncoder  passwordEncoder;
    private final EquipeRepository equipeRepository;

    // ── Mapper User → UserResponse ────────────────────────────────
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .role(user.getRole())
                .statut(user.getStatut())
                .equipeId(user.getEquipe() != null ? user.getEquipe().getId() : null)
                .equipeNom(user.getEquipe() != null ? user.getEquipe().getNom() : null)
                .build();
    }

    // ── Demande de compte (public) ────────────────────────────────
    @Transactional
    public UserResponse demanderCompte(UserRequest request) {
        log.info("Demande de compte pour: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .role(request.getRole() != null ? request.getRole() : Role.METIER)
                .statut(StatutCompte.EN_ATTENTE)
                .build();

        return toResponse(userRepository.save(user));
    }

    // ── ADMIN: créer compte directement ──────────────────────────
    @Transactional
    public UserResponse creerCompte(UserRequest request) {
        log.info("ADMIN - Création de compte pour: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Le mot de passe est obligatoire.");
        }
        if (request.getEquipeId() == null) {
            throw new RuntimeException("L'équipe est obligatoire.");
        }

        Equipe equipe = equipeRepository.findById(request.getEquipeId())
                .orElseThrow(() -> new RuntimeException("Équipe introuvable : " + request.getEquipeId()));

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.TECHNIQUE)
                .statut(StatutCompte.ACCEPTE)
                .equipe(equipe)
                .build();

        userRepository.save(user);
        log.info("Compte créé pour: {} avec équipe: {}", user.getEmail(), equipe.getNom());

        return toResponse(user);
    }

    // ── ADMIN: assigner équipe ────────────────────────────────────
    @Transactional
    public UserResponse assignerEquipe(Long userId, Long equipeId) {
        log.info("Assignation équipe {} -> utilisateur {}", equipeId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));
        Equipe equipe = equipeRepository.findById(equipeId)
                .orElseThrow(() -> new RuntimeException("Équipe introuvable : " + equipeId));

        user.setEquipe(equipe);
        userRepository.save(user);

        return toResponse(user);
    }

    // ── ADMIN: accepter compte ────────────────────────────────────
    @Transactional
    public UserResponse accepterCompte(Long id, String password) {
        log.info("Acceptation du compte ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        if (user.getStatut() != StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Ce compte a déjà été traité.");
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setStatut(StatutCompte.ACCEPTE);
        userRepository.save(user);

        emailService.sendAccountAcceptedEmail(
                user.getEmail(), user.getNom(), user.getPrenom(), password);

        log.info("Compte #{} accepté", id);
        return toResponse(user);
    }

    // ── ADMIN: refuser compte ─────────────────────────────────────
    @Transactional
    public UserResponse refuserCompte(Long id) {
        log.info("Refus du compte ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        if (user.getStatut() != StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Ce compte a déjà été traité.");
        }

        user.setStatut(StatutCompte.REFUSE);
        userRepository.save(user);

        emailService.sendAccountRefusedEmail(
                user.getEmail(), user.getPrenom(), user.getNom());

        log.info("Compte #{} refusé", id);
        return toResponse(user);
    }

    // ── Demandes en attente ───────────────────────────────────────
    public List<UserResponse> getDemandesEnAttente() {
        return userRepository.findByStatut(StatutCompte.EN_ATTENTE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Tous les utilisateurs ─────────────────────────────────────
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Par ID ────────────────────────────────────────────────────
    public UserResponse getUserById(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé.")));
    }

    // ── Par rôle ──────────────────────────────────────────────────
    public List<UserResponse> getByRole(Role role) {
        return userRepository.findByRole(role)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Modifier utilisateur ──────────────────────────────────────
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("Mise à jour utilisateur ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Cet email est déjà utilisé.");
            }
        }

        user.setNom(request.getNom());
        user.setPrenom(request.getPrenom());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getEquipeId() != null) {
            Equipe equipe = equipeRepository.findById(request.getEquipeId())
                    .orElseThrow(() -> new RuntimeException("Équipe introuvable : " + request.getEquipeId()));
            user.setEquipe(equipe);
        }

        return toResponse(userRepository.save(user));
    }

    // ── Supprimer utilisateur ─────────────────────────────────────
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé.");
        }
        userRepository.deleteById(id);
        log.info("Utilisateur #{} supprimé", id);
    }

    // ── Login ─────────────────────────────────────────────────────
    public User login(String email, String password) {
        log.info("Tentative de connexion: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email introuvable."));

        if (user.getStatut() != StatutCompte.ACCEPTE) {
            throw new RuntimeException("Compte non encore activé ou refusé.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect.");
        }

        log.info("Connexion réussie pour: {}", email);
        return user;
    }

    // ── Techniciens ───────────────────────────────────────────────
    public List<UserResponse> getTechniciens() {
        return userRepository.findTechniciens(
                List.of(Role.TECHNIQUE, Role.TECHNICIEN),
                StatutCompte.ACCEPTE
        ).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<UserResponse> getTechniciensByEquipe(String nomEquipe) {
        return userRepository.findTechniciensByEquipe(
                List.of(Role.TECHNIQUE, Role.TECHNICIEN),
                StatutCompte.ACCEPTE,
                nomEquipe
        ).stream().map(this::toResponse).collect(Collectors.toList());
    }
}
