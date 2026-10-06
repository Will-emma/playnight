package com.takima.backskeleton.DAO;

import com.takima.backskeleton.models.Joueur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JoueurDao extends JpaRepository<Joueur, Long> {
}