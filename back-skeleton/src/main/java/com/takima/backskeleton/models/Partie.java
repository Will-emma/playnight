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

import java.util.ArrayList;
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

    private Partie(Builder builder) {
        this.duree = builder.duree;
        this.soiree = builder.soiree;
        this.jeu = builder.jeu;
        this.scores = new ArrayList<>(); // les scores sont ajoutés une fois la partie enregistrée
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

    public static class Builder {
        private Integer duree;
        private Soiree soiree;
        private Jeu jeu;

        public Builder duree(Integer duree) {
            this.duree = duree;
            return this;
        }

        public Builder soiree(Soiree soiree) {
            this.soiree = soiree;
            return this;
        }

        public Builder jeu(Jeu jeu) {
            this.jeu = jeu;
            return this;
        }

        public Partie build() {
            return new Partie(this);
        }
    }
}
