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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "classements", uniqueConstraints = @UniqueConstraint(columnNames = {"joueur_id", "jeu_id"}))
public class Classement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Integer elo = 1000;
    @Column(name = "nb_parties", nullable = false)
    private Integer nbParties = 0;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "joueur_id", nullable = false)
    private Joueur joueur;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jeu_id", nullable = false)
    private Jeu jeu;

    protected Classement() { }

    public Classement(Joueur joueur, Jeu jeu) {
        this.joueur = joueur;
        this.jeu = jeu;
    }

    public Long getId() { return id; }
    public Integer getElo() { return elo; }
    public Integer getNbParties() { return nbParties; }
    public Joueur getJoueur() { return joueur; }
    public Jeu getJeu() { return jeu; }

    public void enregistrerPartie(int nouvelElo) {
        elo = nouvelElo;
        nbParties++;
    }
}
