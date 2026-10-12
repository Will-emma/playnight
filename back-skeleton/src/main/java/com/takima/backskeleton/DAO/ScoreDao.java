package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreDao extends JpaRepository<Score, Long> {
}
