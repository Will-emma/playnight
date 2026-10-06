package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.TypeJeu;

public class JeuResumeDto {
    private final Long id;
    private final String nom;
    private final TypeJeu type;
    private final String image;

    public JeuResumeDto(Long id, String nom, TypeJeu type, String image) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.image = image;
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
