package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Classement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ClassementDao extends JpaRepository<Classement, Long> {
    // Moyenne de ses Elo par jeu, chaque jeu comptant à parts égales.
    // Le rang porte sur le score arrondi affiché et tous les joueurs classés.
    @Query(value = """
            SELECT elo, rang FROM (
                SELECT joueur_id, elo, RANK() OVER (ORDER BY elo DESC) AS rang
                FROM (
                    SELECT joueur_id, CAST(ROUND(AVG(elo)) AS integer) AS elo
                    FROM classements WHERE nb_parties > 0 GROUP BY joueur_id
                ) moyennes
            ) globaux
            WHERE joueur_id = :joueurId
            """, nativeQuery = true)
    Optional<ClassementGlobalProjection> findClassementGlobal(@Param("joueurId") Long joueurId);

    // Le filtre joueur est appliqué APRÈS le rang : il reste relatif à tous les
    // joueurs du jeu, y compris en cas d'ex aequo. Une seule requête pour tous ses jeux.
    @Query(value = """
            SELECT j.id AS "jeuId", j.nom AS nom, j.type AS type, j.image AS image,
                   c.elo AS elo, c.nb_parties AS "nbParties", c.rang AS rang
            FROM (
                SELECT jeu_id, joueur_id, elo, nb_parties,
                       RANK() OVER (PARTITION BY jeu_id ORDER BY elo DESC) AS rang
                FROM classements
                WHERE jeu_id IN (SELECT jeu_id FROM classements WHERE joueur_id = :joueurId)
            ) c
            JOIN jeux j ON j.id = c.jeu_id
            WHERE c.joueur_id = :joueurId
            ORDER BY j.nom, j.id
            """, nativeQuery = true)
    List<ClassementJoueurProjection> findClassementsByJoueurId(@Param("joueurId") Long joueurId);

    @Query("""
            SELECT c FROM Classement c JOIN FETCH c.joueur j
            WHERE c.jeu.id = :jeuId ORDER BY c.elo DESC, j.pseudo ASC, j.id ASC
            """)
    List<Classement> findClassementByJeuId(@Param("jeuId") Long jeuId);
}
