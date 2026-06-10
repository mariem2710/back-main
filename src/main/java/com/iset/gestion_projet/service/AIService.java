package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.AIAnalyzeResponse;
import com.iset.gestion_projet.entity.*;
import com.iset.gestion_projet.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final EquipeRepository    equipeRepository;
    private final UserRepository      userRepository;      // ✅ Plus de MembreRepository
    private final SousTicketRepository sousTicketRepository;
    private final TacheRepository     tacheRepository;
    private final TicketRepository    ticketRepository;
    private final RestTemplate        restTemplate;

    // ✅ Rôles techniques autorisés pour l'assignation
    private static final List<Role> ROLES_TECHNIQUES =
            List.of(Role.TECHNIQUE, Role.TECHNICIEN);

    // ══════════════════════════════════════════════════════════════
    //  ANALYSE PRINCIPALE
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public AIAnalyzeResponse analyserEtCreerSousTickets(Ticket ticket) {
        log.info("🤖 Début analyse IA — ticket #{}: {}", ticket.getId(), ticket.getTitre());

        try {
            // 1. Appeler l'API Python
            Map<String, Object> aiResponse = callAnalyzeAPI(ticket);

            if (aiResponse == null) {
                return buildError("L'API IA a retourné une réponse null");
            }

            if (!Boolean.TRUE.equals(aiResponse.get("success"))) {
                return buildError("Erreur API IA: " +
                        aiResponse.getOrDefault("error", "Erreur inconnue"));
            }

            // 2. Extraire les données
            String       summary          = (String) aiResponse.getOrDefault("summary", "");
            String       rootCause        = (String) aiResponse.getOrDefault("root_cause", "");
            List<String> systemsDetected  = castList(aiResponse.get("systems_detected"));
            List<Map<String, Object>> sousTicketsIA =
                    castListOfMaps(aiResponse.get("sous_tickets"));

            log.info("📊 Systèmes détectés: {} | Sous-tickets IA: {}",
                    systemsDetected, sousTicketsIA.size());

            // 3. Créer les sous-tickets depuis la réponse IA
            List<SousTicket> createdSousTickets = new ArrayList<>();

            if (!sousTicketsIA.isEmpty()) {
                // ✅ Utiliser les sous-tickets générés par l'IA (avec tâches incluses)
                for (Map<String, Object> stData : sousTicketsIA) {
                    SousTicket st = createSousTicketFromIA(ticket, stData);
                    if (st != null) {
                        createdSousTickets.add(st);
                    }
                }
            } else {
                // Fallback : créer un sous-ticket par système détecté
                List<Map<String, Object>> technicalTickets =
                        castListOfMaps(aiResponse.get("technical_tickets"));

                for (Map<String, Object> tt : technicalTickets) {
                    String systeme     = (String) tt.get("system");
                    String titre       = (String) tt.get("title");
                    String description = (String) tt.get("description");
                    String priority    = (String) tt.get("priority");

                    SousTicket st = createSousTicketForSystem(
                            ticket, systeme, titre, description, priority);
                    if (st != null) {
                        createdSousTickets.add(st);
                        genererTachesPourSousTicket(st);
                    }
                }
            }

            // 4. Mettre à jour le ticket
            ticket.setAiSummary(summary);
            ticket.setCauseRacine(rootCause);
            ticket.setSystemesDetectes(systemsDetected);
            ticket.setAnalyseIAEffectuee(true);
            ticket.setDateMiseAJour(LocalDate.now());
            ticketRepository.save(ticket);

            log.info("✅ Analyse terminée — {} sous-ticket(s) créé(s)",
                    createdSousTickets.size());

            return AIAnalyzeResponse.builder()
                    .success(true)
                    .summary(summary)
                    .rootCause(rootCause)
                    .systemsDetected(systemsDetected)
                    .nombreSousTickets(createdSousTickets.size())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur analyse IA: {}", e.getMessage(), e);
            return buildError("Erreur: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  CRÉATION SOUS-TICKET DEPUIS LA RÉPONSE IA
    // ══════════════════════════════════════════════════════════════

    /**
     * Crée un sous-ticket à partir des données générées par l'IA Python.
     * Les tâches sont déjà dans la réponse avec les techniciens assignés.
     */
    @Transactional
    private SousTicket createSousTicketFromIA(
            Ticket ticket,
            Map<String, Object> stData) {

        String equipeNom    = (String) stData.getOrDefault("equipe", "IT");
        String titre        = (String) stData.getOrDefault("titre",  "Sous-ticket");
        String description  = (String) stData.getOrDefault("description", "");
        String prioriteStr  = (String) stData.getOrDefault("priorite", "MEDIUM");
        List<Map<String, Object>> tachesData = castListOfMaps(stData.get("taches"));

        // Récupérer l'équipe
        Equipe equipe = getEquipeFromDatabase(equipeNom);

        // Créer le sous-ticket
        SousTicket sousTicket = SousTicket.builder()
                .ticket(ticket)
                .titre(titre)
                .description(description)
                .systemeImpacte(equipeNom)
                .priorite(convertPriority(prioriteStr))
                .statut(Statut.A_FAIRE)
                .generePar("IA - Mistral")
                .dateCreation(LocalDate.now())
                .equipe(equipe)
                .build();

        SousTicket saved = sousTicketRepository.save(sousTicket);
        log.info("✅ Sous-ticket créé: #{} — {}", saved.getId(), saved.getTitre());

        // Compteur de charge par technicien pour ce sous-ticket
        Map<Long, Integer> chargeParTechnicien = new HashMap<>();

        // Créer les tâches avec assignation équitable
        List<Tache> taches = new ArrayList<>();
        List<Long>  idsDejaAssignesDansCeSousTicket = new ArrayList<>();

        for (Map<String, Object> tacheData : tachesData) {
            String titreTache       = (String) tacheData.getOrDefault("titre", "");
            String descTache        = (String) tacheData.getOrDefault("description", "");
            String prioriteTache    = (String) tacheData.getOrDefault("priorite", "MEDIUM");
            int    dureeHeures      = getInt(tacheData.get("duree_heures"), 4);

            // ✅ Choisir le technicien avec rotation + charge équilibrée
            User technicien = choisirTechnicienEquitable(
                    equipe,
                    chargeParTechnicien,
                    idsDejaAssignesDansCeSousTicket
            );

            Tache tache = Tache.builder()
                    .titre(titreTache)
                    .description(descTache)
                    .sousTicket(saved)
                    .statut(Statut.A_faire)
                    .priorite(convertPriority(prioriteTache))
                    .dateCreation(LocalDateTime.now())
                    .dateLimite(LocalDateTime.now().plusDays(dureeHeures / 8 + 1))
                    .assignee(technicien)    // ✅ User directement
                    .build();

            taches.add(tache);

            if (technicien != null) {
                idsDejaAssignesDansCeSousTicket.add(technicien.getId());
                chargeParTechnicien.merge(technicien.getId(), 1, Integer::sum);
                log.info("👤 Tâche '{}' → {} {}",
                        titreTache,
                        technicien.getPrenom(),
                        technicien.getNom());
            }
        }

        tacheRepository.saveAll(taches);
        log.info("✅ {} tâche(s) créée(s) pour '{}'", taches.size(), saved.getTitre());

        return saved;
    }

    // ══════════════════════════════════════════════════════════════
    //  ASSIGNATION ÉQUITABLE DES TECHNICIENS
    // ══════════════════════════════════════════════════════════════

    /**
     * Choisit le technicien le moins chargé, avec rotation dans le sous-ticket.
     * Filtre : Role IN (TECHNIQUE, TECHNICIEN) + Statut = ACCEPTE + Equipe assignée.
     */
    private User choisirTechnicienEquitable(
            Equipe equipe,
            Map<Long, Integer> chargeParTechnicien,
            List<Long> idsDejaAssignesDansCeSousTicket) {

        // Récupérer les techniciens filtrés de l'équipe
        List<User> techniciens = getTechniciensEquipe(equipe);

        if (techniciens.isEmpty()) {
            log.warn("⚠️ Aucun technicien disponible pour l'équipe '{}'",
                    equipe != null ? equipe.getNom() : "null");
            return null;
        }

        // Trier par charge globale croissante
        List<User> tries = techniciens.stream()
                .sorted(Comparator.comparingInt(
                        t -> chargeParTechnicien.getOrDefault(t.getId(), 0)
                ))
                .collect(Collectors.toList());

        // Préférer un technicien pas encore assigné dans CE sous-ticket
        User choisi = tries.stream()
                .filter(t -> !idsDejaAssignesDansCeSousTicket.contains(t.getId()))
                .findFirst()
                .orElse(tries.get(0)); // fallback : le moins chargé globalement

        log.debug("👤 Technicien sélectionné: {} {} (charge: {})",
                choisi.getPrenom(), choisi.getNom(),
                chargeParTechnicien.getOrDefault(choisi.getId(), 0));

        return choisi;
    }

    /**
     * Récupère les techniciens d'une équipe :
     * Role IN (TECHNIQUE, TECHNICIEN) + Statut = ACCEPTE + equipe non nulle.
     */
    private List<User> getTechniciensEquipe(Equipe equipe) {
        if (equipe != null) {
            List<User> techniciens = userRepository.findTechniciensByEquipeObj(
                    ROLES_TECHNIQUES, StatutCompte.ACCEPTE, equipe);
            if (!techniciens.isEmpty()) {
                return techniciens;
            }
        }
        // Fallback : tous les techniciens actifs toutes équipes
        return userRepository.findTechniciens(ROLES_TECHNIQUES, StatutCompte.ACCEPTE);
    }

    // ══════════════════════════════════════════════════════════════
    //  CRÉATION SOUS-TICKET (fallback technique_tickets)
    // ══════════════════════════════════════════════════════════════

    private SousTicket createSousTicketForSystem(
            Ticket ticket,
            String systeme,
            String titre,
            String description,
            String priority) {

        Equipe equipe = getEquipeFromDatabase(systeme);

        SousTicket sousTicket = SousTicket.builder()
                .ticket(ticket)
                .titre(titre != null ? titre : "[" + systeme + "] " + ticket.getTitre())
                .description(description != null ? description : "À traiter: " + systeme)
                .systemeImpacte(systeme)
                .priorite(convertPriority(priority))
                .statut(Statut.A_FAIRE)
                .generePar("IA - Mistral")
                .dateCreation(LocalDate.now())
                .equipe(equipe)
                .build();

        SousTicket saved = sousTicketRepository.save(sousTicket);
        log.info("✅ Sous-ticket fallback: #{} — {}", saved.getId(), saved.getTitre());
        return saved;
    }

    // ══════════════════════════════════════════════════════════════
    //  GÉNÉRATION TÂCHES (appelée manuellement depuis TacheService)
    // ══════════════════════════════════════════════════════════════

    @Transactional
    public List<Tache> genererTachesPourSousTicket(SousTicket sousTicket) {
        log.info("🎯 Génération tâches pour sous-ticket #{}: {}",
                sousTicket.getId(), sousTicket.getTitre());

        // Supprimer les anciennes tâches
        List<Tache> existantes = tacheRepository.findBySousTicketId(sousTicket.getId());
        if (!existantes.isEmpty()) {
            tacheRepository.deleteAll(existantes);
            tacheRepository.flush();
        }

        Equipe equipe = sousTicket.getEquipe();
        if (equipe == null) {
            equipe = getEquipeFromDatabase(sousTicket.getSystemeImpacte());
        }

        // Générer les descriptions
        List<String> descriptions = getTachesDescriptions(
                sousTicket.getSystemeImpacte(), sousTicket.getDescription());

        Map<Long, Integer> chargeParTechnicien        = new HashMap<>();
        List<Long>         idsAssignesDansCeSousTicket = new ArrayList<>();
        List<Tache>        taches                      = new ArrayList<>();

        for (String desc : descriptions) {
            User technicien = choisirTechnicienEquitable(
                    equipe, chargeParTechnicien, idsAssignesDansCeSousTicket);

            Tache tache = Tache.builder()
                    .titre(desc.length() > 80 ? desc.substring(0, 77) + "…" : desc)
                    .description(desc)
                    .sousTicket(sousTicket)
                    .statut(Statut.A_faire)
                    .priorite(sousTicket.getPriorite())
                    .dateCreation(LocalDateTime.now())
                    .assignee(technicien)
                    .build();

            taches.add(tache);

            if (technicien != null) {
                idsAssignesDansCeSousTicket.add(technicien.getId());
                chargeParTechnicien.merge(technicien.getId(), 1, Integer::sum);
            }
        }

        List<Tache> saved = tacheRepository.saveAll(taches);
        log.info("✅ {} tâches générées", saved.size());
        return saved;
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private Map<String, Object> callAnalyzeAPI(Ticket ticket) {
        String url = "http://localhost:8000/api/v1/analyze";
        Map<String, Object> request = new HashMap<>();
        request.put("title",       ticket.getTitre());
        request.put("description", ticket.getDescription());
        request.put("ticket_id",   String.valueOf(ticket.getId()));
        request.put("priority",    ticket.getPriorite() != null
                ? ticket.getPriorite().name() : "MEDIUM");

        try {
            long start    = System.currentTimeMillis();
            var  response = restTemplate.postForEntity(url, request, Map.class);
            log.info("⏱ Réponse IA en {}ms", System.currentTimeMillis() - start);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("❌ Erreur appel API IA: {}", e.getMessage());
        }
        return null;
    }

    public String analyze(String prompt) {
        String url = "http://localhost:8000/api/v1/analyze-text";
        try {
            Map<String, String> request = new HashMap<>();
            request.put("prompt", prompt);
            var response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().getOrDefault("result", "Aucune solution trouvée");
            }
        } catch (Exception e) {
            log.error("Erreur IA: {}", e.getMessage());
        }
        return "Service IA indisponible";
    }

    private Equipe getEquipeFromDatabase(String systeme) {
        if (systeme == null || systeme.isBlank()) return null;

        List<Equipe> equipes = equipeRepository.findBySystemeAssocieIgnoreCase(systeme);
        if (!equipes.isEmpty()) return equipes.get(0);

        equipes = equipeRepository.findByNomContainingIgnoreCase(systeme);
        if (!equipes.isEmpty()) return equipes.get(0);

        return equipeRepository.findByNom("IT Support").orElse(null);
    }

    private Priorite convertPriority(String priority) {
        if (priority == null) return Priorite.MOYENNE;
        return switch (priority.toUpperCase()) {
            case "BASSE",   "LOW"      -> Priorite.BASSE;
            case "HAUTE",   "HIGH"     -> Priorite.HAUTE;
            case "CRITIQUE","CRITICAL" -> Priorite.CRITIQUE;
            default                    -> Priorite.MOYENNE;
        };
    }

    private List<String> getTachesDescriptions(String systeme, String description) {
        String sys = systeme != null ? systeme.toLowerCase() : "";
        return switch (sys) {
            case "crm"            -> List.of(
                    "Analyser les logs du système CRM",
                    "Vérifier les intégrations CRM",
                    "Tester les workflows CRM"
            );
            case "sap"            -> List.of(
                    "Vérifier les logs SAP",
                    "Contrôler les interfaces IDOC",
                    "Valider la configuration SAP"
            );
            case "billing"        -> List.of(
                    "Analyser les transactions en échec",
                    "Vérifier les règles de calcul",
                    "Corriger les transactions bloquées"
            );
            case "authentication" -> List.of(
                    "Vérifier les services d'authentification",
                    "Contrôler les certificats SSL",
                    "Analyser les logs d'accès"
            );
            default               -> List.of(
                    "Analyser le problème signalé",
                    "Diagnostiquer la cause racine",
                    "Appliquer la correction"
            );
        };
    }

    private AIAnalyzeResponse buildError(String message) {
        log.error("❌ {}", message);
        return AIAnalyzeResponse.builder()
                .success(false)
                .error(message)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object obj) {
        if (obj instanceof List<?>) return (List<String>) obj;
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castListOfMaps(Object obj) {
        if (obj instanceof List<?>) return (List<Map<String, Object>>) obj;
        return new ArrayList<>();
    }

    private int getInt(Object obj, int defaultValue) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        return defaultValue;
    }
}