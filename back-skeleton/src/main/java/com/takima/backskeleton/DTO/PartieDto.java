package com.takima.backskeleton.DTO;

import java.util.List;

public class PartieDto {
    private final Long id;
    private final Integer duree;
    private final JeuResumeDto jeu;
    private final List<ScoreDto> scores;

    public PartieDto(Long id, Integer duree, JeuResumeDto jeu, List<ScoreDto> scores) {
        this.id = id;
        this.duree = duree;
        this.jeu = jeu;
        this.scores = scores;
    }

    public Long getId() {
        return id;
    }

    public Integer getDuree() {
        return duree;
    }

    public JeuResumeDto getJeu() {
        return jeu;
    }

    public List<ScoreDto> getScores() {
        return scores;
    }
}
