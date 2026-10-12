package com.takima.backskeleton.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class PartieJoueeDto {
    private final Long partieId;
    private final LocalDateTime date;
    private final Integer duree;
    private final List<String> vainqueurs;

    public PartieJoueeDto(Long partieId, LocalDateTime date, Integer duree, List<String> vainqueurs) {
        this.partieId = partieId;
        this.date = date;
        this.duree = duree;
        this.vainqueurs = vainqueurs;
    }

    public Long getPartieId() {
        return partieId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Integer getDuree() {
        return duree;
    }

    public List<String> getVainqueurs() {
        return vainqueurs;
    }
}
