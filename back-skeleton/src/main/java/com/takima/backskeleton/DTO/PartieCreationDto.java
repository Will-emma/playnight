package com.takima.backskeleton.DTO;

import java.util.List;

// Une partie jouée pendant la soirée, avec les scores de ses joueurs.
// On reçoit seulement l'id du jeu : le service ira chercher le vrai Jeu en base.
public class PartieCreationDto {
    private Long jeuId;
    private Integer duree; // en minutes
    private List<ScoreCreationDto> scores;

    public Long getJeuId() {
        return jeuId;
    }

    public Integer getDuree() {
        return duree;
    }

    public List<ScoreCreationDto> getScores() {
        return scores;
    }

    // Les setters sont utilisés par Jackson pour remplir l'objet à partir du JSON reçu
    public void setJeuId(Long jeuId) {
        this.jeuId = jeuId;
    }

    public void setDuree(Integer duree) {
        this.duree = duree;
    }

    public void setScores(List<ScoreCreationDto> scores) {
        this.scores = scores;
    }
}
