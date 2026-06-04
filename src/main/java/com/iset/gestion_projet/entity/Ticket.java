package com.iset.gestion_projet.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "ticket")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private Statut statut;

    @Enumerated(EnumType.STRING)
    private Priorite priorite;

    private LocalDate dateCreation;
    private LocalDate dateSouhaite;
    private LocalDate dateMiseAJour;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(columnDefinition = "TEXT")
    private String causeRacine;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ticket_systemes",
            joinColumns = @JoinColumn(name = "ticket_id")
    )
    @Column(name = "systeme")
    private List<String> systemesDetectes;

    @Builder.Default
    private boolean analyseIAEffectuee = false;

    // ✅ JsonIgnore pour éviter récursion infinie
    @JsonIgnore
    @OneToMany(
            mappedBy = "ticket",
            cascade  = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Commentaire> commentaires;

    // ✅ JsonIgnoreProperties pour couper la boucle
    @OneToMany(
            mappedBy      = "ticket",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.EAGER
    )
    @JsonIgnoreProperties("ticket")
    private List<SousTicket> sousTickets;

    private Long   createdById;
    private String createdBy;

    @PrePersist
    public void prePersist() {
        this.dateCreation  = LocalDate.now();
        this.dateMiseAJour = LocalDate.now();
        if (this.statut == null) this.statut = Statut.A_FAIRE;
    }

    @PreUpdate
    public void preUpdate() {
        this.dateMiseAJour = LocalDate.now();
    }
}