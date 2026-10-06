package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Joueur;

public class JoueurMapper {
    private JoueurMapper() {
    }

    public static Joueur fromDto(JoueurDto dto, Long id) {
        return new Joueur.Builder()
                .id(id)
                .pseudo(dto.getPseudo())
                .avatar(dto.getAvatar())
                .build();
    }

    public static JoueurDto toDto(Joueur joueur) {
        return new JoueurDto.JoueurDtoBuilder()
                .id(joueur.getId())
                .pseudo(joueur.getPseudo())
                .avatar(joueur.getAvatar())
                .build();
    }

    public static JoueurResumeDto toResumeDto(Joueur joueur) {
        return new JoueurResumeDto(joueur.getId(), joueur.getPseudo(), joueur.getAvatar());
    }
}
