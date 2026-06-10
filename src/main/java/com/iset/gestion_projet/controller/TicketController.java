package com.iset.gestion_projet.controller;

import com.iset.gestion_projet.DTOS.TicketRequest;
import com.iset.gestion_projet.DTOS.TicketResponse;
import com.iset.gestion_projet.entity.Statut;
import com.iset.gestion_projet.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(
                    ticketService.getAllTickets()
            );
        } catch (Exception e) {
            log.error("GET /api/tickets → 500: {}",
                    e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",   "Erreur serveur",
                            "message", e.getMessage()
                    ));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody TicketRequest request) {
        try {
            return ResponseEntity.ok(
                    ticketService.createTicket(request)
            );
        } catch (Exception e) {
            log.error("POST /api/tickets → 500: {}",
                    e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    ticketService.getById(id)
            );
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody TicketRequest request) {
        try {
            return ResponseEntity.ok(
                    ticketService.updateTicket(id, request)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/todo")
    public ResponseEntity<List<TicketResponse>> getTodo() {
        return ResponseEntity.ok(
                ticketService.getByStatut(Statut.A_FAIRE)
        );
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    ticketService.approveTicket(id)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    ticketService.rejectTicket(id)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<?> analyser(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    ticketService.analyserTicket(id)
            );
        } catch (Exception e) {
            log.error("Analyse IA ticket #{}: {}",
                    id, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Dans TicketController.java
    @GetMapping("/{id}/progression")
    public ResponseEntity<Map<String, Object>> getTicketProgression(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketProgression(id));
    }
}