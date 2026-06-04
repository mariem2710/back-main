package com.iset.gestion_projet.DTOS;

import com.iset.gestion_projet.entity.Priorite;
import com.iset.gestion_projet.entity.Statut;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    // ──────────────────────────────────────────────────────────────
    // CHAMPS PRINCIPAUX
    // ──────────────────────────────────────────────────────────────
    private Long id;
    private String titre;
    private String description;
    private Statut statut;
    private Priorite priorite;
    private LocalDate dateCreation;
    private LocalDate dateSouhaite;
    private LocalDate dateMiseAJour;

    // ──────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ──────────────────────────────────────────────────────────────
    @Builder.Default
    private Integer nombreCommentaires = 0;

    @Builder.Default
    private Integer nombreSousTickets = 0;

    @Builder.Default
    private Double progression = 0.0;

    // ──────────────────────────────────────────────────────────────
    // ANALYSE IA
    // ──────────────────────────────────────────────────────────────
    @Builder.Default
    private Boolean analyseIAEffectuee = false;

    private String aiSummary;
    private String causeRacine;

    // ──────────────────────────────────────────────────────────────
    // COLLECTIONS
    // ──────────────────────────────────────────────────────────────
    @Builder.Default
    private List<String> systemesDetectes = new ArrayList<>();

    @Builder.Default
    private List<SousTicketResponse> sousTickets = new ArrayList<>();

    // ──────────────────────────────────────────────────────────────
    // MÉTADONNÉES
    // ──────────────────────────────────────────────────────────────
    private String createdBy;
    private Long createdById;

    // ──────────────────────────────────────────────────────────────
    // MÉTHODES UTILITAIRES
    // ──────────────────────────────────────────────────────────────

    /**
     * Retourne le titre ou une valeur par défaut si null
     */
    public String getTitreOrDefault() {
        return titre != null && !titre.trim().isEmpty() ? titre : "Sans titre";
    }

    /**
     * Retourne la description ou une valeur par défaut si null
     */
    public String getDescriptionOrDefault() {
        return description != null && !description.trim().isEmpty()
                ? description : "Aucune description fournie";
    }

    /**
     * Retourne le résumé IA ou une valeur par défaut
     */
    public String getAiSummaryOrDefault() {
        return aiSummary != null && !aiSummary.trim().isEmpty()
                ? aiSummary : "Analyse non disponible";
    }

    /**
     * Retourne la cause racine ou une valeur par défaut
     */
    public String getCauseRacineOrDefault() {
        return causeRacine != null && !causeRacine.trim().isEmpty()
                ? causeRacine : "Non déterminée";
    }

    /**
     * Vérifie si le ticket a des sous-tickets
     */
    public boolean hasSousTickets() {
        return sousTickets != null && !sousTickets.isEmpty();
    }

    /**
     * Vérifie si l'analyse IA a été effectuée et a des résultats
     */
    public boolean hasAIAnalysis() {
        return Boolean.TRUE.equals(analyseIAEffectuee) &&
                aiSummary != null && !aiSummary.trim().isEmpty();
    }

    /**
     * Retourne le nombre total de tâches (somme des tâches de tous les sous-tickets)
     */
    public int getTotalTaches() {
        if (sousTickets == null || sousTickets.isEmpty()) {
            return 0;
        }
        return sousTickets.stream()
                .mapToInt(st -> st.getTaches() != null ? st.getTaches().size() : 0)
                .sum();
    }

    /**
     * Retourne le nombre de tâches terminées
     */
    public int getTachesTerminees() {
        if (sousTickets == null || sousTickets.isEmpty()) {
            return 0;
        }
        return (int) sousTickets.stream()
                .flatMap(st -> st.getTaches().stream())
                .filter(t -> t.getStatut() != null && t.getStatut().name().equals("FAIT"))
                .count();
    }

    /**
     * Retourne la progression formatée en pourcentage
     */
    public String getProgressionFormatted() {
        if (progression == null) return "0%";
        return String.format("%.1f%%", progression);
    }

    /**
     * Retourne la date de création formatée
     */
    public String getDateCreationFormatted() {
        if (dateCreation == null) return "Non définie";
        return dateCreation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Retourne la date souhaitée formatée
     */
    public String getDateSouhaiteFormatted() {
        if (dateSouhaite == null) return "Non définie";
        return dateSouhaite.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Retourne la date de mise à jour formatée
     */
    public String getDateMiseAJourFormatted() {
        if (dateMiseAJour == null) return "Non définie";
        return dateMiseAJour.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Vérifie si le ticket est en retard
     */
    public boolean isEnRetard() {
        if (dateSouhaite == null || statut == null) return false;
        if (statut == Statut.TERMINE || statut == Statut.Fait) return false;
        return LocalDate.now().isAfter(dateSouhaite);
    }

    /**
     * Retourne la couleur CSS pour le statut
     */
    public String getStatutColor() {
        if (statut == null) return "gray";
        switch (statut) {
            case A_FAIRE: return "orange";
            case En_cours: return "blue";
            case APPROUVE: return "green";
            case REJETE: return "red";
            case TERMINE: return "green";
            case Fait: return "green";
            default: return "gray";
        }
    }

    /**
     * Retourne la couleur CSS pour la priorité
     */
    public String getPrioriteColor() {
        if (priorite == null) return "gray";
        switch (priorite) {
            case BASSE: return "green";
            case MOYENNE: return "orange";
            case HAUTE: return "red";
            case CRITIQUE: return "darkred";
            default: return "gray";
        }
    }

    /**
     * Retourne l'icône CSS pour le statut
     */
    public String getStatutIcon() {
        if (statut == null) return "help_outline";
        switch (statut) {
            case A_FAIRE: return "pending";
            case En_cours: return "sync";
            case APPROUVE: return "check_circle";
            case REJETE: return "cancel";
            case TERMINE: return "done_all";
            case Fait: return "done";
            default: return "help_outline";
        }
    }

    /**
     * Retourne les systèmes détectés sous forme de chaîne
     */
    public String getSystemesDetectesAsString() {
        if (systemesDetectes == null || systemesDetectes.isEmpty()) {
            return "Aucun";
        }
        return String.join(", ", systemesDetectes);
    }

    /**
     * Crée une copie du DTO avec des listes immutables
     */
    public TicketResponse toImmutable() {
        return TicketResponse.builder()
                .id(this.id)
                .titre(this.titre)
                .description(this.description)
                .statut(this.statut)
                .priorite(this.priorite)
                .dateCreation(this.dateCreation)
                .dateSouhaite(this.dateSouhaite)
                .dateMiseAJour(this.dateMiseAJour)
                .nombreCommentaires(this.nombreCommentaires)
                .nombreSousTickets(this.nombreSousTickets)
                .progression(this.progression)
                .analyseIAEffectuee(this.analyseIAEffectuee)
                .aiSummary(this.aiSummary)
                .causeRacine(this.causeRacine)
                .systemesDetectes(this.systemesDetectes != null
                        ? Collections.unmodifiableList(new ArrayList<>(this.systemesDetectes))
                        : Collections.emptyList())
                .sousTickets(this.sousTickets != null
                        ? Collections.unmodifiableList(new ArrayList<>(this.sousTickets))
                        : Collections.emptyList())
                .createdBy(this.createdBy)
                .createdById(this.createdById)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // OVERRIDES
    // ──────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketResponse that = (TicketResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("TicketResponse{id=%d, titre='%s', statut=%s, priorite=%s, progression=%.1f%%}",
                id, getTitreOrDefault(), statut, priorite, progression != null ? progression : 0.0);
    }
}