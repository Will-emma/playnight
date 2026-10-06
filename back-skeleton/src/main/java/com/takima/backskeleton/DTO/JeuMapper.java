package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Jeu;

public class JeuMapper {
    private JeuMapper() {
    }

    public static JeuResumeDto toResumeDto(Jeu jeu) {
        return new JeuResumeDto(jeu.getId(), jeu.getNom(), jeu.getType(), jeu.getImage());
    }
}
