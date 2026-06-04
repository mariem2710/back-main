package com.iset.gestion_projet.controller;

import com.iset.gestion_projet.DTOS.CommentaireRequest;
import com.iset.gestion_projet.DTOS.CommentaireResponse;
import com.iset.gestion_projet.service.CommentaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CommentaireController {

    private final CommentaireService commentaireService;

    // ✅ Add a comment to a ticket + retourne la liste à jour
    @PostMapping("/{ticketId}")
    public ResponseEntity<List<CommentaireResponse>> addComment(
            @PathVariable Long ticketId,
            @RequestBody CommentaireRequest request) {
        commentaireService.addComment(ticketId, request);
        return ResponseEntity.ok(commentaireService.getByTicket(ticketId));
    }

    // ✅ Get all comments
    @GetMapping
    public ResponseEntity<List<CommentaireResponse>> getAll() {
        return ResponseEntity.ok(commentaireService.getAll());
    }

    // ✅ Get comments by ticket
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<CommentaireResponse>> getByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentaireService.getByTicket(ticketId));
    }

    // ✅ Delete a comment
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentaireService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Update a comment
    @PutMapping("/{id}")
    public ResponseEntity<CommentaireResponse> update(
            @PathVariable Long id,
            @RequestBody CommentaireRequest request) {
        return ResponseEntity.ok(commentaireService.update(id, request));
    }
}