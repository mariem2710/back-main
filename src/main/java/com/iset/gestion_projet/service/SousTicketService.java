package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.SousTicketResponse;
import com.iset.gestion_projet.DTOS.TacheResponse;
import com.iset.gestion_projet.entity.*;
import com.iset.gestion_projet.repository.SousTicketRepository;
import com.iset.gestion_projet.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SousTicketService {

    private static final Logger logger = LoggerFactory.getLogger(SousTicketService.class);

    private final SousTicketRepository sousTicketRepository;
    private final TacheRepository tacheRepository;
    private final EquipeService equipeService;

    @Transactional(readOnly = true)
    public List<SousTicketResponse> getByTicketId(Long ticketId) {
        logger.info("Récupération des sous-tickets pour le ticket ID: {}", ticketId);

        List<SousTicket> sousTickets = sousTicketRepository.findByTicketIdWithEquipe(ticketId);

        if (sousTickets == null || sousTickets.isEmpty()) {
            logger.info("Aucun sous-ticket trouvé");
            return List.of();
        }

        return sousTickets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SousTicketResponse getById(Long id) {
        SousTicket sousTicket = sousTicketRepository.findById(id).orElse(null);
        if (sousTicket == null) {
            throw new RuntimeException("Sous-ticket non trouvé: " + id);
        }
        return mapToResponse(sousTicket);
    }

    private SousTicketResponse mapToResponse(SousTicket sousTicket) {
        if (sousTicket == null) return null;

        List<Tache> taches = tacheRepository.findBySousTicketId(sousTicket.getId());
        double progression = calculerProgressionSousTicket(sousTicket.getId(), taches);

        Equipe equipe = sousTicket.getEquipe();

        // Gestion sécurisée de la date
        LocalDateTime dateCreation = null;
        if (sousTicket.getDateCreation() != null) {
            dateCreation = sousTicket.getDateCreation().atStartOfDay();
        }

        return SousTicketResponse.builder()
                .id(sousTicket.getId())
                .titre(sousTicket.getTitre())
                .description(sousTicket.getDescription())
                .systemeImpacte(sousTicket.getSystemeImpacte())
                .priorite(sousTicket.getPriorite() != null ? sousTicket.getPriorite().name() : "MOYENNE")
                .statut(sousTicket.getStatut() != null ? sousTicket.getStatut().name() : "A_FAIRE")
                .generePar(sousTicket.getGenerePar())
                .dateCreation(dateCreation)
                .equipeId(equipe != null ? equipe.getId() : null)
                .equipeNom(equipe != null ? equipe.getNom() : null)
                .progression(progression)
                .taches(taches.stream()
                        .map(this::mapTacheToResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private TacheResponse mapTacheToResponse(Tache tache) {
        if (tache == null) return null;

        // Gestion sécurisée de la date limite
        LocalDateTime dateLimite = null;
        if (tache.getDateLimite() != null) {
            dateLimite = tache.getDateLimite().atStartOfDay();
        }

        return TacheResponse.builder()
                .id(tache.getId())
                .titre(tache.getTitre())
                .description(tache.getDescription())
                .priorite(tache.getPriorite())
                .statut(tache.getStatut())
                .dateCreation(tache.getDateCreation())
                .dateLimite(dateLimite)
                .assigneeId(tache.getAssignee() != null ? tache.getAssignee().getId() :
                        (tache.getAssigneA() != null ? tache.getAssigneA().getId() : null))
                .assigneeNom(tache.getAssignee() != null ? tache.getAssignee().getNom() :
                        (tache.getAssigneA() != null ? tache.getAssigneA().getNom() : null))
                .build();
    }

    private double calculerProgressionSousTicket(Long sousTicketId, List<Tache> taches) {
        if (taches == null || taches.isEmpty()) {
            return 0.0;
        }

        long totalTaches = taches.size();
        long completedTaches = taches.stream()
                .filter(t -> t.getStatut() != null && t.getStatut() == Statut.Fait)
                .count();

        return totalTaches > 0 ? (completedTaches * 100.0) / totalTaches : 0.0;
    }

    @Transactional
    public SousTicket createSousTicket(SousTicket sousTicket) {
        logger.info("Création sous-ticket: {}", sousTicket.getTitre());

        if (sousTicket.getStatut() == null) {
            sousTicket.setStatut(Statut.A_faire);
        }
        if (sousTicket.getDateCreation() == null) {
            sousTicket.setDateCreation(LocalDate.now());
        }
        if (sousTicket.getPriorite() == null) {
            sousTicket.setPriorite(Priorite.MOYENNE);
        }

        if (sousTicket.getEquipe() == null && sousTicket.getSystemeImpacte() != null) {
            try {
                Equipe equipe = equipeService.findOrCreateBySysteme(sousTicket.getSystemeImpacte());
                sousTicket.setEquipe(equipe);
                logger.info("Équipe '{}' assignée", equipe.getNom());
            } catch (Exception e) {
                logger.warn("Impossible d'assigner une équipe: {}", e.getMessage());
            }
        }

        return sousTicketRepository.save(sousTicket);
    }

    public List<SousTicket> getAll() {
        return sousTicketRepository.findAll();
    }

    @Transactional
    public SousTicketResponse changerStatut(Long id, Statut statut) {
        logger.info("Changement statut sous-ticket {} -> {}", id, statut);

        SousTicket sousTicket = sousTicketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sous-ticket non trouvé: " + id));

        sousTicket.setStatut(statut);
        SousTicket saved = sousTicketRepository.save(sousTicket);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculerProgression(Long ticketId) {
        logger.info("Calcul progression ticket ID: {}", ticketId);

        List<SousTicket> sousTickets = sousTicketRepository.findByTicketIdWithEquipe(ticketId);

        if (sousTickets == null || sousTickets.isEmpty()) {
            return Map.of(
                    "progression", 0.0,
                    "totalSousTickets", 0,
                    "message", "Aucun sous-ticket trouvé"
            );
        }

        double progressionTotale = sousTickets.stream()
                .mapToDouble(st -> {
                    List<Tache> taches = tacheRepository.findBySousTicketId(st.getId());
                    return calculerProgressionSousTicket(st.getId(), taches);
                })
                .average()
                .orElse(0.0);

        return Map.of(
                "progression", Math.round(progressionTotale),
                "totalSousTickets", sousTickets.size(),
                "sousTicketsTermines", sousTickets.stream()
                        .filter(st -> st.getStatut() == Statut.Fait)
                        .count(),
                "sousTickets", sousTickets.stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList())
        );
    }
}