package com.takima.backskeleton.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "parties")
public class Partie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer duree;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soiree_id", nullable = false)
    private Soiree soiree;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jeu_id", nullable = false)
    private Jeu jeu;
    @OneToMany(mappedBy = "partie")
    private List<Score> scores;

    public Partie() {
    }

    public Long getId() {
        return id;
    }

    public Integer getDuree() {
        return duree;
    }

    public Soiree getSoiree() {
        return soiree;
    }

    public Jeu getJeu() {
        return jeu;
    }

    public List<Score> getScores() {
        return scores;
    }
}
