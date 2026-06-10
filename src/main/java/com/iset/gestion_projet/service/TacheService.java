package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.TacheResponse;
import com.iset.gestion_projet.entity.*;
import com.iset.gestion_projet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TacheService {

    private final TacheRepository      tacheRepository;
    private final SousTicketRepository sousTicketRepository;
    private final UserRepository       userRepository;
    private final TicketRepository     ticketRepository;
    private final AIService            aiService;

    // ══════════════════════════════════════════════════════════════
    //  GÉNÉRATION
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public List<TacheResponse> genererTachesIA(Long sousTicketId) {
        log.info("🎯 Génération tâches IA — sous-ticket #{}", sousTicketId);

        SousTicket sousTicket = sousTicketRepository.findById(sousTicketId)
                .orElseThrow(() -> new RuntimeException("SousTicket introuvable: " + sousTicketId));

        List<Tache> existantes = tacheRepository.findBySousTicketId(sousTicketId);
        if (!existantes.isEmpty()) {
            tacheRepository.deleteAll(existantes);
            tacheRepository.flush();
        }

        List<Tache> taches = aiService.genererTachesPourSousTicket(sousTicket);
        return taches.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════
    //  GESTION
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public TacheResponse prendreEnCharge(Long tacheId, Long employeId) {
        Tache tache = findTache(tacheId);
        User  user  = findUser(employeId);

        tache.setAssignee(user);
        tache.setStatut(Statut.En_cours);
        return toResponse(tacheRepository.save(tache));
    }

    @Transactional
    public TacheResponse terminerTache(Long tacheId, Long employeId) {
        log.info("🔧 Terminaison tâche #{} par employé #{}", tacheId, employeId);

        Tache tache = findTache(tacheId);
        User  user  = findUser(employeId);

        // ✅ Vérification assignation — un seul champ assignee
        if (tache.getAssignee() == null ||
                !tache.getAssignee().getId().equals(employeId)) {
            throw new RuntimeException(
                    "Cette tâche n'est pas assignée à l'employé #" + employeId);
        }

        if (tache.getStatut() == Statut.Fait) {
            throw new RuntimeException("La tâche #" + tacheId + " est déjà terminée");
        }

        tache.setStatut(Statut.Fait);
        tache = tacheRepository.save(tache);
        log.info("✅ Tâche #{} terminée", tacheId);

        // Vérifier fermeture automatique du ticket
        if (tache.getSousTicket() != null && tache.getSousTicket().getTicket() != null) {
            verifierEtFermerTicket(tache.getSousTicket().getTicket());
        }

        return toResponse(tache);
    }

    private void verifierEtFermerTicket(Ticket ticket) {
        List<SousTicket> sousTickets = sousTicketRepository.findByTicketId(ticket.getId());
        long total     = 0;
        long terminees = 0;

        for (SousTicket st : sousTickets) {
            List<Tache> taches = tacheRepository.findBySousTicketId(st.getId());
            total     += taches.size();
            terminees += taches.stream().filter(t -> t.getStatut() == Statut.Fait).count();
        }

        double progression = total == 0 ? 0 : (terminees * 100.0 / total);

        if (progression >= 100.0 && ticket.getStatut() != Statut.TERMINE) {
            log.info("🎉 Ticket #{} → 100% — Fermeture automatique", ticket.getId());
            ticket.setStatut(Statut.TERMINE);
            ticket.setDateMiseAJour(LocalDate.now());
            ticketRepository.save(ticket);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PROGRESSION
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> calculerProgressionTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket introuvable: " + ticketId));

        List<SousTicket> sousTickets = sousTicketRepository.findByTicketId(ticketId);
        long total     = 0;
        long terminees = 0;

        for (SousTicket st : sousTickets) {
            List<Tache> taches = tacheRepository.findBySousTicketId(st.getId());
            total     += taches.size();
            terminees += taches.stream().filter(t -> t.getStatut() == Statut.Fait).count();
        }

        double progression    = total == 0 ? 0 : (terminees * 100.0 / total);
        double progArrondie   = Math.round(progression * 10.0) / 10.0;
        boolean estFerme      = progArrondie >= 100.0;

        if (estFerme && ticket.getStatut() != Statut.TERMINE) {
            ticket.setStatut(Statut.TERMINE);
            ticket.setDateMiseAJour(LocalDate.now());
            ticketRepository.save(ticket);
        }

        return Map.of(
                "ticketId",       ticketId,
                "titre",          ticket.getTitre(),
                "statut",         ticket.getStatut().name(),
                "totalTaches",    total,
                "tachesFaites",   terminees,
                "progression",    progArrondie,
                "progressionPct", String.format("%.1f%%", progArrondie),
                "ferme",          estFerme
        );
    }

    // ══════════════════════════════════════════════════════════════
    //  AIDE IA
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public String envoyerProblemeIA(Long tacheId, String probleme) {
        Tache tache = findTache(tacheId);
        String prompt = String.format(
                "Un technicien est bloqué sur une tâche. " +
                        "Propose une solution concrète et technique en français.\n\n" +
                        "Tâche : %s\nDescription : %s\nProblème : %s\n\nSolution :",
                tache.getTitre(), tache.getDescription(), probleme
        );
        return aiService.analyze(prompt);
    }

    // ══════════════════════════════════════════════════════════════
    //  REQUÊTES
    // ══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<TacheResponse> getMesTaches(Long employeId) {
        findUser(employeId); // Vérifier existence
        return tacheRepository.findAllTachesForUser(employeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheResponse> getBySousTicket(Long sousTicketId) {
        return tacheRepository.findBySousTicketId(sousTicketId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheResponse> getByTicket(Long ticketId) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket introuvable: " + ticketId));
        return sousTicketRepository.findByTicketId(ticketId)
                .stream()
                .flatMap(st -> tacheRepository.findBySousTicketId(st.getId()).stream())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════
    //  MAPPING
    // ══════════════════════════════════════════════════════════════

    private TacheResponse toResponse(Tache tache) {
        if (tache == null) return null;

        TacheResponse response = TacheResponse.builder()
                .id(tache.getId())
                .titre(tache.getTitre())
                .description(tache.getDescription())
                .statut(tache.getStatut())
                .priorite(tache.getPriorite())
                .dateCreation(tache.getDateCreation() != null
                        ? tache.getDateCreation().toLocalDate() : null)
                .dateLimite(tache.getDateLimite() != null
                        ? tache.getDateLimite().toLocalDate() : null)
                .build();

        if (tache.getSousTicket() != null) {
            response.setSousTicketId(tache.getSousTicket().getId());
            response.setSousTicketTitre(tache.getSousTicket().getTitre());
            if (tache.getSousTicket().getTicket() != null) {
                response.setTicketId(tache.getSousTicket().getTicket().getId());
            }
        }

        // ✅ Un seul champ assignee
        if (tache.getAssignee() != null) {
            response.setAssigneeId(tache.getAssignee().getId());
            response.setAssigneeNom(tache.getAssignee().getNom());
            response.setAssigneePrenom(tache.getAssignee().getPrenom());
        }

        return response;
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private Tache findTache(Long id) {
        return tacheRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche introuvable: " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));
    }
}