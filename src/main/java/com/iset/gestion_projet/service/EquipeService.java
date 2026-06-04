package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.EquipeDTO;
import com.iset.gestion_projet.DTOS.MembreDTO;
import com.iset.gestion_projet.entity.Equipe;
import com.iset.gestion_projet.repository.EquipeRepository;
import com.iset.gestion_projet.repository.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final MembreRepository membreRepository;

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

    @Transactional
    public Equipe findOrCreateBySysteme(String systeme) {
        // 1. Try exact systemeAssocie match
        List<Equipe> bySysteme = equipeRepository.findBySystemeAssocieIgnoreCase(systeme);
        if (!bySysteme.isEmpty()) return bySysteme.get(0);

        // 2. Try name contains
        List<Equipe> byNom = equipeRepository.findByNomContainingIgnoreCase(systeme);
        if (!byNom.isEmpty()) return byNom.get(0);

        // 3. Create new
        Equipe newEquipe = Equipe.builder()
                .nom(systeme + " Team")
                .systemeAssocie(systeme)
                .description("Équipe générée automatiquement pour " + systeme)
                .build();
        return equipeRepository.save(newEquipe);
    }

    private EquipeDTO toDTO(Equipe e) {
        List<MembreDTO> membres = membreRepository.findByEquipe(e)
                .stream().map(m -> MembreDTO.builder()
                        .id(m.getId())
                        .nom(m.getNom())
                        .prenom(m.getPrenom())
                        .email(m.getEmail())
                        .role(m.getRole())
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