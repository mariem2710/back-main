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

    private final EquipeRepository equipeRepository;
    private final MembreRepository membreRepository;
    private final SousTicketRepository sousTicketRepository;
    private final TacheRepository tacheRepository;
    private final TicketRepository ticketRepository;
    private final RestTemplate restTemplate;

    /**
     * Analyse un ticket et crée des sous-tickets avec leurs tâches
     * C'est la méthode principale appelée par TicketService.analyserTicket()
     */
    @Transactional
    public AIAnalyzeResponse analyserEtCreerSousTickets(Ticket ticket) {
        log.info("========================================");
        log.info("🤖 Début de l'analyse IA pour le ticket #{}: {}", ticket.getId(), ticket.getTitre());

        try {
            // 1. Appeler l'API Python pour analyser le ticket
            Map<String, Object> aiResponse = callAnalyzeAPI(ticket);

            if (aiResponse == null) {
                log.error("❌ La réponse de l'API IA est null");
                return AIAnalyzeResponse.builder()
                        .success(false)
                        .error("L'API IA a retourné une réponse null")
                        .build();
            }

            // Vérifier le champ success
            boolean isSuccess = Boolean.TRUE.equals(aiResponse.get("success"));

            if (!isSuccess) {
                String errorMsg = (String) aiResponse.getOrDefault("error", "Erreur inconnue");
                log.error("❌ L'API IA a indiqué une erreur: {}", errorMsg);
                return AIAnalyzeResponse.builder()
                        .success(false)
                        .error("Erreur API IA: " + errorMsg)
                        .build();
            }

            // 2. Extraire les données de la réponse
            String summary = (String) aiResponse.getOrDefault("summary", "");
            String rootCause = (String) aiResponse.getOrDefault("root_cause", "");
            List<String> systemsDetected = (List<String>) aiResponse.getOrDefault("systems_detected", new ArrayList<>());
            List<Map<String, Object>> technicalTickets = (List<Map<String, Object>>) aiResponse.getOrDefault("technical_tickets", new ArrayList<>());

            // Vérifier si les données sont valides
            if (technicalTickets.isEmpty()) {
                if (!systemsDetected.isEmpty()) {
                    log.info("📝 Création de tickets par défaut pour les systèmes: {}", systemsDetected);
                    for (String systeme : systemsDetected) {
                        Map<String, Object> defaultTicket = new HashMap<>();
                        defaultTicket.put("system", systeme);
                        defaultTicket.put("title", "[" + systeme + "] " + ticket.getTitre());
                        defaultTicket.put("description", "À traiter par l'équipe " + systeme + ". " + summary);
                        defaultTicket.put("priority", "MOYENNE");
                        technicalTickets.add(defaultTicket);
                    }
                } else {
                    log.error("❌ Aucun système détecté et aucun ticket technique");
                    return AIAnalyzeResponse.builder()
                            .success(false)
                            .error("Aucun système détecté ni ticket technique dans la réponse")
                            .build();
                }
            }

            log.info("🏗️ CRÉATION DES SOUS-TICKETS");

            // 3. Créer les sous-tickets pour chaque système détecté
            List<SousTicket> createdSousTickets = new ArrayList<>();

            for (Map<String, Object> techTicket : technicalTickets) {
                String systeme = (String) techTicket.get("system");
                String titre = (String) techTicket.get("title");
                String description = (String) techTicket.get("description");
                String priority = (String) techTicket.get("priority");

                // Créer le sous-ticket
                SousTicket sousTicket = createSousTicketForSystem(ticket, systeme, titre, description, priority);
                createdSousTickets.add(sousTicket);

                // Générer les tâches pour ce sous-ticket
                List<Tache> taches = genererTachesPourSousTicket(sousTicket);
                log.info("✅ {} tâches générées pour le sous-ticket #{}", taches.size(), sousTicket.getId());
            }

            // 4. Mettre à jour le ticket
            ticket.setAiSummary(summary);
            ticket.setCauseRacine(rootCause);
            ticket.setSystemesDetectes(systemsDetected);
            ticket.setAnalyseIAEffectuee(true);
            ticket.setDateMiseAJour(LocalDate.now());
            ticketRepository.save(ticket);

            log.info("✅ Analyse terminée avec succès! {} sous-ticket(s) créé(s)", createdSousTickets.size());
            log.info("========================================");

            return AIAnalyzeResponse.builder()
                    .success(true)
                    .summary(summary)
                    .rootCause(rootCause)
                    .systemsDetected(systemsDetected)
                    .nombreSousTickets(createdSousTickets.size())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'analyse IA: {}", e.getMessage(), e);
            return AIAnalyzeResponse.builder()
                    .success(false)
                    .error("Erreur: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Appelle l'API Python pour analyser le ticket
     */
    private Map<String, Object> callAnalyzeAPI(Ticket ticket) {
        String aiServiceUrl = "http://localhost:8000/api/v1/analyze";

        Map<String, Object> request = new HashMap<>();
        request.put("title", ticket.getTitre());
        request.put("description", ticket.getDescription());
        request.put("ticket_id", String.valueOf(ticket.getId()));
        request.put("priority", ticket.getPriorite() != null ? ticket.getPriorite().name() : "MEDIUM");

        log.info("📡 Appel de l'API IA: {}", aiServiceUrl);

        try {
            long startTime = System.currentTimeMillis();
            var response = restTemplate.postForEntity(aiServiceUrl, request, Map.class);
            long endTime = System.currentTimeMillis();

            log.info("⏱️ Temps de réponse: {} ms", (endTime - startTime));
            log.info("📊 Status code: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✅ Réponse reçue de l'API IA");
                return response.getBody();
            } else {
                log.error("❌ Erreur API IA: status {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("❌ Exception lors de l'appel API IA: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Crée un sous-ticket pour un système spécifique
     */
    private SousTicket createSousTicketForSystem(Ticket ticket, String systeme, String titre, String description, String priority) {
        // Récupérer l'équipe associée au système
        Equipe equipe = getEquipeFromDatabase(systeme);
        if (equipe == null) {
            log.warn("⚠️ Aucune équipe trouvée pour: {}, utilisation IT Support", systeme);
            equipe = equipeRepository.findByNom("IT Support").orElse(null);
        }

        Priorite priorite = convertPriority(priority);

        SousTicket sousTicket = SousTicket.builder()
                .ticket(ticket)
                .titre(titre != null && !titre.isEmpty() ? titre : "[" + systeme + "] " + ticket.getTitre())
                .description(description != null && !description.isEmpty() ? description : "À traiter par l'équipe " + systeme)
                .systemeImpacte(systeme)
                .priorite(priorite)
                .statut(Statut.A_FAIRE)
                .generePar("IA - Mistral")
                .dateCreation(LocalDate.now())
                .equipe(equipe)
                .build();

        SousTicket saved = sousTicketRepository.save(sousTicket);
        log.info("✅ Sous-ticket créé: ID={}, Titre={}", saved.getId(), saved.getTitre());

        return saved;
    }

    /**
     * Convertit la priorité reçue de l'API en enum Priorite
     */
    private Priorite convertPriority(String priority) {
        if (priority == null) return Priorite.MOYENNE;
        switch (priority.toUpperCase()) {
            case "BASSE": case "LOW": return Priorite.BASSE;
            case "HAUTE": case "HIGH": return Priorite.HAUTE;
            case "CRITIQUE": case "CRITICAL": return Priorite.CRITIQUE;
            default: return Priorite.MOYENNE;
        }
    }

    /**
     * Génère des tâches pour un sous-ticket avec assignation uniquement aux membres ACCEPTE
     */
    @Transactional
    public List<Tache> genererTachesPourSousTicket(SousTicket sousTicket) {
        log.info("========================================");
        log.info("🎯 Génération de tâches pour le sous-ticket #{}: {}", sousTicket.getId(), sousTicket.getTitre());
        log.info("📌 Système impacté: {}", sousTicket.getSystemeImpacte());

        List<Tache> taches = new ArrayList<>();
        String systeme = sousTicket.getSystemeImpacte();

        // 1. Supprimer les anciennes tâches
        List<Tache> existingTaches = tacheRepository.findBySousTicketId(sousTicket.getId());
        if (!existingTaches.isEmpty()) {
            log.info("🗑️ Suppression de {} anciennes tâches", existingTaches.size());
            tacheRepository.deleteAll(existingTaches);
            tacheRepository.flush();
        }

        // 2. Récupérer l'équipe
        Equipe equipe = sousTicket.getEquipe();
        if (equipe == null) {
            equipe = getEquipeFromDatabase(systeme);
        }

        if (equipe == null) {
            log.warn("⚠️ Aucune équipe trouvée pour: {}, utilisation IT Support", systeme);
            equipe = equipeRepository.findByNom("IT Support").orElse(null);
        }

        // 3. Récupérer UNIQUEMENT les membres avec statut ACCEPTE
        List<Membre> membresAcceptes = getMembresAcceptesFromDatabase(equipe);

        log.info("🏢 Équipe: {}", equipe != null ? equipe.getNom() : "Aucune");
        log.info("✅ Membres ACCEPTE trouvés: {}", membresAcceptes.size());

        if (!membresAcceptes.isEmpty()) {
            log.info("📋 Liste des membres validés:");
            for (Membre membre : membresAcceptes) {
                log.info("   - {} {} ({}) - Email: {} - Statut: {}",
                        membre.getPrenom(), membre.getNom(), membre.getRole(),
                        membre.getEmail(), membre.getStatut());
            }
        } else {
            log.warn("⚠️ Aucun membre ACCEPTE trouvé!");
        }

        // 4. Générer les descriptions des tâches
        List<String> descriptionsTaches = getTachesDescriptions(systeme, sousTicket.getDescription());
        log.info("📝 {} tâches à générer", descriptionsTaches.size());

        // 5. Créer et répartir les tâches
        Map<Long, Integer> compteurTachesParMembre = new HashMap<>();

        for (int i = 0; i < descriptionsTaches.size(); i++) {
            Tache tache = new Tache();
            String description = descriptionsTaches.get(i);

            String titre = String.format("[%s] Tâche %d: %s",
                    systeme != null ? systeme.toUpperCase() : "GENERAL",
                    i + 1,
                    description.length() > 80 ? description.substring(0, 77) + "..." : description
            );
            tache.setTitre(titre);
            tache.setDescription(description);
            tache.setSousTicket(sousTicket);
            tache.setStatut(Statut.A_FAIRE);
            tache.setPriorite(sousTicket.getPriorite());
            tache.setDateCreation(LocalDateTime.now());

            // Assigner uniquement à un membre ACCEPTE
            if (!membresAcceptes.isEmpty()) {
                Membre membreAssigne = assignerMembreRoundRobin(membresAcceptes, i);
                tache.setAssigneA(membreAssigne);
                compteurTachesParMembre.merge(membreAssigne.getId(), 1, Integer::sum);
                log.debug("✅ Tâche assignée à {} {}", membreAssigne.getPrenom(), membreAssigne.getNom());
            } else {
                log.error("❌ Aucun membre ACCEPTE disponible!");
                tache.setDescription(description + "\n\n⚠️ ATTENTION: Aucun membre validé disponible!");
            }

            taches.add(tache);
        }

        // 6. Sauvegarder les tâches
        List<Tache> savedTaches = tacheRepository.saveAll(taches);
        log.info("✅ {} tâches générées", savedTaches.size());

        // 7. Afficher le récapitulatif
        log.info("📊 RÉPARTITION DES TÂCHES PAR MEMBRE:");
        for (Map.Entry<Long, Integer> entry : compteurTachesParMembre.entrySet()) {
            Membre membre = membresAcceptes.stream()
                    .filter(m -> m.getId().equals(entry.getKey()))
                    .findFirst().orElse(null);
            if (membre != null) {
                log.info("   👤 {} {}: {} tâche(s)", membre.getPrenom(), membre.getNom(), entry.getValue());
            }
        }
        log.info("========================================");

        return savedTaches;
    }

    /**
     * Récupère uniquement les membres avec statut ACCEPTE
     */
    private List<Membre> getMembresAcceptesFromDatabase(Equipe equipe) {
        List<Membre> membresAcceptes = new ArrayList<>();

        if (equipe != null) {
            // Utiliser la méthode avec StatutCompte.ACCEPTE
            membresAcceptes = membreRepository.findByEquipeAndStatut(equipe, StatutCompte.ACCEPTE);
            log.debug("Membres ACCEPTE dans '{}': {}", equipe.getNom(), membresAcceptes.size());
        }

        // Si aucun membre trouvé dans l'équipe, chercher tous les membres ACCEPTE
        if (membresAcceptes.isEmpty()) {
            membresAcceptes = membreRepository.findByStatut(StatutCompte.ACCEPTE);
            log.debug("Total membres ACCEPTE dans la base: {}", membresAcceptes.size());
        }

        // Filtrer ceux avec email valide
        membresAcceptes = membresAcceptes.stream()
                .filter(m -> m.getEmail() != null && !m.getEmail().isEmpty())
                .collect(Collectors.toList());

        return membresAcceptes;
    }

    /**
     * Récupère l'équipe depuis la base de données
     */
    private Equipe getEquipeFromDatabase(String systeme) {
        if (systeme == null || systeme.trim().isEmpty()) {
            return null;
        }

        // Recherche par systemeAssocie
        List<Equipe> equipes = equipeRepository.findBySystemeAssocieIgnoreCase(systeme);
        if (!equipes.isEmpty()) {
            return equipes.get(0);
        }

        // Recherche par nom
        equipes = equipeRepository.findByNomContainingIgnoreCase(systeme);
        if (!equipes.isEmpty()) {
            return equipes.get(0);
        }

        // Mapping
        String teamName = mapSystemToTeamName(systeme);
        if (teamName != null) {
            Optional<Equipe> equipe = equipeRepository.findByNom(teamName);
            if (equipe.isPresent()) {
                return equipe.get();
            }
        }

        return equipeRepository.findByNom("IT Support").orElse(null);
    }

    /**
     * Mappe un système vers un nom d'équipe
     */
    private String mapSystemToTeamName(String systeme) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("CRM", "CRM Team");
        mapping.put("SAP", "SAP Team");
        mapping.put("Billing", "Billing Team");
        mapping.put("Authentication", "Security Team");
        mapping.put("Mobile App", "Mobile Team");
        mapping.put("Mobile", "Mobile Team");
        mapping.put("Website", "Web Team");
        mapping.put("Web", "Web Team");
        mapping.put("Reporting", "Data Team");
        mapping.put("IT Support", "IT Support");
        return mapping.get(systeme);
    }

    /**
     * Assigne un membre en round-robin
     */
    private Membre assignerMembreRoundRobin(List<Membre> membres, int taskIndex) {
        if (membres == null || membres.isEmpty()) {
            return null;
        }
        int memberIndex = taskIndex % membres.size();
        return membres.get(memberIndex);
    }

    /**
     * Génère les descriptions des tâches
     */
    private List<String> getTachesDescriptions(String systeme, String description) {
        List<String> taches = new ArrayList<>();
        String sys = systeme != null ? systeme.toLowerCase() : "";

        switch (sys) {
            case "crm":
                taches.addAll(Arrays.asList(
                        "Analyser les logs du système CRM",
                        "Vérifier les intégrations CRM",
                        "Tester les workflows CRM",
                        "Valider la correction"
                ));
                break;
            case "sap":
                taches.addAll(Arrays.asList(
                        "Vérifier les logs SAP",
                        "Contrôler les interfaces IDOC",
                        "Valider la configuration SAP",
                        "Tester les flux corrigés"
                ));
                break;
            case "billing":
                taches.addAll(Arrays.asList(
                        "Analyser les transactions en échec",
                        "Vérifier les règles de calcul",
                        "Corriger les transactions bloquées",
                        "Valider avec l'équipe finance"
                ));
                break;
            case "authentication":
                taches.addAll(Arrays.asList(
                        "Vérifier les services d'authentification",
                        "Contrôler les certificats SSL",
                        "Analyser les logs d'accès",
                        "Tester la connexion"
                ));
                break;
            default:
                taches.addAll(Arrays.asList(
                        "Analyser le problème",
                        "Diagnostiquer la cause",
                        "Appliquer la correction",
                        "Tester la résolution"
                ));
                break;
        }
        return taches;
    }

    /**
     * Obtient la répartition des tâches
     */
    public Map<String, Object> getRepartitionTaches(List<Tache> taches) {
        Map<String, Object> repartition = new HashMap<>();
        Map<String, Integer> parMembre = new HashMap<>();

        for (Tache tache : taches) {
            if (tache.getAssigneA() != null) {
                Membre membre = tache.getAssigneA();
                String nomMembre = membre.getPrenom() + " " + membre.getNom();
                parMembre.put(nomMembre, parMembre.getOrDefault(nomMembre, 0) + 1);
            }
        }

        repartition.put("parMembre", parMembre);
        repartition.put("totalTaches", taches.size());
        return repartition;
    }

    /**
     * Analyse un prompt texte
     */
    public String analyze(String prompt) {
        String aiServiceUrl = "http://localhost:8000/api/v1/analyze-text";

        try {
            Map<String, String> request = new HashMap<>();
            request.put("prompt", prompt);

            var response = restTemplate.postForEntity(aiServiceUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().getOrDefault("result", "Aucune solution trouvée");
            }
        } catch (Exception e) {
            log.error("Erreur appel IA: {}", e.getMessage());
        }

        return "Service IA indisponible";
    }
}