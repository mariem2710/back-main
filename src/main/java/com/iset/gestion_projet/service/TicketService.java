package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.*;
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
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SousTicketRepository sousTicketRepository;
    private final TacheRepository tacheRepository;
    private final AIService aiService;

    // ──────────────────────────────────────────────────────────────
    // CREATE
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public TicketResponse createTicket(TicketRequest request) {
        log.info("Création d'un nouveau ticket: {}", request.getTitre());

        Ticket ticket = Ticket.builder()
                .titre(request.getTitre())
                .description(request.getDescription())
                .statut(Statut.A_FAIRE)
                .priorite(request.getPriorite() != null ? request.getPriorite() : Priorite.MOYENNE)
                .dateSouhaite(request.getDateSouhaite())
                .dateCreation(LocalDate.now())
                .dateMiseAJour(LocalDate.now())
                .analyseIAEffectuee(false)
                .systemesDetectes(new ArrayList<>())
                .build();

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket créé avec ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    // ──────────────────────────────────────────────────────────────
    // READ
    // ──────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        try {
            return ticketRepository.findAll()
                    .stream()
                    .map(this::mapToResponseSafe)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur getAllTickets: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        Ticket ticket = findTicket(id);
        return mapToResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getByStatut(Statut statut) {
        return ticketRepository.findByStatut(statut)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────
    // UPDATE
    // ──────────────────────────────────────────────────────────────
    @Transactional
    public TicketResponse updateTicket(Long id, TicketRequest request) {
        Ticket ticket = findTicket(id);

        if (ticket.getStatut() != Statut.A_FAIRE) {
            throw new RuntimeException("Ce ticket ne peut pas être modifié, statut actuel: " + ticket.getStatut());
        }

        if (request.getTitre() != null) {
            ticket.setTitre(request.getTitre());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriorite() != null) {
            ticket.setPriorite(request.getPriorite());
        }
        if (request.getDateSouhaite() != null) {
            ticket.setDateSouhaite(request.getDateSouhaite());
        }

        ticket.setDateMiseAJour(LocalDate.now());
        Ticket updated = ticketRepository.save(ticket);
        log.info("Ticket #{} mis à jour", id);

        return mapToResponse(updated);
    }

    @Transactional
    public TicketResponse approveTicket(Long id) {
        Ticket ticket = findTicket(id);
        ticket.setStatut(Statut.APPROUVE);
        ticket.setDateMiseAJour(LocalDate.now());
        Ticket updated = ticketRepository.save(ticket);
        log.info("Ticket #{} approuvé", id);
        return mapToResponse(updated);
    }

    @Transactional
    public TicketResponse rejectTicket(Long id) {
        Ticket ticket = findTicket(id);
        ticket.setStatut(Statut.REJETE);
        ticket.setDateMiseAJour(LocalDate.now());
        Ticket updated = ticketRepository.save(ticket);
        log.info("Ticket #{} rejeté", id);
        return mapToResponse(updated);
    }

    @Transactional
    public TicketResponse analyserTicket(Long id) {
        Ticket ticket = findTicket(id);
        log.info("Début analyse IA pour ticket #{}", id);

        try {
            AIAnalyzeResponse aiResponse = aiService.analyserEtCreerSousTickets(ticket);

            if (aiResponse != null && aiResponse.isSuccess()) {
                ticket.setAiSummary(aiResponse.getSummary());
                ticket.setCauseRacine(aiResponse.getRootCause());
                ticket.setSystemesDetectes(aiResponse.getSystemsDetected());
                ticket.setAnalyseIAEffectuee(true);
                ticket.setDateMiseAJour(LocalDate.now());

                Ticket updated = ticketRepository.save(ticket);
                log.info("Analyse IA terminée avec succès pour ticket #{}", id);
                return mapToResponse(updated);
            } else {
                log.warn("L'IA n'a pas retourné de réponse valide pour le ticket #{}", id);
                return mapToResponse(ticket);
            }
        } catch (Exception e) {
            log.error("Erreur analyse IA ticket #{}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erreur analyse IA : " + e.getMessage());
        }
    }

    @Transactional
    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
        log.info("Ticket #{} supprimé", id);
    }

    // ──────────────────────────────────────────────────────────────
    // MAPPING SÉCURISÉ
    // ──────────────────────────────────────────────────────────────

    private TicketResponse mapToResponseSafe(Ticket ticket) {
        try {
            return mapToResponse(ticket);
        } catch (Exception e) {
            log.error("Erreur mapping ticket #{}: {}", ticket.getId(), e.getMessage());
            return TicketResponse.builder()
                    .id(ticket.getId())
                    .titre(ticket.getTitre() != null ? ticket.getTitre() : "")
                    .description("")
                    .statut(ticket.getStatut() != null ? ticket.getStatut() : Statut.A_FAIRE)
                    .priorite(ticket.getPriorite() != null ? ticket.getPriorite() : Priorite.MOYENNE)
                    .sousTickets(Collections.emptyList())
                    .systemesDetectes(Collections.emptyList())
                    .build();
        }
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        if (ticket == null) return null;

        // Récupération des sous-tickets
        List<SousTicketResponse> sousTicketResponses = getSousTicketResponses(ticket.getId());

        // Calcul des statistiques
        int nbCommentaires = getNombreCommentaires(ticket.getId());
        double progression = calculerProgressionTicket(ticket.getId());

        return TicketResponse.builder()
                .id(ticket.getId())
                .titre(nullSafe(ticket.getTitre(), ""))
                .description(nullSafe(ticket.getDescription(), ""))
                .statut(ticket.getStatut())
                .priorite(ticket.getPriorite())
                .dateCreation(ticket.getDateCreation())
                .dateSouhaite(ticket.getDateSouhaite())
                .dateMiseAJour(ticket.getDateMiseAJour())
                .nombreCommentaires(nbCommentaires)
                .nombreSousTickets(sousTicketResponses.size())
                .progression(progression)
                .sousTickets(sousTicketResponses)
                .aiSummary(nullSafe(ticket.getAiSummary(), null))
                .systemesDetectes(ticket.getSystemesDetectes() != null ? ticket.getSystemesDetectes() : Collections.emptyList())
                .causeRacine(nullSafe(ticket.getCauseRacine(), null))
                .analyseIAEffectuee(ticket.isAnalyseIAEffectuee())
                .createdBy(nullSafe(ticket.getCreatedBy(), null))
                .createdById(ticket.getCreatedById())
                .build();
    }

    private List<SousTicketResponse> getSousTicketResponses(Long ticketId) {
        try {
            List<SousTicket> sousTickets = sousTicketRepository.findByTicketId(ticketId);
            if (sousTickets == null || sousTickets.isEmpty()) {
                return Collections.emptyList();
            }
            return sousTickets.stream()
                    .map(this::mapSousTicket)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Erreur chargement sous-tickets pour ticket #{}: {}", ticketId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private SousTicketResponse mapSousTicket(SousTicket st) {
        if (st == null) return null;

        // Récupération des tâches
        List<TacheResponse> tacheResponses = getTacheResponses(st.getId());

        // Gestion sécurisée de la date
        LocalDateTime dateCreation = null;
        if (st.getDateCreation() != null) {
            dateCreation = st.getDateCreation().atStartOfDay();
        }

        return SousTicketResponse.builder()
                .id(st.getId())
                .titre(nullSafe(st.getTitre(), ""))
                .description(nullSafe(st.getDescription(), ""))
                .ticketId(Long.valueOf(st.getTicket() != null ? Math.toIntExact(st.getTicket().getId()) : null))
                .priorite(st.getPriorite() != null ? st.getPriorite().name() : "MOYENNE")
                .statut(st.getStatut() != null ? st.getStatut().name() : "A_FAIRE")
                .systemeImpacte(nullSafe(st.getSystemeImpacte(), "Non spécifié"))
                .equipeNom(st.getEquipe() != null ? st.getEquipe().getNom() : null)
                .generePar(nullSafe(st.getGenerePar(), "IA"))
                .dateCreation(dateCreation)
                .taches(tacheResponses)
                .build();
    }

    private List<TacheResponse> getTacheResponses(Long sousTicketId) {
        try {
            List<Tache> taches = tacheRepository.findBySousTicketId(sousTicketId);
            if (taches == null || taches.isEmpty()) {
                return Collections.emptyList();
            }
            return taches.stream()
                    .map(this::mapTache)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Erreur chargement tâches pour sous-ticket #{}: {}", sousTicketId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private TacheResponse mapTache(Tache tache) {
        if (tache == null) return null;

        // Gestion sécurisée de la date limite
        LocalDateTime dateLimite = null;
        if (tache.getDateLimite() != null) {
            dateLimite = tache.getDateLimite();
        }

        return TacheResponse.builder()
                .id(tache.getId())
                .titre(nullSafe(tache.getTitre(), ""))
                .description(nullSafe(tache.getDescription(), ""))
                .statut(tache.getStatut())
                .priorite(tache.getPriorite())
                .dateCreation(
                        tache.getDateCreation() != null
                                ? tache.getDateCreation().toLocalDate()
                                : null
                )

                .dateLimite(
                        tache.getDateLimite() != null
                                ? tache.getDateLimite().toLocalDate()
                                : null
                )
                .sousTicketId(tache.getSousTicket() != null ? tache.getSousTicket().getId() : null)
                .sousTicketTitre(tache.getSousTicket() != null ? tache.getSousTicket().getTitre() : null)
                .ticketId(tache.getSousTicket() != null && tache.getSousTicket().getTicket() != null
                        ? tache.getSousTicket().getTicket().getId() : null)
                // ✅ Dans mapTache() — remplacer le bloc assignee par :
                .assigneeId(getAssigneeId(tache))
                .assigneeNom(getAssigneeNom(tache))
                .assigneePrenom(getAssigneePrenom(tache))
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // CALCUL PROGRESSION
    // ──────────────────────────────────────────────────────────────

    private double calculerProgressionTicket(Long ticketId) {
        try {
            List<SousTicket> sousTickets = sousTicketRepository.findByTicketId(ticketId);
            if (sousTickets == null || sousTickets.isEmpty()) {
                return 0.0;
            }

            long totalTaches = 0;
            long tachesFaites = 0;

            for (SousTicket st : sousTickets) {
                List<Tache> taches = tacheRepository.findBySousTicketId(st.getId());
                if (taches != null && !taches.isEmpty()) {
                    totalTaches += taches.size();
                    tachesFaites += taches.stream()
                            .filter(t -> t.getStatut() == Statut.Fait)
                            .count();
                }
            }

            if (totalTaches == 0) return 0.0;
            return Math.round((tachesFaites * 100.0 / totalTaches) * 10.0) / 10.0;
        } catch (Exception e) {
            log.warn("Erreur calcul progression ticket #{}: {}", ticketId, e.getMessage());
            return 0.0;
        }
    }

    private int getNombreCommentaires(Long ticketId) {
        try {
            return (int) ticketRepository.countCommentairesByTicketId(ticketId);
        } catch (Exception e) {
            log.warn("Erreur count commentaires ticket #{}: {}", ticketId, e.getMessage());
            return 0;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // HELPERS NULL-SAFE
    // ──────────────────────────────────────────────────────────────

    private String nullSafe(String value, String defaultValue) {
        return value != null && !value.trim().isEmpty() ? value : defaultValue;
    }

    // ✅ Remplacer ces 3 méthodes dans TicketService.java

    private Long getAssigneeId(Tache tache) {
        return tache.getAssignee() != null
                ? tache.getAssignee().getId()
                : null;
    }

    private String getAssigneeNom(Tache tache) {
        return tache.getAssignee() != null
                ? tache.getAssignee().getNom()
                : null;
    }

    private String getAssigneePrenom(Tache tache) {
        return tache.getAssignee() != null
                ? tache.getAssignee().getPrenom()
                : null;
    }

    private Ticket findTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket introuvable : " + id));
    }

    // Dans TicketService.java
    @Transactional(readOnly = true)
    public Map<String, Object> getTicketProgression(Long id) {
        Ticket ticket = findTicket(id);
        List<SousTicket> sousTickets = sousTicketRepository.findByTicketId(id);

        long totalTaches = 0;
        long tachesFaites = 0;

        for (SousTicket st : sousTickets) {
            List<Tache> taches = tacheRepository.findBySousTicketId(st.getId());
            totalTaches += taches.size();
            tachesFaites += taches.stream()
                    .filter(t -> t.getStatut() == Statut.Fait)
                    .count();
        }

        double progression = totalTaches == 0 ? 0 : (tachesFaites * 100.0 / totalTaches);
        double progressionArrondie = Math.round(progression * 10.0) / 10.0;

        // ✅ FERMETURE AUTOMATIQUE DU TICKET À 100%
        if (progressionArrondie >= 100.0 && ticket.getStatut() != Statut.TERMINE) {
            log.info("🎉 Ticket #{} atteint 100% de progression - Fermeture automatique", id);
            ticket.setStatut(Statut.TERMINE);
            ticket.setDateMiseAJour(LocalDate.now());
            ticketRepository.save(ticket);
        }

        return Map.of(
                "ticketId", id,
                "progression", progressionArrondie,
                "totalTaches", totalTaches,
                "tachesFaites", tachesFaites,
                "estFerme", progressionArrondie >= 100.0
        );
    }
    // ✅ Ajouter dans ticket.service.ts



    }

