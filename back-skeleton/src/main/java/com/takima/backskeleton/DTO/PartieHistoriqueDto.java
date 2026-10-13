package com.takima.backskeleton.DTO;

import java.time.LocalDateTime;

public class PartieHistoriqueDto {
    private final Long partieId;
    private final LocalDateTime date;
    private final JeuResumeDto jeu;
    private final Integer rang;
    private final Integer points;

    public PartieHistoriqueDto(Long partieId, LocalDateTime date, JeuResumeDto jeu, Integer rang, Integer points) {
        this.partieId = partieId;
        this.date = date;
        this.jeu = jeu;
        this.rang = rang;
        this.points = points;
    }

    public Long getPartieId() {
        return partieId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public JeuResumeDto getJeu() {
        return jeu;
    }

    public Integer getRang() {
        return rang;
    }

    public Integer getPoints() {
        return points;
    }
}
