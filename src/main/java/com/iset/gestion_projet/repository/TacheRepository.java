package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TacheRepository
        extends JpaRepository<Tache, Long> {

    // ── Par sous-ticket ───────────────────────────────
    List<Tache> findBySousTicketId(Long sousTicketId);

    // ── Par assignee direct (User) ────────────────────
    List<Tache> findByAssigneeId(Long userId);

    // ── Par membre IA (Membre) ────────────────────────
    List<Tache> findByAssigneAId(Long membreId);

    // ✅ Toutes les tâches d'un user
    // Cherche via User (assignee) OU Membre (assigneA) par email
    // LEFT JOIN pour éviter NullPointerException si l'un est null
    @Query("""
        SELECT DISTINCT t FROM Tache t
        LEFT JOIN t.assignee  u
        LEFT JOIN t.assigneA  m
        WHERE u.id = :userId
           OR m.email = (
               SELECT usr.email
               FROM User usr
               WHERE usr.id = :userId
           )
    """)
    List<Tache> findAllTachesForUser(
            @Param("userId") Long userId
    );

    // ── Toutes les tâches d'un ticket
    //    (via les sous-tickets) ──────────────────────
    @Query("""
        SELECT t FROM Tache t
        WHERE t.sousTicket.ticket.id = :ticketId
    """)
    List<Tache> findByTicketId(
            @Param("ticketId") Long ticketId
    );


    long countBySousTicketIdAndStatut(Long sousTicketId, String statut);

}