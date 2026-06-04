package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.MembreDTO;
import com.iset.gestion_projet.DTOS.MembreRequest;
import com.iset.gestion_projet.entity.Equipe;
import com.iset.gestion_projet.entity.Membre;
import com.iset.gestion_projet.repository.EquipeRepository;
import com.iset.gestion_projet.repository.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembreService {

    private final MembreRepository membreRepository;
    private final EquipeRepository equipeRepository;

    public List<MembreDTO> getAll() {
        return membreRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<MembreDTO> getByEquipe(Long equipeId) {
        return membreRepository.findByEquipeId(equipeId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public MembreDTO create(MembreRequest req) {
        Equipe equipe = equipeRepository.findById(req.getEquipeId())
                .orElseThrow(() -> new RuntimeException("Équipe introuvable"));

        Membre membre = Membre.builder()
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .email(req.getEmail())
                .role(req.getRole())
                .equipe(equipe)
                .build();

        return toDTO(membreRepository.save(membre));
    }

    public MembreDTO update(Long id, MembreRequest req) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membre introuvable"));
        Equipe equipe = equipeRepository.findById(req.getEquipeId())
                .orElseThrow(() -> new RuntimeException("Équipe introuvable"));

        membre.setNom(req.getNom());
        membre.setPrenom(req.getPrenom());
        membre.setEmail(req.getEmail());
        membre.setRole(req.getRole());
        membre.setEquipe(equipe);

        return toDTO(membreRepository.save(membre));
    }

    public void delete(Long id) {
        membreRepository.deleteById(id);
    }

    private MembreDTO toDTO(Membre m) {
        return MembreDTO.builder()
                .id(m.getId())
                .nom(m.getNom())
                .prenom(m.getPrenom())
                .email(m.getEmail())
                .role(m.getRole())
                .equipeId(m.getEquipe() != null ? m.getEquipe().getId() : null)
                .equipeNom(m.getEquipe() != null ? m.getEquipe().getNom() : null)
                .build();
    }
}