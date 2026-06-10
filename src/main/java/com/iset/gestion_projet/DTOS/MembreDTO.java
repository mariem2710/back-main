package com.iset.gestion_projet.DTOS;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembreDTO {

    private Long   id;
    private String nom;
    private String prenom;
    private String email;
    private String role;        // Nom de l'enum : "TECHNIQUE" ou "TECHNICIEN"
    private Long   equipeId;
    private String equipeNom;
}