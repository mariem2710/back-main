package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.Equipe;
import com.iset.gestion_projet.entity.Role;
import com.iset.gestion_projet.entity.StatutCompte;
import com.iset.gestion_projet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByStatut(StatutCompte statut);

    List<User> findByRole(Role role);

    List<User> findByEquipeNom(String nomEquipe);

    List<User> findByEquipe(Equipe equipe);

    // ✅ Techniciens actifs (TECHNIQUE ou TECHNICIEN) + ACCEPTE + équipe assignée
    @Query("""
        SELECT u FROM User u
        WHERE u.role IN :roles
          AND u.statut = :statut
          AND u.equipe IS NOT NULL
        ORDER BY u.nom, u.prenom
    """)
    List<User> findTechniciens(
            @Param("roles")  List<Role> roles,
            @Param("statut") StatutCompte statut
    );

    // ✅ Techniciens d'une équipe précise
    @Query("""
        SELECT u FROM User u
        WHERE u.role IN :roles
          AND u.statut = :statut
          AND u.equipe IS NOT NULL
          AND LOWER(u.equipe.nom) = LOWER(:nomEquipe)
        ORDER BY u.nom, u.prenom
    """)
    List<User> findTechniciensByEquipe(
            @Param("roles")     List<Role> roles,
            @Param("statut")    StatutCompte statut,
            @Param("nomEquipe") String nomEquipe
    );

    // ✅ Techniciens d'une équipe par objet Equipe
    @Query("""
        SELECT u FROM User u
        WHERE u.role IN :roles
          AND u.statut = :statut
          AND u.equipe = :equipe
        ORDER BY u.nom, u.prenom
    """)
    List<User> findTechniciensByEquipeObj(
            @Param("roles")  List<Role> roles,
            @Param("statut") StatutCompte statut,
            @Param("equipe") Equipe equipe
    );
}