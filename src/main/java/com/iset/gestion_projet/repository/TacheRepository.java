package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TacheRepository extends JpaRepository<Tache, Long> {

    // ── Par sous-ticket ───────────────────────────────────────────
    List<Tache> findBySousTicketId(Long sousTicketId);

    // ── Par assignee (User uniquement maintenant) ─────────────────
    List<Tache> findByAssigneeId(Long userId);

    // ✅ Toutes les tâches d'un user — un seul champ assignee
    @Query("""
        SELECT t FROM Tache t
        WHERE t.assignee.id = :userId
    """)
    List<Tache> findAllTachesForUser(@Param("userId") Long userId);

    // ── Toutes les tâches d'un ticket (via sous-tickets) ──────────
    @Query("""
        SELECT t FROM Tache t
        WHERE t.sousTicket.ticket.id = :ticketId
    """)
    List<Tache> findByTicketId(@Param("ticketId") Long ticketId);

    long countBySousTicketId(Long sousTicketId);
}