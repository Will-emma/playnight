package com.takima.backskeleton.DTO;

import java.util.List;

public class FicheJeuDto {
    private final JeuDto jeu;
    private final Long nbParties;
    private final List<MeilleurJoueurDto> meilleursJoueurs;
    private final List<PartieJoueeDto> parties;

    public FicheJeuDto(
            JeuDto jeu,
            Long nbParties,
            List<MeilleurJoueurDto> meilleursJoueurs,
            List<PartieJoueeDto> parties) {
        this.jeu = jeu;
        this.nbParties = nbParties;
        this.meilleursJoueurs = meilleursJoueurs;
        this.parties = parties;
    }

    public JeuDto getJeu() {
        return jeu;
    }

    public Long getNbParties() {
        return nbParties;
    }

    public List<MeilleurJoueurDto> getMeilleursJoueurs() {
        return meilleursJoueurs;
    }

    public List<PartieJoueeDto> getParties() {
        return parties;
    }
}
