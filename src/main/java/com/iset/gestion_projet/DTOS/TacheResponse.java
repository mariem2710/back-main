package com.iset.gestion_projet.DTOS;

import com.iset.gestion_projet.entity.Priorite;
import com.iset.gestion_projet.entity.Statut;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TacheResponse {

    private Long       id;
    private String     titre;
    private String     description;
    private Statut     statut;
    private Priorite   priorite;
    private LocalDate  dateCreation;
    private LocalDate  dateLimite;
    private Long       sousTicketId;
    private String     sousTicketTitre;
    private Long       ticketId;

    // ✅ Champs assignee — viennent de Membre (assigneA) ou User (assignee)
    private Long       assigneeId;
    private String     assigneeNom;     // ← doit être non null pour affichage
    private String     assigneePrenom;  // ← doit être non null pour affichage
}