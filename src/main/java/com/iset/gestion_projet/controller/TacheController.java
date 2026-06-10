package com.iset.gestion_projet.controller;

import com.iset.gestion_projet.DTOS.TacheResponse;
import com.iset.gestion_projet.service.TacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/taches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TacheController {

    private final TacheService tacheService;

    @PostMapping("/generer/{sousTicketId}")
    public ResponseEntity<List<TacheResponse>> generer(@PathVariable Long sousTicketId) {
        log.info("📝 Génération des tâches pour le sous-ticket #{}", sousTicketId);
        return ResponseEntity.ok(tacheService.genererTachesIA(sousTicketId));
    }

    @PutMapping("/{tacheId}/prendre/{employeId}")
    public ResponseEntity<TacheResponse> prendre(@PathVariable Long tacheId, @PathVariable Long employeId) {
        log.info("👤 Prise en charge de la tâche #{} par l'employé #{}", tacheId, employeId);
        return ResponseEntity.ok(tacheService.prendreEnCharge(tacheId, employeId));
    }

    @PutMapping("/{tacheId}/terminer/{employeId}")
    public ResponseEntity<?> terminer(@PathVariable Long tacheId, @PathVariable Long employeId) {
        log.info("🔧 Tentative de terminaison - Tâche #{} par employé #{}", tacheId, employeId);
        try {
            TacheResponse response = tacheService.terminerTache(tacheId, employeId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tache", response,
                    "message", "Tâche terminée avec succès"
            ));
        } catch (RuntimeException e) {
            log.warn("❌ Erreur terminaison tâche: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/mes-taches/{employeId}")
    public ResponseEntity<List<TacheResponse>> mesTaches(@PathVariable Long employeId) {
        log.info("📋 Récupération des tâches pour l'employé #{}", employeId);
        return ResponseEntity.ok(tacheService.getMesTaches(employeId));
    }

    @GetMapping("/sous-ticket/{sousTicketId}")
    public ResponseEntity<List<TacheResponse>> bySousTicket(@PathVariable Long sousTicketId) {
        log.info("📋 Récupération des tâches pour le sous-ticket #{}", sousTicketId);
        return ResponseEntity.ok(tacheService.getBySousTicket(sousTicketId));
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<TacheResponse>> byTicket(@PathVariable Long ticketId) {
        log.info("📋 Récupération des tâches pour le ticket #{}", ticketId);
        return ResponseEntity.ok(tacheService.getByTicket(ticketId));
    }

    @PostMapping("/{tacheId}/probleme")
    public ResponseEntity<Map<String, String>> probleme(@PathVariable Long tacheId, @RequestParam String probleme) {
        log.info("🤖 Demande d'aide IA pour la tâche #{}", tacheId);
        try {
            String reponse = tacheService.envoyerProblemeIA(tacheId, probleme);
            return ResponseEntity.ok(Map.of(
                    "reponse", reponse,
                    "success", "true"
            ));
        } catch (RuntimeException e) {
            log.warn("❌ Erreur aide IA: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "success", "false"
            ));
        }
    }

    @GetMapping("/progression/ticket/{ticketId}")
    public ResponseEntity<Map<String, Object>> getProgressionTicket(@PathVariable Long ticketId) {
        log.info("📊 Calcul de la progression pour le ticket #{}", ticketId);
        try {
            Map<String, Object> progression = tacheService.calculerProgressionTicket(ticketId);
            return ResponseEntity.ok(progression);
        } catch (RuntimeException e) {
            log.error("❌ Erreur calcul progression: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }
}