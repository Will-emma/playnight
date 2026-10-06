package com.takima.backskeleton.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "joueurs")
public class Joueur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String pseudo;
    @Column(columnDefinition = "TEXT")
    private String avatar;
    @OneToMany(mappedBy = "joueur")
    private List<Score> scores;

    private Joueur(Builder builder) {
        this.id = builder.id;
        this.pseudo = builder.pseudo;
        this.avatar = builder.avatar;
    }

    public Joueur() {
    }

    public Long getId() {
        return id;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getAvatar() {
        return avatar;
    }

    public List<Score> getScores() {
        return scores;
    }

    public static class Builder {
        private Long id;
        private String pseudo;
        private String avatar;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder pseudo(String pseudo) {
            this.pseudo = pseudo;
            return this;
        }

        public Builder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public Joueur build() {
            return new Joueur(this);
        }
    }
}