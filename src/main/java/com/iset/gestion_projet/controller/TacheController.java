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
        return ResponseEntity.ok(tacheService.genererTachesIA(sousTicketId));
    }

    @PutMapping("/{tacheId}/prendre/{employeId}")
    public ResponseEntity<TacheResponse> prendre(@PathVariable Long tacheId, @PathVariable Long employeId) {
        return ResponseEntity.ok(tacheService.prendreEnCharge(tacheId, employeId));
    }

    @PutMapping("/{tacheId}/terminer/{employeId}")
    public ResponseEntity<?> terminer(@PathVariable Long tacheId, @PathVariable Long employeId) {
        try {
            return ResponseEntity.ok(tacheService.terminerTache(tacheId, employeId));
        } catch (RuntimeException e) {
            log.warn("Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/mes-taches/{employeId}")
    public ResponseEntity<List<TacheResponse>> mesTaches(@PathVariable Long employeId) {
        return ResponseEntity.ok(tacheService.getMesTaches(employeId));
    }

    @GetMapping("/sous-ticket/{sousTicketId}")
    public ResponseEntity<List<TacheResponse>> bySousTicket(@PathVariable Long sousTicketId) {
        return ResponseEntity.ok(tacheService.getBySousTicket(sousTicketId));
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<TacheResponse>> byTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(tacheService.getByTicket(ticketId));
    }

    @PostMapping("/{tacheId}/probleme")
    public ResponseEntity<Map<String, String>> probleme(@PathVariable Long tacheId, @RequestParam String probleme) {
        try {
            String reponse = tacheService.envoyerProblemeIA(tacheId, probleme);
            return ResponseEntity.ok(Map.of("reponse", reponse));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/progression/ticket/{ticketId}")
    public ResponseEntity<Map<String, Object>> progression(@PathVariable Long ticketId) {
        return ResponseEntity.ok(tacheService.calculerProgressionTicket(ticketId));
    }
}