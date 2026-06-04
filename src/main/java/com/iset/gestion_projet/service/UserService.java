package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.UserResponse;
import com.iset.gestion_projet.Request.UserRequest;
import com.iset.gestion_projet.entity.*;
import com.iset.gestion_projet.repository.EquipeRepository;
import com.iset.gestion_projet.repository.MembreRepository;
import com.iset.gestion_projet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final EquipeRepository equipeRepository;
    private final MembreRepository membreRepository;

    // ── Shared mapper ──
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

    /**
     * Employee requests an account (no equipe, no password yet)
     */
    @Transactional
    public UserResponse demanderCompte(UserRequest request) {
        log.info("📝 Demande de création de compte pour: {}", request.getEmail());

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

        User saved = userRepository.save(user);
        log.info("✅ Demande de compte créée pour: {}", saved.getEmail());

        return toResponse(saved);
    }

    /**
     * ADMIN: creates User + Membre in the same equipe
     */
    @Transactional
    public UserResponse creerCompte(UserRequest request) {
        log.info("👤 ADMIN - Création de compte pour: {}", request.getEmail());

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

        // 1. Create User (can login)
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

        // 2. Auto-create Membre in same equipe (gets AI task assignments)
        Membre membre = Membre.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .role(request.getRole() != null ? request.getRole().name() : "TECHNIQUE")
                .statut(StatutCompte.ACCEPTE)  // Utilisation de l'enum directement
                .equipe(equipe)
                .build();
        membreRepository.save(membre);

        log.info("✅ Compte créé pour: {} avec équipe: {}", user.getEmail(), equipe.getNom());

        return toResponse(user);
    }

    /**
     * ADMIN: assign/change equipe of existing user + sync Membre
     */
    @Transactional
    public UserResponse assignerEquipe(Long userId, Long equipeId) {
        log.info("🔄 Assignation équipe pour utilisateur ID: {} -> équipe ID: {}", userId, equipeId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));
        Equipe equipe = equipeRepository.findById(equipeId)
                .orElseThrow(() -> new RuntimeException("Équipe introuvable : " + equipeId));

        user.setEquipe(equipe);
        userRepository.save(user);

        // Sync Membre equipe too
        Optional<Membre> membreOpt = membreRepository.findByEmail(user.getEmail());
        if (membreOpt.isPresent()) {
            Membre membre = membreOpt.get();
            membre.setEquipe(equipe);
            membreRepository.save(membre);
            log.info("✅ Équipe synchronisée pour le membre: {}", membre.getEmail());
        } else {
            // Create membre if doesn't exist
            log.warn("⚠️ Membre non trouvé pour: {}, création automatique", user.getEmail());
            Membre newMembre = Membre.builder()
                    .nom(user.getNom())
                    .prenom(user.getPrenom())
                    .email(user.getEmail())
                    .role(user.getRole() != null ? user.getRole().name() : "TECHNIQUE")
                    .statut(user.getStatut())  // Utilisation de l'enum directement
                    .equipe(equipe)
                    .build();
            membreRepository.save(newMembre);
        }

        return toResponse(user);
    }

    /**
     * ADMIN: Accept user account and sync Membre with status ACCEPTE
     */
    @Transactional
    public UserResponse accepterCompte(Long id, String password) {
        log.info("✅ Acceptation du compte utilisateur ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        if (user.getStatut() != StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Ce compte a déjà été traité.");
        }

        // Update User
        user.setPassword(passwordEncoder.encode(password));
        user.setStatut(StatutCompte.ACCEPTE);
        userRepository.save(user);

        // Sync Membre: create or update with status ACCEPTE
        updateOrCreateMembre(user, StatutCompte.ACCEPTE);

        // Send email
        emailService.sendAccountAcceptedEmail(
                user.getEmail(), user.getNom(), user.getPrenom(), password);

        log.info("✅ Compte utilisateur #{} accepté et membre synchronisé", id);

        return toResponse(user);
    }

    /**
     * ADMIN: Refuse user account and sync Membre with status REFUSE
     */
    @Transactional
    public UserResponse refuserCompte(Long id) {
        log.info("❌ Refus du compte utilisateur ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        if (user.getStatut() != StatutCompte.EN_ATTENTE) {
            throw new RuntimeException("Ce compte a déjà été traité.");
        }

        // Update User
        user.setStatut(StatutCompte.REFUSE);
        userRepository.save(user);

        // Sync Membre: update status to REFUSE
        updateOrCreateMembre(user, StatutCompte.REFUSE);

        // Send email
        emailService.sendAccountRefusedEmail(
                user.getEmail(), user.getPrenom(), user.getNom());

        log.info("❌ Compte utilisateur #{} refusé et membre synchronisé", id);

        return toResponse(user);
    }

    /**
     * Updates or creates a Membre based on User status
     * @param user The user to sync
     * @param statutMembre The StatutCompte to apply to the member
     */
    private void updateOrCreateMembre(User user, StatutCompte statutMembre) {
        Optional<Membre> existingMembre = membreRepository.findByEmail(user.getEmail());

        if (existingMembre.isPresent()) {
            Membre membre = existingMembre.get();
            membre.setStatut(statutMembre);
            membre.setNom(user.getNom());
            membre.setPrenom(user.getPrenom());
            if (user.getRole() != null) {
                membre.setRole(user.getRole().name());
            }
            if (user.getEquipe() != null) {
                membre.setEquipe(user.getEquipe());
            }
            membreRepository.save(membre);

            log.info("🔄 Membre mis à jour: {} {} - Statut: {}",
                    membre.getPrenom(), membre.getNom(), statutMembre);
        } else if (statutMembre == StatutCompte.ACCEPTE) {
            // Create new membre only if account is accepted
            Membre newMembre = Membre.builder()
                    .nom(user.getNom())
                    .prenom(user.getPrenom())
                    .email(user.getEmail())
                    .role(user.getRole() != null ? user.getRole().name() : "UTILISATEUR")
                    .statut(statutMembre)
                    .equipe(user.getEquipe())
                    .build();
            membreRepository.save(newMembre);
            log.info("✅ Nouveau membre créé: {} {}", newMembre.getPrenom(), newMembre.getNom());
        }
    }

    /**
     * Get all pending account requests
     */
    public List<UserResponse> getDemandesEnAttente() {
        return userRepository.findByStatut(StatutCompte.EN_ATTENTE)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get user by ID
     */
    public UserResponse getUserById(Long id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé.")));
    }

    /**
     * Get all users
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Get users by role
     */
    public List<UserResponse> getByRole(Role role) {
        return userRepository.findByRole(role)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Update user information
     */
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        log.info("✏️ Mise à jour utilisateur ID: {}", id);

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

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setRole(request.getRole());

        if (request.getEquipeId() != null) {
            Equipe equipe = equipeRepository.findById(request.getEquipeId())
                    .orElseThrow(() -> new RuntimeException("Équipe introuvable : " + request.getEquipeId()));
            user.setEquipe(equipe);
        }

        User saved = userRepository.save(user);

        // Sync membre with the user's new status
        updateOrCreateMembre(saved, saved.getStatut());

        log.info("✅ Utilisateur #{} mis à jour", id);

        return toResponse(saved);
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("🗑️ Suppression utilisateur ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));

        // Delete associated membre
        membreRepository.findByEmail(user.getEmail()).ifPresent(membreRepository::delete);

        userRepository.deleteById(id);
        log.info("✅ Utilisateur #{} supprimé", id);
    }

    /**
     * Login user
     */
    public User login(String email, String password) {
        log.info("🔐 Tentative de connexion: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email introuvable."));

        if (user.getStatut() != StatutCompte.ACCEPTE) {
            throw new RuntimeException("Compte non encore activé ou refusé.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect.");
        }

        log.info("✅ Connexion réussie pour: {}", email);

        return user;
    }

    /**
     * Synchronize all existing users with membre table (for migration)
     */
    @Transactional
    public void synchroniserTousLesUtilisateurs() {
        log.info("🔄 Synchronisation de tous les utilisateurs avec la table membre");

        List<User> allUsers = userRepository.findAll();
        int compteur = 0;

        for (User user : allUsers) {
            updateOrCreateMembre(user, user.getStatut());
            compteur++;
        }

        log.info("✅ Synchronisation terminée: {} utilisateurs traités", compteur);
    }
}