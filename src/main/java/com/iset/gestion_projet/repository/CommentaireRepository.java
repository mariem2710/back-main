package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    // ✅ AJOUTER CETTE MÉTHODE
    List<Commentaire> findByTicketId(Long ticketId);
}