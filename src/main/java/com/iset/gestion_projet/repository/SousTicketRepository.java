package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.SousTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SousTicketRepository extends JpaRepository<SousTicket, Long> {

    // Méthode simple
    List<SousTicket> findByTicketId(Long ticketId);

    // Avec fetch explicite pour éviter LazyInitializationException
    @Query("SELECT DISTINCT st FROM SousTicket st " +
            "LEFT JOIN FETCH st.equipe " +
            "WHERE st.ticket.id = :ticketId")
    List<SousTicket> findByTicketIdWithEquipe(@Param("ticketId") Long ticketId);
}