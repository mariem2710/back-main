package com.iset.gestion_projet.DTOS.profile;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String poste;
    private String avatarUrl;
    private LocalDate dateNaissance;
    private String role;
    private String equipe;
}