package com.iset.gestion_projet.DTOS;

import lombok.*;
import java.util.List;

@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class EquipeDTO {
    private Long id;
    private String nom;
    private String description;
    private String systemeAssocie;
    private int nombreMembres;
    private List<MembreDTO> membres;
}