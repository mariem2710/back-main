package com.iset.gestion_projet.service;

import com.iset.gestion_projet.DTOS.CommentaireRequest;
import com.iset.gestion_projet.DTOS.CommentaireResponse;
import com.iset.gestion_projet.entity.Commentaire;
import com.iset.gestion_projet.entity.Ticket;
import com.iset.gestion_projet.entity.User;
import com.iset.gestion_projet.repository.CommentaireRepository;
import com.iset.gestion_projet.repository.TicketRepository;
import com.iset.gestion_projet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentaireService {

    private final CommentaireRepository commentaireRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository; // ✅ ajouter

    // ✅ ADD COMMENT
    public CommentaireResponse addComment(Long ticketId, CommentaireRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        User user = userRepository.findById(request.getUserId()) // ✅ ajouter
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

        Commentaire commentaire = Commentaire.builder()
                .commentaire(request.getCommentaire())
                .ticket(ticket)
                .user(user) // ✅ ajouter
                .build();

        return mapToResponse(commentaireRepository.save(commentaire));
    }

    // ✅ GET ALL
    public List<CommentaireResponse> getAll() {
        return commentaireRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ✅ GET BY TICKET ID
    public List<CommentaireResponse> getByTicket(Long ticketId) {
        return commentaireRepository.findByTicketId(ticketId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ✅ DELETE
    public void delete(Long id) {
        if (!commentaireRepository.existsById(id)) {
            throw new RuntimeException("Commentaire not found: " + id);
        }
        commentaireRepository.deleteById(id);
    }

    // ✅ UPDATE
    public CommentaireResponse update(Long id, CommentaireRequest request) {
        Commentaire commentaire = commentaireRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commentaire not found: " + id));

        commentaire.setCommentaire(request.getCommentaire());

        return mapToResponse(commentaireRepository.save(commentaire));
    }

    // 🔁 MAPPING ENTITY → DTO
    private CommentaireResponse mapToResponse(Commentaire c) {
        return CommentaireResponse.builder()
                .id(c.getId())
                .commentaire(c.getCommentaire())
                .ticketId(c.getTicket().getId())
                .userId(c.getUser() != null ? c.getUser().getId() : null)                                          // ✅ null check
                .nomAuteur(c.getUser() != null ? c.getUser().getNom() + " " + c.getUser().getPrenom() : "Inconnu") // ✅ null check
                .build();
    }
}