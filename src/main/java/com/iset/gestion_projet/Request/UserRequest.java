package com.iset.gestion_projet.Request;

import com.iset.gestion_projet.entity.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    private String nom;
    private String prenom;
    private String email;
    private String password;
    private Role role;
    private Long equipeId; // ── ADDED: admin assigns user to equipe on creation ──
}