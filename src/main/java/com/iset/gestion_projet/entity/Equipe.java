package com.iset.gestion_projet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "equipe")
@Data @Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nom; // ex: "SAP", "CRM", "Billing"

    private String description;

    private String systemeAssocie; // ex: "SAP", "CRM" — pour matching IA

    @OneToMany(mappedBy = "equipe", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Membre> membres;
}