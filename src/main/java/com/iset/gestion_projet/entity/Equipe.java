package com.iset.gestion_projet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "equipe")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nom;

    private String description;

    private String systemeAssocie;

    // ✅ Remplacé List<Membre> par List<User>
    @JsonIgnore
    @OneToMany(mappedBy = "equipe", fetch = FetchType.LAZY)
    private List<User> membres;
}