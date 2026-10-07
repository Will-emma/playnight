package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Soiree;

import java.util.List;

public class SoireeMapper {
    private SoireeMapper() {
    }

    public static SoireeDto toDto(Soiree soiree, long nbParties) {
        return new SoireeDto(
                soiree.getId(),
                soiree.getDate(),
                soiree.getLieu(),
                mapJoueurs(soiree),
                nbParties);
    }

    public static SoireeDetailDto toDetailDto(Soiree soiree, List<Partie> parties) {
        return new SoireeDetailDto(
                soiree.getId(),
                soiree.getDate(),
                soiree.getLieu(),
                mapJoueurs(soiree),
                parties.stream().map(PartieMapper::toDto).toList());
    }

    private static List<JoueurResumeDto> mapJoueurs(Soiree soiree) {
        return soiree.getJoueurs().stream()
                .map(JoueurMapper::toResumeDto)
                .toList();
    }
}
