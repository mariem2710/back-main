package com.iset.gestion_projet.controller;

import com.iset.gestion_projet.DTOS.SousTicketResponse;
import com.iset.gestion_projet.entity.SousTicket;
import com.iset.gestion_projet.entity.Statut;
import com.iset.gestion_projet.service.SousTicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sous-tickets")
@RequiredArgsConstructor
public class SousTicketController {

    private static final Logger logger = LoggerFactory.getLogger(SousTicketController.class);
    private final SousTicketService sousTicketService;

    @PostMapping
    public ResponseEntity<SousTicket> create(@RequestBody SousTicket sousTicket) {
        try {
            SousTicket created = sousTicketService.createSousTicket(sousTicket);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.error("Erreur création: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<SousTicket>> getAll() {
        return ResponseEntity.ok(sousTicketService.getAll());
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<SousTicketResponse>> getByTicket(@PathVariable Long ticketId) {
        try {
            List<SousTicketResponse> response = sousTicketService.getByTicketId(ticketId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur récupération: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SousTicketResponse> getById(@PathVariable Long id) {
        try {
            SousTicketResponse response = sousTicketService.getById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur récupération: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<SousTicketResponse> changerStatut(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String statutValue = body.get("statut");
            if (statutValue == null) {
                return ResponseEntity.badRequest().build();
            }
            Statut statut = Statut.valueOf(statutValue.toUpperCase());
            SousTicketResponse response = sousTicketService.changerStatut(id, statut);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ticket/{ticketId}/progression")
    public ResponseEntity<Map<String, Object>> getProgression(@PathVariable Long ticketId) {
        try {
            Map<String, Object> progression = sousTicketService.calculerProgression(ticketId);
            return ResponseEntity.ok(progression);
        } catch (Exception e) {
            logger.error("Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}