package com.iset.gestion_projet.DTOS;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SousTicketResponse {

    // ──────────────────────────────────────────────────────────────
    // CHAMPS PRINCIPAUX
    // ──────────────────────────────────────────────────────────────
    private Long id;
    private String titre;
    private String description;
    private String systemeImpacte;
    private String priorite;
    private String statut;
    private String generePar;
    private LocalDateTime dateCreation;

    // ──────────────────────────────────────────────────────────────
    // RELATIONS
    // ──────────────────────────────────────────────────────────────
    private Long ticketId;           // ✅ AJOUTÉ : ID du ticket parent
    private String ticketTitre;      // ✅ AJOUTÉ : Titre du ticket parent (optionnel)
    private Long equipeId;
    private String equipeNom;

    // ──────────────────────────────────────────────────────────────
    // STATISTIQUES
    // ──────────────────────────────────────────────────────────────
    @Builder.Default
    private Double progression = 0.0;

    @Builder.Default
    private Integer nombreTaches = 0;

    @Builder.Default
    private Integer tachesTerminees = 0;

    // ──────────────────────────────────────────────────────────────
    // COLLECTIONS
    // ──────────────────────────────────────────────────────────────
    @Builder.Default
    private List<TacheResponse> taches = new ArrayList<>();

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
                ? description : "Aucune description";
    }

    /**
     * Retourne le système impacté ou une valeur par défaut
     */
    public String getSystemeImpacteOrDefault() {
        return systemeImpacte != null && !systemeImpacte.trim().isEmpty()
                ? systemeImpacte : "Non spécifié";
    }

    /**
     * Retourne la priorité formatée
     */
    public String getPrioriteFormatted() {
        if (priorite == null) return "MOYENNE";
        return priorite.substring(0, 1).toUpperCase() + priorite.substring(1).toLowerCase();
    }

    /**
     * Retourne la couleur CSS pour la priorité
     */
    public String getPrioriteColor() {
        if (priorite == null) return "gray";
        switch (priorite.toUpperCase()) {
            case "BASSE": return "green";
            case "MOYENNE": return "orange";
            case "HAUTE": return "red";
            case "CRITIQUE": return "darkred";
            default: return "gray";
        }
    }

    /**
     * Retourne le statut formaté
     */
    public String getStatutFormatted() {
        if (statut == null) return "A_FAIRE";
        return statut.replace("_", " ");
    }

    /**
     * Retourne la couleur CSS pour le statut
     */
    public String getStatutColor() {
        if (statut == null) return "gray";
        switch (statut.toUpperCase()) {
            case "A_FAIRE": return "orange";
            case "EN_COURS": return "blue";
            case "FAIT": return "green";
            case "TERMINE": return "green";
            case "BLOQUE": return "red";
            case "APPROUVE": return "green";
            case "REJETE": return "red";
            default: return "gray";
        }
    }

    /**
     * Retourne l'icône CSS pour le statut
     */
    public String getStatutIcon() {
        if (statut == null) return "help_outline";
        switch (statut.toUpperCase()) {
            case "A_FAIRE": return "pending";
            case "EN_COURS": return "sync";
            case "FAIT": return "check_circle";
            case "TERMINE": return "done_all";
            case "BLOQUE": return "block";
            case "APPROUVE": return "thumb_up";
            case "REJETE": return "thumb_down";
            default: return "help_outline";
        }
    }

    /**
     * Retourne la date de création formatée
     */
    public String getDateCreationFormatted() {
        if (dateCreation == null) return "Non définie";
        return dateCreation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Retourne la date de création au format court
     */
    public String getDateCreationShort() {
        if (dateCreation == null) return "N/A";
        return dateCreation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Retourne la progression formatée en pourcentage
     */
    public String getProgressionFormatted() {
        if (progression == null) return "0%";
        return String.format("%.1f%%", progression);
    }

    /**
     * Calcule et met à jour la progression basée sur les tâches
     */
    public void updateProgressionFromTaches() {
        if (taches == null || taches.isEmpty()) {
            this.progression = 0.0;
            this.nombreTaches = 0;
            this.tachesTerminees = 0;
            return;
        }

        this.nombreTaches = taches.size();
        this.tachesTerminees = (int) taches.stream()
                .filter(t -> t.getStatut() != null &&
                        (t.getStatut().name().equals("FAIT") ||
                                t.getStatut().name().equals("TERMINE")))
                .count();

        this.progression = (nombreTaches > 0)
                ? (tachesTerminees * 100.0) / nombreTaches
                : 0.0;
    }

    /**
     * Vérifie si le sous-ticket a des tâches
     */
    public boolean hasTaches() {
        return taches != null && !taches.isEmpty();
    }

    /**
     * Vérifie si le sous-ticket est terminé
     */
    public boolean isTermine() {
        return statut != null && (statut.equalsIgnoreCase("FAIT") ||
                statut.equalsIgnoreCase("TERMINE"));
    }

    /**
     * Vérifie si le sous-ticket est en cours
     */
    public boolean isEnCours() {
        return statut != null && statut.equalsIgnoreCase("EN_COURS");
    }

    /**
     * Vérifie si le sous-ticket est bloqué
     */
    public boolean isBloque() {
        return statut != null && statut.equalsIgnoreCase("BLOQUE");
    }

    /**
     * Retourne les tâches non terminées
     */
    public List<TacheResponse> getTachesNonTerminees() {
        if (taches == null || taches.isEmpty()) {
            return Collections.emptyList();
        }
        return taches.stream()
                .filter(t -> t.getStatut() != null &&
                        !t.getStatut().name().equals("FAIT") &&
                        !t.getStatut().name().equals("TERMINE"))
                .collect(Collectors.toList());
    }

    /**
     * Retourne les tâches terminées
     */
    public List<TacheResponse> getTachesTerminees() {
        if (taches == null || taches.isEmpty()) {
            return Collections.emptyList();
        }
        return taches.stream()
                .filter(t -> t.getStatut() != null &&
                        (t.getStatut().name().equals("FAIT") ||
                                t.getStatut().name().equals("TERMINE")))
                .collect(Collectors.toList());
    }

    /**
     * Retourne le résumé pour affichage
     */
    public String getSummary() {
        return String.format("[%s] %s - %s (%d/%d tâches)",
                systemeImpacte != null ? systemeImpacte : "Système",
                getTitreOrDefault(),
                getStatutFormatted(),
                tachesTerminees != null ? tachesTerminees : 0,
                nombreTaches != null ? nombreTaches : 0);
    }

    /**
     * Crée une copie immuable du DTO
     */
    public SousTicketResponse toImmutable() {
        return SousTicketResponse.builder()
                .id(this.id)
                .titre(this.titre)
                .description(this.description)
                .systemeImpacte(this.systemeImpacte)
                .priorite(this.priorite)
                .statut(this.statut)
                .generePar(this.generePar)
                .dateCreation(this.dateCreation)
                .ticketId(this.ticketId)
                .ticketTitre(this.ticketTitre)
                .equipeId(this.equipeId)
                .equipeNom(this.equipeNom)
                .progression(this.progression)
                .nombreTaches(this.nombreTaches)
                .tachesTerminees(this.tachesTerminees)
                .taches(this.taches != null
                        ? Collections.unmodifiableList(new ArrayList<>(this.taches))
                        : Collections.emptyList())
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // OVERRIDES
    // ──────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SousTicketResponse that = (SousTicketResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("SousTicketResponse{id=%d, titre='%s', systeme='%s', statut=%s, progression=%.1f%%}",
                id, getTitreOrDefault(), getSystemeImpacteOrDefault(), statut, progression != null ? progression : 0.0);
    }
}