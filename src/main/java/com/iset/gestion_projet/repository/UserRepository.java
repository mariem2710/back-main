package com.iset.gestion_projet.repository;

import com.iset.gestion_projet.entity.Role;
import com.iset.gestion_projet.entity.StatutCompte;
import com.iset.gestion_projet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.iset.gestion_projet.entity.Equipe;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByStatut(StatutCompte statut);
    List<User> findByRole(Role role);

    // ✅ Ajout : pour l'assignation automatique IA → employeurs d'une équipe
    List<User> findByEquipeNom(String nomEquipe);
    List<User> findByEquipe(Equipe equipe);

}