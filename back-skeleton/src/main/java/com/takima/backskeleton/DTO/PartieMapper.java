package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;

import java.util.Comparator;
import java.util.List;

public class PartieMapper {
    private PartieMapper() {
    }

    public static PartieDto toDto(Partie partie) {
        List<ScoreDto> scores = partie.getScores().stream()
                .sorted(Comparator.comparing(Score::getRang).thenComparing(Score::getId))
                .map(ScoreMapper::toDto)
                .toList();

        return new PartieDto(
                partie.getId(),
                partie.getDuree(),
                JeuMapper.toResumeDto(partie.getJeu()),
                scores);
    }
}
