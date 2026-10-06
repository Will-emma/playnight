package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.Score;

public class ScoreMapper {
    private ScoreMapper() {
    }

    public static ScoreDto toDto(Score score) {
        return new ScoreDto(
                score.getId(),
                score.getPoints(),
                score.getRang(),
                score.getNumeroEquipe(),
                JoueurMapper.toResumeDto(score.getJoueur()));
    }
}
