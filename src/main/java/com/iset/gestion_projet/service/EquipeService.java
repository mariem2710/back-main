package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.EquipeDTO;
import com.iset.gestion_projet.DTOS.MembreDTO;
import com.iset.gestion_projet.entity.Equipe;
import com.iset.gestion_projet.entity.Role;
import com.iset.gestion_projet.entity.StatutCompte;
import com.iset.gestion_projet.entity.User;
import com.iset.gestion_projet.repository.EquipeRepository;
import com.iset.gestion_projet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final UserRepository   userRepository;

    private static final List<Role> ROLES_TECHNIQUES =
            List.of(Role.TECHNIQUE, Role.TECHNICIEN);

    public List<EquipeDTO> getAll() {
        return equipeRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public EquipeDTO getById(Long id) {
        return equipeRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Équipe introuvable : " + id));
    }

    public EquipeDTO create(EquipeDTO dto) {
        Equipe equipe = Equipe.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .systemeAssocie(dto.getSystemeAssocie())
                .build();
        return toDTO(equipeRepository.save(equipe));
    }

    public EquipeDTO update(Long id, EquipeDTO dto) {
        Equipe equipe = equipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Équipe introuvable : " + id));
        equipe.setNom(dto.getNom());
        equipe.setDescription(dto.getDescription());
        equipe.setSystemeAssocie(dto.getSystemeAssocie());
        return toDTO(equipeRepository.save(equipe));
    }

    public void delete(Long id) {
        equipeRepository.deleteById(id);
    }

    public List<User> getTechniciens(Long equipeId) {
        Equipe equipe = equipeRepository.findById(equipeId)
                .orElseThrow(() -> new RuntimeException("Équipe introuvable: " + equipeId));
        return userRepository.findTechniciensByEquipeObj(
                ROLES_TECHNIQUES, StatutCompte.ACCEPTE, equipe);
    }

    @Transactional
    public Equipe findOrCreateBySysteme(String systeme) {
        List<Equipe> bySysteme = equipeRepository.findBySystemeAssocieIgnoreCase(systeme);
        if (!bySysteme.isEmpty()) return bySysteme.get(0);

        List<Equipe> byNom = equipeRepository.findByNomContainingIgnoreCase(systeme);
        if (!byNom.isEmpty()) return byNom.get(0);

        log.info("Création équipe pour système: {}", systeme);
        Equipe nouvelle = Equipe.builder()
                .nom(systeme + " Team")
                .description("Équipe générée automatiquement pour " + systeme)
                .systemeAssocie(systeme)
                .build();
        return equipeRepository.save(nouvelle);
    }

    private EquipeDTO toDTO(Equipe e) {
        List<User> techniciens = userRepository.findTechniciensByEquipeObj(
                ROLES_TECHNIQUES, StatutCompte.ACCEPTE, e);

        List<MembreDTO> membres = techniciens.stream()
                .map(u -> MembreDTO.builder()
                        .id(u.getId())
                        .nom(u.getNom())
                        .prenom(u.getPrenom())
                        .email(u.getEmail())
                        .role(u.getRole() != null ? u.getRole().name() : null)
                        .equipeId(e.getId())
                        .equipeNom(e.getNom())
                        .build())
                .collect(Collectors.toList());

        return EquipeDTO.builder()
                .id(e.getId())
                .nom(e.getNom())
                .description(e.getDescription())
                .systemeAssocie(e.getSystemeAssocie())
                .nombreMembres(membres.size())
                .membres(membres)
                .build();
    }
}