package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.TacheResponse;
import com.iset.gestion_projet.entity.*;
import com.iset.gestion_projet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TacheService {

    private final TacheRepository tacheRepository;
    private final SousTicketRepository sousTicketRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AIService aiService;

    @Transactional
    public List<TacheResponse> genererTachesIA(Long sousTicketId) {
        SousTicket sousTicket = sousTicketRepository
                .findById(sousTicketId)
                .orElseThrow(() -> new RuntimeException("SousTicket introuvable : " + sousTicketId));

        List<Tache> taches = aiService.genererTachesPourSousTicket(sousTicket);

        // Définir les valeurs par défaut
        taches.forEach(tache -> {
            if (tache.getDateCreation() == null) {
                tache.setDateCreation(LocalDateTime.now());
            }
            if (tache.getStatut() == null) {
                tache.setStatut(Statut.A_faire);
            }
            if (tache.getPriorite() == null) {
                tache.setPriorite(Priorite.MOYENNE);
            }
            tache.setSousTicket(sousTicket);
        });

        List<Tache> saved = tacheRepository.saveAll(taches);
        log.info("✅ {} tâches créées pour SousTicket #{}", saved.size(), sousTicketId);

        return saved.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TacheResponse prendreEnCharge(Long tacheId, Long employeId) {
        Tache tache = findTache(tacheId);
        User user = findUser(employeId);

        tache.setAssignee(user);
        tache.setStatut(Statut.En_cours);
        tache = tacheRepository.save(tache);

        log.info("👤 Tâche #{} prise en charge par {}", tacheId, user.getEmail());
        return toResponse(tache);
    }

    @Transactional
    public TacheResponse terminerTache(Long tacheId, Long employeId) {
        Tache tache = findTache(tacheId);
        User user = findUser(employeId);

        // Vérifier l'assignation
        boolean isAssigned = false;
        if (tache.getAssigneA() != null && tache.getAssigneA().getEmail() != null) {
            isAssigned = tache.getAssigneA().getEmail().equals(user.getEmail());
        }
        if (!isAssigned && tache.getAssignee() != null) {
            isAssigned = tache.getAssignee().getId().equals(employeId);
        }

        if (!isAssigned) {
            throw new RuntimeException("Cette tâche n'est pas assignée à l'employé #" + employeId);
        }

        if (tache.getStatut() == Statut.Fait) {
            throw new RuntimeException("La tâche #" + tacheId + " est déjà terminée");
        }

        tache.setStatut(Statut.Fait);
        tache = tacheRepository.save(tache);

        log.info("✅ Tâche #{} terminée", tacheId);
        return toResponse(tache);
    }

    @Transactional(readOnly = true)
    public List<TacheResponse> getMesTaches(Long employeId) {
        User user = findUser(employeId);

        return tacheRepository
                .findAllTachesForUser(employeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheResponse> getBySousTicket(Long sousTicketId) {
        return tacheRepository
                .findBySousTicketId(sousTicketId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TacheResponse> getByTicket(Long ticketId) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket introuvable : " + ticketId));

        List<SousTicket> sousTickets = sousTicketRepository.findByTicketId(ticketId);

        return sousTickets.stream()
                .flatMap(st -> tacheRepository.findBySousTicketId(st.getId()).stream())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public String envoyerProblemeIA(Long tacheId, String probleme) {
        Tache tache = findTache(tacheId);

        String prompt = String.format(
                "Un employé est bloqué sur une tâche. Propose une solution concrète en français.\n\n" +
                        "Tâche : %s\nDescription : %s\nProblème signalé : %s\n\nSolution :",
                tache.getTitre(),
                tache.getDescription(),
                probleme
        );

        String solution = aiService.analyze(prompt);
        log.info("🤖 Solution IA pour tâche #{}: {}", tacheId, solution);
        return solution;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculerProgressionTicket(Long ticketId) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket introuvable : " + ticketId));

        List<SousTicket> sousTickets = sousTicketRepository.findByTicketId(ticketId);

        long totalTaches = 0;
        long tachesFaites = 0;

        for (SousTicket st : sousTickets) {
            List<Tache> taches = tacheRepository.findBySousTicketId(st.getId());
            totalTaches += taches.size();
            tachesFaites += taches.stream()
                    .filter(t -> t.getStatut() == Statut.Fait)
                    .count();
        }

        double progression = totalTaches == 0 ? 0.0 : Math.round((tachesFaites * 100.0 / totalTaches) * 10.0) / 10.0;
        boolean ferme = progression >= 100.0;

        // Fermer le ticket automatiquement si 100%
        if (ferme) {
            ticketRepository.findById(ticketId).ifPresent(t -> {
                if (t.getStatut() != Statut.TERMINE) {
                    t.setStatut(Statut.TERMINE);
                    ticketRepository.save(t);
                    log.info("🎉 Ticket #{} fermé automatiquement", ticketId);
                }
            });
        }

        return Map.of(
                "ticketId", ticketId,
                "totalTaches", totalTaches,
                "tachesFaites", tachesFaites,
                "progression", progression,
                "progressionPct", progression + "%",
                "ferme", ferme
        );
    }

    // ──────────────── PRIVATE HELPERS ────────────────
    private Tache findTache(Long id) {
        return tacheRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tâche introuvable : " + id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé introuvable : " + id));
    }

    private TacheResponse toResponse(Tache t) {
        if (t == null) return null;

        // Gestion sécurisée des dates
        LocalDateTime dateLimite = null;
        if (t.getDateLimite() != null) {
            dateLimite = t.getDateLimite().atStartOfDay();
        }

        TacheResponse response = TacheResponse.builder()
                .id(t.getId())
                .titre(t.getTitre())
                .description(t.getDescription())
                .statut(t.getStatut())
                .priorite(t.getPriorite())
                .dateCreation(t.getDateCreation())
                .dateLimite(dateLimite)
                .build();

        // Sous-ticket
        if (t.getSousTicket() != null) {
            response.setSousTicketId(t.getSousTicket().getId());
            response.setSousTicketTitre(t.getSousTicket().getTitre());

            if (t.getSousTicket().getTicket() != null) {
                response.setTicketId(t.getSousTicket().getTicket().getId());
            }
        }

        // Assignation (priorité au Membre IA, puis User)
        if (t.getAssigneA() != null) {
            response.setAssigneeId(t.getAssigneA().getId());
            response.setAssigneeNom(t.getAssigneA().getNom());
            response.setAssigneePrenom(t.getAssigneA().getPrenom());
        } else if (t.getAssignee() != null) {
            response.setAssigneeId(t.getAssignee().getId());
            response.setAssigneeNom(t.getAssignee().getNom());
            response.setAssigneePrenom(t.getAssignee().getPrenom());
        }

        return response;
    }
}