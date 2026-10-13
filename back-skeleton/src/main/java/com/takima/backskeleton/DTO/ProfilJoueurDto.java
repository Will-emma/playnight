package com.takima.backskeleton.DTO;

import java.util.List;

public class ProfilJoueurDto {
    private final JoueurResumeDto joueur;
    private final Long nbParties;
    private final Long nbVictoires;
    private final List<JeuFavoriDto> jeuxFavoris;
    private final List<PartieHistoriqueDto> historique;

    public ProfilJoueurDto(
            JoueurResumeDto joueur,
            Long nbParties,
            Long nbVictoires,
            List<JeuFavoriDto> jeuxFavoris,
            List<PartieHistoriqueDto> historique) {
        this.joueur = joueur;
        this.nbParties = nbParties;
        this.nbVictoires = nbVictoires;
        this.jeuxFavoris = jeuxFavoris;
        this.historique = historique;
    }

    public JoueurResumeDto getJoueur() {
        return joueur;
    }

    public Long getNbParties() {
        return nbParties;
    }

    public Long getNbVictoires() {
        return nbVictoires;
    }

    public List<JeuFavoriDto> getJeuxFavoris() {
        return jeuxFavoris;
    }

    public List<PartieHistoriqueDto> getHistorique() {
        return historique;
    }
}
