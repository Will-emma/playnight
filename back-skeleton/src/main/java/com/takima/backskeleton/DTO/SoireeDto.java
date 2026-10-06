package com.takima.backskeleton.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class SoireeDto {
    private final Long id;
    private final LocalDateTime date;
    private final String lieu;
    private final List<JoueurResumeDto> joueurs;
    private final long nbParties;

    public SoireeDto(Long id, LocalDateTime date, String lieu, List<JoueurResumeDto> joueurs, long nbParties) {
        this.id = id;
        this.date = date;
        this.lieu = lieu;
        this.joueurs = joueurs;
        this.nbParties = nbParties;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getLieu() {
        return lieu;
    }

    public List<JoueurResumeDto> getJoueurs() {
        return joueurs;
    }

    public long getNbParties() {
        return nbParties;
    }
}
