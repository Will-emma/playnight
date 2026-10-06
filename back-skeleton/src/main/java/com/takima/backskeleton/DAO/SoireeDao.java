package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Soiree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoireeDao extends JpaRepository<Soiree, Long> {
    @Query("SELECT DISTINCT s FROM Soiree s LEFT JOIN FETCH s.joueurs ORDER BY s.date DESC, s.id DESC")
    List<Soiree> findAllWithJoueurs();

    @Query("SELECT DISTINCT s FROM Soiree s LEFT JOIN FETCH s.joueurs WHERE s.id = :id")
    Optional<Soiree> findWithJoueursById(@Param("id") Long id);

    @Query("""
            SELECT s.id AS soireeId, COUNT(p) AS nbParties
            FROM Soiree s LEFT JOIN s.parties p
            GROUP BY s.id
            """)
    List<SoireePartieCount> countPartiesBySoiree();
}
