package com.iset.gestion_projet.DTOS;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


// ── Requête envoyée au microservice IA ────────────────────────────────────────
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AIAnalyzeRequest {

    private String title;           // = ticket.getTitre()
    private String description;     // = ticket.getDescription()

    @JsonProperty("ticket_id")
    private String ticketId;        // = ticket.getId().toString()

    private String priority;        // = ticket.getPriorite().name()
}