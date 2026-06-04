package com.iset.gestion_projet.DTOS;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String role;
    private Long   id;      // ← AJOUTÉ
    private String nom;     // ← AJOUTÉ
    private String prenom;  // ← AJOUTÉ
}