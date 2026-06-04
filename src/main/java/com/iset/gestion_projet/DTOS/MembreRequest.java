package com.iset.gestion_projet.DTOS;

import lombok.*;

@Data
@NoArgsConstructor @AllArgsConstructor
public class MembreRequest {
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private Long equipeId;
}