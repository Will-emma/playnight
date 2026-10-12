package com.takima.backskeleton.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "soirees")
public class Soiree {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "date", nullable = false)
    private LocalDateTime date;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lieu;
    @ManyToMany
    @JoinTable(
            name = "soiree_joueur",
            joinColumns = @JoinColumn(name = "soiree_id"),
            inverseJoinColumns = @JoinColumn(name = "joueur_id"))
    private List<Joueur> joueurs;
    @OneToMany(mappedBy = "soiree")
    private List<Partie> parties;

    public Soiree() {
    }

    private Soiree(Builder builder) {
        this.date = builder.date;
        this.lieu = builder.lieu;
        this.joueurs = builder.joueurs;
        this.parties = new ArrayList<>(); // les parties sont ajoutées une fois la soirée enregistrée
    }

    // Utilisé par PUT /soirees/{id} : remplace les infos de la soirée.
    // Les anciennes parties sont supprimées par le service, on repart donc d'une liste vide.
    public void modifier(LocalDateTime date, String lieu, List<Joueur> joueurs) {
        this.date = date;
        this.lieu = lieu;
        this.joueurs = joueurs;
        this.parties = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getLieu() {
        return lieu;
    }

    public List<Joueur> getJoueurs() {
        return joueurs;
    }

    public List<Partie> getParties() {
        return parties;
    }

    public static class Builder {
        private LocalDateTime date;
        private String lieu;
        private List<Joueur> joueurs;

        public Builder date(LocalDateTime date) {
            this.date = date;
            return this;
        }

        public Builder lieu(String lieu) {
            this.lieu = lieu;
            return this;
        }

        public Builder joueurs(List<Joueur> joueurs) {
            this.joueurs = joueurs;
            return this;
        }

        public Soiree build() {
            return new Soiree(this);
        }
    }
}
