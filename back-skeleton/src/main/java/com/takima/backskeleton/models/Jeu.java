package com.takima.backskeleton.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jeux")
public class Jeu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String nom;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TypeJeu type;
    @Column(columnDefinition = "TEXT")
    private String image;

    public Jeu() {
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public TypeJeu getType() {
        return type;
    }

    public String getImage() {
        return image;
    }
}
