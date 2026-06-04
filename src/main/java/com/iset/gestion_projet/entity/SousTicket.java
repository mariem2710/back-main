package com.iset.gestion_projet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "sous_tickets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SousTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String systemeImpacte;
    private String generePar;
    private LocalDate dateCreation;

    @Enumerated(EnumType.STRING)
    private Priorite priorite;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Statut statut = Statut.A_faire;

    // ✅ Couper boucle SousTicket → Ticket → SousTicket
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    @JsonIgnoreProperties({"sousTickets", "commentaires"})
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id", nullable = true)
    private Equipe equipe;

    // ✅ Couper boucle Tache → SousTicket → Tache
    @OneToMany(
            mappedBy      = "sousTicket",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.EAGER
    )
    @JsonIgnoreProperties("sousTicket")
    private List<Tache> taches;

    @PrePersist
    public void prePersist() {
        this.dateCreation = LocalDate.now();
        if (this.statut    == null) this.statut    = Statut.A_faire;
        if (this.generePar == null) this.generePar = "IA";
    }
}