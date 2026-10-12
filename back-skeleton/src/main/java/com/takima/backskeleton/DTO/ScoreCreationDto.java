package com.takima.backskeleton.DTO;

// Score d'un joueur dans une partie, tel qu'envoyé par le front à la création d'une soirée.
// On reçoit seulement l'id du joueur : le service ira chercher le vrai Joueur en base.
public class ScoreCreationDto {
    private Long joueurId;
    private Integer rang;
    private Integer points;
    private Integer numeroEquipe; // null si la partie ne se joue pas en équipes

    public Long getJoueurId() {
        return joueurId;
    }

    public Integer getRang() {
        return rang;
    }

    public Integer getPoints() {
        return points;
    }

    public Integer getNumeroEquipe() {
        return numeroEquipe;
    }

    // Les setters sont utilisés par Jackson pour remplir l'objet à partir du JSON reçu
    public void setJoueurId(Long joueurId) {
        this.joueurId = joueurId;
    }

    public void setRang(Integer rang) {
        this.rang = rang;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public void setNumeroEquipe(Integer numeroEquipe) {
        this.numeroEquipe = numeroEquipe;
    }
}
