package com.iset.gestion_projet.DTOS;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentaireRequest {
    private String commentaire;
    private Long userId; // ✅ ajouter
}
