package com.iset.gestion_projet.DTOS;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AITechnicalTicket {

    private String system;
    private String title;
    private String description;
    private String priority;

    @JsonProperty("suggested_team")
    private String suggestedTeam;
}