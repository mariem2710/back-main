package com.iset.gestion_projet.DTOS;

import com.iset.gestion_projet.entity.Priorite;
import com.iset.gestion_projet.entity.Statut;
import lombok.*;

import java.time.LocalDate;@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRequest {

    private Long id;

    private String titre;
    private String description;

    private Statut statut;
    private Priorite priorite;

    private LocalDate dateSouhaite;

    // champs UI
    private Long createdById;
    private String createdBy;
}