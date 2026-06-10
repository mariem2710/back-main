package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.MembreDTO;
import com.iset.gestion_projet.DTOS.MembreRequest;
import com.iset.gestion_projet.entity.Equipe;
import com.iset.gestion_projet.entity.Role;
import com.iset.gestion_projet.entity.StatutCompte;
import com.iset.gestion_projet.entity.User;
import com.iset.gestion_projet.repository.EquipeRepository;
import com.iset.gestion_projet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembreService {

    private final UserRepository    userRepository;
    private final EquipeRepository  equipeRepository;

    private static final List<Role> ROLES_TECHNIQUES =
            List.of(Role.TECHNIQUE, Role.TECHNICIEN);

    // ── Tous les techniciens actifs ───────────────────────────────
    public List<MembreDTO> getAll() {
        return userRepository
                .findTechniciens(ROLES_TECHNIQUES, StatutCompte.ACCEPTE)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Techniciens d'une équipe ──────────────────────────────────
    public List<MembreDTO> getByEquipe(Long equipeId) {
        Equipe equipe = equipeRepository.findById(equipeId)
                .orElseThrow(() -> new RuntimeException("Équipe introuvable"));
        return userRepository
                .findTechniciensByEquipeObj(ROLES_TECHNIQUES, StatutCompte.ACCEPTE, equipe)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── Créer un technicien (crée un User TECHNIQUE) ──────────────
    public MembreDTO create(MembreRequest req) {
        Equipe equipe = equipeRepository.findById(req.getEquipeId())
                .orElseThrow(() -> new RuntimeException("Équipe introuvable"));

        User user = User.builder()
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .email(req.getEmail())
                .role(Role.TECHNIQUE)
                .statut(StatutCompte.ACCEPTE)
                .equipe(equipe)
                .build();

        return toDTO(userRepository.save(user));
    }

    // ── Mettre à jour un technicien ───────────────────────────────
    public MembreDTO update(Long id, MembreRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Technicien introuvable"));
        Equipe equipe = equipeRepository.findById(req.getEquipeId())
                .orElseThrow(() -> new RuntimeException("Équipe introuvable"));

        user.setNom(req.getNom());
        user.setPrenom(req.getPrenom());
        user.setEmail(req.getEmail());
        user.setEquipe(equipe);

        return toDTO(userRepository.save(user));
    }

    // ── Supprimer ─────────────────────────────────────────────────
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    // ── Mapping ───────────────────────────────────────────────────
    private MembreDTO toDTO(User u) {
        return MembreDTO.builder()
                .id(u.getId())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .email(u.getEmail())
                .role(u.getRole() != null ? u.getRole().name() : "TECHNIQUE")
                .equipeId(u.getEquipe() != null ? u.getEquipe().getId() : null)
                .equipeNom(u.getEquipe() != null ? u.getEquipe().getNom() : null)
                .build();
    }
}