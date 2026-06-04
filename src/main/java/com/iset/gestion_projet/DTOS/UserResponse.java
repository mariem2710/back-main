// UserResponse.java
package com.iset.gestion_projet.DTOS;

import com.iset.gestion_projet.entity.Role;
import com.iset.gestion_projet.entity.StatutCompte;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private StatutCompte statut;
    private Long equipeId;
    private String equipeNom;
}