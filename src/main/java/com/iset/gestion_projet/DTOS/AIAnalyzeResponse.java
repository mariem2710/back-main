package com.iset.gestion_projet.DTOS;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

// ── Réponse complète du microservice IA ───────────────────────────────────────
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)  // ignore les champs inconnus
public class AIAnalyzeResponse {

    private boolean success;

    @JsonProperty("ticket_id")
    private String ticketId;

    private String summary;                     // résumé IA du ticket

    @JsonProperty("systems_detected")
    private List<String> systemsDetected;       // ["SAP", "Mobile App", "Billing"]

    @JsonProperty("root_cause")
    private String rootCause;

    @JsonProperty("technical_tickets")
    private List<AITechnicalTicket> technicalTickets;   // sous-tickets à créer

    @JsonProperty("ai_model")
    private String aiModel;

    @JsonProperty("processing_time_ms")
    private Double processingTimeMs;

    private String error;
    private int nombreSousTickets;
}