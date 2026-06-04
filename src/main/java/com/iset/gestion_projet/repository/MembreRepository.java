package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.Equipe;
import com.iset.gestion_projet.entity.Membre;
import com.iset.gestion_projet.entity.StatutCompte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {

    List<Membre> findByEquipe(Equipe equipe);

    List<Membre> findByEquipeId(Long equipeId);

    Optional<Membre> findByEmail(String email);

    List<Membre> findByEquipeNom(String equipeNom);

    // ✅ Version corrigée - utilise StatutCompte au lieu de Integer
    List<Membre> findByStatut(StatutCompte statut);

    List<Membre> findByEquipeAndStatut(Equipe equipe, StatutCompte statut);

    List<Membre> findByEquipeIdAndStatut(Long equipeId, StatutCompte statut);

    @Query("SELECT m FROM Membre m WHERE m.equipe.systemeAssocie = :systeme AND m.statut = :statut")
    List<Membre> findBySystemeAssocieAndStatut(@Param("systeme") String systeme, @Param("statut") StatutCompte statut);
}