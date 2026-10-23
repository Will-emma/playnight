package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreDao extends JpaRepository<Score, Long> {
    // Un rang 1 vaut une victoire, y compris pour chaque membre d'une équipe
    // gagnante ou pour plusieurs premiers ex aequo. Un score unique par partie/joueur.
    @Query("""
            SELECT j.id AS joueurId, j.pseudo AS pseudo, j.avatar AS avatar,
                   COUNT(s) AS nbVictoires
            FROM Score s JOIN s.joueur j
            WHERE s.partie.jeu.id = :jeuId AND s.rang = 1
            GROUP BY j.id, j.pseudo, j.avatar
            ORDER BY COUNT(s) DESC, j.pseudo ASC, j.id ASC
            """)
    List<ClassementVictoiresProjection> findClassementVictoiresByJeuId(@Param("jeuId") Long jeuId);

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
