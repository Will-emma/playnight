package com.takima.backskeleton.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class SoireeDetailDto {
    private final Long id;
    private final LocalDateTime date;
    private final String lieu;
    private final List<JoueurResumeDto> joueurs;
    private final List<PartieDto> parties;

    public SoireeDetailDto(
            Long id,
            LocalDateTime date,
            String lieu,
            List<JoueurResumeDto> joueurs,
            List<PartieDto> parties) {
        this.id = id;
        this.date = date;
        this.lieu = lieu;
        this.joueurs = joueurs;
        this.parties = parties;
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

    public List<PartieDto> getParties() {
        return parties;
    }
}
