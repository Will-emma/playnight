package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreDao extends JpaRepository<Score, Long> {
    // Un score par partie jouée (contrainte unique partie/joueur), du plus récent au plus ancien
    @Query("""
            SELECT s
            FROM Score s
            JOIN FETCH s.partie p
            JOIN FETCH p.soiree soiree
            JOIN FETCH p.jeu
            WHERE s.joueur.id = :joueurId
            ORDER BY soiree.date DESC, p.id DESC
            """)
    List<Score> findDetailsByJoueurId(@Param("joueurId") Long joueurId);
}
