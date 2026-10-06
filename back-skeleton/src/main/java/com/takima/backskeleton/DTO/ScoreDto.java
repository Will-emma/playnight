package com.takima.backskeleton.DTO;

public class ScoreDto {
    private final Long id;
    private final Integer points;
    private final Integer rang;
    private final Integer numeroEquipe;
    private final JoueurResumeDto joueur;

    public ScoreDto(Long id, Integer points, Integer rang, Integer numeroEquipe, JoueurResumeDto joueur) {
        this.id = id;
        this.points = points;
        this.rang = rang;
        this.numeroEquipe = numeroEquipe;
        this.joueur = joueur;
    }

    public Long getId() {
        return id;
    }

    public Integer getPoints() {
        return points;
    }

    public Integer getRang() {
        return rang;
    }

    public Integer getNumeroEquipe() {
        return numeroEquipe;
    }

    public JoueurResumeDto getJoueur() {
        return joueur;
    }
}
