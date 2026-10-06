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
}
