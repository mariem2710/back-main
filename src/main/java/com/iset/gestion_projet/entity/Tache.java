package com.iset.gestion_projet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Statut statut = Statut.A_faire;

    @Enumerated(EnumType.STRING)
    private Priorite priorite;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_limite")
    private LocalDateTime dateLimite;

    // ✅ UN SEUL champ assignation — User filtré TECHNIQUE/TECHNICIEN
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    // ✅ Sous-ticket parent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sous_ticket_id")
    @JsonIgnore
    private SousTicket sousTicket;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        if (this.statut   == null) this.statut   = Statut.A_faire;
        if (this.priorite == null) this.priorite = Priorite.MOYENNE;
    }
}