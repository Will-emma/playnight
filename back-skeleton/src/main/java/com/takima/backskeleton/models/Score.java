package com.takima.backskeleton.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "scores")
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer points;
    @Column(nullable = false)
    private Integer rang;
    @Column(name = "numero_equipe")
    private Integer numeroEquipe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partie_id", nullable = false)
    private Partie partie;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "joueur_id", nullable = false)
    private Joueur joueur;

    public Score() {
    }

    private Score(Builder builder) {
        this.points = builder.points;
        this.rang = builder.rang;
        this.numeroEquipe = builder.numeroEquipe;
        this.partie = builder.partie;
        this.joueur = builder.joueur;
    }

    public Long getId() {
        return id;
    }

    public Integer getPoints() {
        return points;
    }

    public Integer getRang() {
        return rang;
    }

    public Integer getNumeroEquipe() {
        return numeroEquipe;
    }

    public Partie getPartie() {
        return partie;
    }

    public Joueur getJoueur() {
        return joueur;
    }

    public static class Builder {
        private Integer points;
        private Integer rang;
        private Integer numeroEquipe;
        private Partie partie;
        private Joueur joueur;

        public Builder points(Integer points) {
            this.points = points;
            return this;
        }

        public Builder rang(Integer rang) {
            this.rang = rang;
            return this;
        }

        public Builder numeroEquipe(Integer numeroEquipe) {
            this.numeroEquipe = numeroEquipe;
            return this;
        }

        public Builder partie(Partie partie) {
            this.partie = partie;
            return this;
        }

        public Builder joueur(Joueur joueur) {
            this.joueur = joueur;
            return this;
        }

        public Score build() {
            return new Score(this);
        }
    }
}
