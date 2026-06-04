package com.iset.gestion_projet.DTOS;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SousTicketRequest {

    private String titre;
    private String description;
    private Long ticketId; // référence au ticket parent
}