
package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;

import java.util.List;

public class JeuMapper {

    private JeuMapper() {
    }

    public static Jeu fromDto(JeuDto dto, Long id) {
        return new Jeu.Builder()
                .id(id)
                .nom(dto.getNom())
                .type(dto.getType())
                .plateforme(dto.getPlateforme())
                .nbJoueursMin(dto.getNbJoueursMin())
                .nbJoueursMax(dto.getNbJoueursMax())
                .image(dto.getImage())
                .description(dto.getDescription())
                .dureeEstimee(dto.getDureeEstimee())
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
                .description(jeu.getDescription())
                .dureeEstimee(jeu.getDureeEstimee())
                .build();
    }

    public static JeuResumeDto toResumeDto(Jeu jeu) {
        return new JeuResumeDto(
                jeu.getId(),
                jeu.getNom(),
                jeu.getType(),
                jeu.getImage()
        );
    }

    public static FicheJeuDto toFicheDto(Jeu jeu, List<Partie> parties, List<MeilleurJoueurDto> meilleursJoueurs) {
        return new FicheJeuDto(
                toDto(jeu),
                (long) parties.size(),
                meilleursJoueurs,
                parties.stream()
                        .map(JeuMapper::toPartieJoueeDto)
                        .toList()
        );
    }

    public static MeilleurJoueurDto toMeilleurJoueurDto(Joueur joueur, Long nbVictoires) {
        return new MeilleurJoueurDto(JoueurMapper.toResumeDto(joueur), nbVictoires);
    }

    private static PartieJoueeDto toPartieJoueeDto(Partie partie) {
        List<String> vainqueurs = partie.getScores().stream()
                .filter(Score::estVainqueur)
                .map(score -> score.getJoueur().getPseudo())
                .sorted()
                .toList();
        return new PartieJoueeDto(
                partie.getId(),
                partie.getSoiree().getDate(),
                partie.getDuree(),
                vainqueurs
        );
    }
}
