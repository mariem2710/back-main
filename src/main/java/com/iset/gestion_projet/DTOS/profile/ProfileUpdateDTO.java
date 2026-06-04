package com.iset.gestion_projet.DTOS.profile;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileUpdateDTO {
    private String nom;
    private String prenom;
    private String telephone;
    private String poste;
    private LocalDate dateNaissance;
}