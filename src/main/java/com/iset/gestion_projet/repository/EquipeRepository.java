package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface EquipeRepository extends JpaRepository<Equipe, Long> {
    Optional<Equipe> findByNom(String nom);
    List<Equipe> findByNomContainingIgnoreCase(String keyword);
    List<Equipe> findBySystemeAssocieIgnoreCase(String systeme);


    @Query("SELECT e FROM Equipe e WHERE LOWER(e.systemeAssocie) = LOWER(:systeme)")
    Optional<Equipe> findBySystemeAssocie(@Param("systeme") String systeme);

    @Query("SELECT e FROM Equipe e LEFT JOIN FETCH e.membres WHERE e.id = :id")
    Optional<Equipe> findByIdWithMembres(@Param("id") Long id);
}