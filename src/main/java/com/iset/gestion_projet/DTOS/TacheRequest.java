package com.iset.gestion_projet.DTOS;

import com.iset.gestion_projet.entity.Priorite;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TacheRequest {
    private String titre;
    private String description;
    private Priorite priorite;
    private LocalDate dateLimite;
    private Long sousTicketId;
}