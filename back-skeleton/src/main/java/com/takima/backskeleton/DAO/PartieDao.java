package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Partie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartieDao extends JpaRepository<Partie, Long> {
    @Query("""
            SELECT DISTINCT p
            FROM Partie p
            LEFT JOIN FETCH p.jeu
            LEFT JOIN FETCH p.scores score
            LEFT JOIN FETCH score.joueur
            WHERE p.soiree.id = :soireeId
            ORDER BY p.id
            """)
    List<Partie> findDetailsBySoireeId(@Param("soireeId") Long soireeId);

    @Query("""
            SELECT DISTINCT p
            FROM Partie p
            JOIN FETCH p.soiree soiree
            LEFT JOIN FETCH p.scores score
            LEFT JOIN FETCH score.joueur
            WHERE p.jeu.id = :jeuId
            ORDER BY soiree.date DESC, p.id DESC
            """)
    List<Partie> findDetailsByJeuId(@Param("jeuId") Long jeuId);

    // Requête générée par Spring à partir du nom : supprime toutes les parties d'une soirée
    void deleteBySoireeId(Long soireeId);
}
