package com.iset.gestion_projet.DTOS;

import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentaireResponse {
    private Long id;
    private String commentaire;
    private Long ticketId;
    private Long userId;       // ✅ ajouter
    private String nomAuteur;  // ✅ ajouter (nom + prénom)
}