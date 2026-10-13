package com.takima.backskeleton.DTO;

public class JeuFavoriDto {
    private final JeuResumeDto jeu;
    private final Long nbParties;

    public JeuFavoriDto(JeuResumeDto jeu, Long nbParties) {
        this.jeu = jeu;
        this.nbParties = nbParties;
    }

    public JeuResumeDto getJeu() {
        return jeu;
    }

    public Long getNbParties() {
        return nbParties;
    }
}
