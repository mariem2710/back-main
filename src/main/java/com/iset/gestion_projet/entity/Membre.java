package com.iset.gestion_projet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "membre")
@Data @Builder
@NoArgsConstructor @AllArgsConstructor
public class Membre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private String email;
    private String role; // ex: "Développeur", "Analyste", "DevOps"
    // Dans Membre.java
    // ✅ Correction: Utiliser @Enumerated(EnumType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutCompte statut; // Valeurs possibles: "ACCEPTE", "EN_ATTENTE", "REJETE"

    @ManyToOne
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;
}