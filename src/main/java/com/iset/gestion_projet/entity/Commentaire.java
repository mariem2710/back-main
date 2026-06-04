package com.iset.gestion_projet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Commentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String commentaire;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "user_id")  // ✅ ajouter
    private User user;
}