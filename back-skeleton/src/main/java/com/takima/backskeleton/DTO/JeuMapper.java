package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Jeu;

public class JeuMapper {
    public static Jeu fromDto(JeuDto dto, Long id) {
        return new Jeu.Builder()
                .id(id)
                .nom(dto.getNom())
                .type(dto.getType())
                .plateforme(dto.getPlateforme())
                .nbJoueursMin(dto.getNbJoueursMin())
                .nbJoueursMax(dto.getNbJoueursMax())
                .image(dto.getImage())
                .build();
    }

    public static JeuDto toDto(Jeu jeu) {
        return new JeuDto.JeuDtoBuilder()
                .id(jeu.getId())
                .nom(jeu.getNom())
                .type(jeu.getType())
                .plateforme(jeu.getPlateforme())
                .nbJoueursMin(jeu.getNbJoueursMin())
                .nbJoueursMax(jeu.getNbJoueursMax())
                .image(jeu.getImage())
                .build();
    }
}
