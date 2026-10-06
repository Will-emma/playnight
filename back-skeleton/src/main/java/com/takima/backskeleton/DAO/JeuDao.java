package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.TypeJeu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JeuDao extends JpaRepository<Jeu, Long> {
    List<Jeu> findAllByOrderByNomAsc();

    List<Jeu> findByTypeOrderByNomAsc(TypeJeu type);
}
