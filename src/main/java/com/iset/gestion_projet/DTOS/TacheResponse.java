package com.iset.gestion_projet.DTOS;

import com.iset.gestion_projet.entity.Priorite;
import com.iset.gestion_projet.entity.Statut;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TacheResponse {
    private Long id;
    private String titre;
    private String description;
    private Statut statut;
    private Priorite priorite;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLimite;

    // Assignation
    private Long assigneeId;
    private String assigneeNom;
    private String assigneePrenom;

    // Relations
    private Long sousTicketId;
    private String sousTicketTitre;
    private Long ticketId;
}