package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.Statut;
import com.iset.gestion_projet.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatut(Statut statut);

    @Query("""
           SELECT COUNT(c)
           FROM Commentaire c
           WHERE c.ticket.id = :ticketId
           """)
    long countCommentairesByTicketId(@Param("ticketId") Long ticketId);
}