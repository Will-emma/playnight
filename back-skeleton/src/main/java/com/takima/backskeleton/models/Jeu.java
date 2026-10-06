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
    @Column(nullable = false, length = 20)
    private TypeJeu type;
    @Column(columnDefinition = "TEXT")
    private String plateforme;
    @Column(name = "nb_joueurs_min", nullable = false)
    private Integer nbJoueursMin;
    @Column(name = "nb_joueurs_max", nullable = false)
    private Integer nbJoueursMax;
    @Column(columnDefinition = "TEXT")
    private String image;

    private Jeu(Builder builder) {
        this.id = builder.id;
        this.nom = builder.nom;
        this.type = builder.type;
        this.plateforme = builder.plateforme;
        this.nbJoueursMin = builder.nbJoueursMin;
        this.nbJoueursMax = builder.nbJoueursMax;
        this.image = builder.image;
    }

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

    public String getPlateforme() {
        return plateforme;
    }

    public Integer getNbJoueursMin() {
        return nbJoueursMin;
    }

    public Integer getNbJoueursMax() {
        return nbJoueursMax;
    }

    public String getImage() {
        return image;
    }

    public static class Builder {
        private Long id;
        private String nom;
        private TypeJeu type;
        private String plateforme;
        private Integer nbJoueursMin;
        private Integer nbJoueursMax;
        private String image;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder nom(String nom) {
            this.nom = nom;
            return this;
        }

        public Builder type(TypeJeu type) {
            this.type = type;
            return this;
        }

        public Builder plateforme(String plateforme) {
            this.plateforme = plateforme;
            return this;
        }

        public Builder nbJoueursMin(Integer nbJoueursMin) {
            this.nbJoueursMin = nbJoueursMin;
            return this;
        }

        public Builder nbJoueursMax(Integer nbJoueursMax) {
            this.nbJoueursMax = nbJoueursMax;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Jeu build() {
            return new Jeu(this);
        }
    }
}
