package com.takima.backskeleton.DTO;

public class MeilleurJoueurDto {
    private final JoueurResumeDto joueur;
    private final Long nbVictoires;

    public MeilleurJoueurDto(JoueurResumeDto joueur, Long nbVictoires) {
        this.joueur = joueur;
        this.nbVictoires = nbVictoires;
    }

    public JoueurResumeDto getJoueur() {
        return joueur;
    }

    public Long getNbVictoires() {
        return nbVictoires;
    }
}
