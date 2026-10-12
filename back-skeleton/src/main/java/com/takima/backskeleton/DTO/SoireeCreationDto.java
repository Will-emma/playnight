package com.takima.backskeleton.DTO;

import java.time.LocalDateTime;
import java.util.List;

// Corps de la requête POST /soirees et PUT /soirees/{id} :
// toute la soirée (présents, parties, scores) arrive en une seule fois.
public class SoireeCreationDto {
    private LocalDateTime date;
    private String lieu;
    private List<Long> joueurIds;              // ids des joueurs présents
    private List<PartieCreationDto> parties;   // parties jouées, avec leurs scores

    public LocalDateTime getDate() {
        return date;
    }

    public String getLieu() {
        return lieu;
    }

    public List<Long> getJoueurIds() {
        return joueurIds;
    }

    public List<PartieCreationDto> getParties() {
        return parties;
    }

    // Les setters sont utilisés par Jackson pour remplir l'objet à partir du JSON reçu
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public void setJoueurIds(List<Long> joueurIds) {
        this.joueurIds = joueurIds;
    }

    public void setParties(List<PartieCreationDto> parties) {
        this.parties = parties;
    }
}
