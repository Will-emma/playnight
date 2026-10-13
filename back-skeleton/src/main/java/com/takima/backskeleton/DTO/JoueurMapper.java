package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;

import java.util.List;

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

    public static ProfilJoueurDto toProfilDto(
            Joueur joueur,
            List<Score> scores,
            Long nbVictoires,
            List<JeuFavoriDto> jeuxFavoris) {
        return new ProfilJoueurDto(
                toResumeDto(joueur),
                (long) scores.size(),
                nbVictoires,
                jeuxFavoris,
                scores.stream()
                        .map(JoueurMapper::toPartieHistoriqueDto)
                        .toList()
        );
    }

    public static JeuFavoriDto toJeuFavoriDto(Jeu jeu, Long nbParties) {
        return new JeuFavoriDto(JeuMapper.toResumeDto(jeu), nbParties);
    }

    private static PartieHistoriqueDto toPartieHistoriqueDto(Score score) {
        Partie partie = score.getPartie();
        return new PartieHistoriqueDto(
                partie.getId(),
                partie.getSoiree().getDate(),
                JeuMapper.toResumeDto(partie.getJeu()),
                score.getRang(),
                score.getPoints()
        );
    }
}