package com.takima.backskeleton.DTO;

// Corps de POST /jeux/rawg/{idRawg} : ce que RAWG ne fournit pas et que l'utilisateur saisit avant l'import
public class RawgImportDto {
    private Integer nbJoueursMin;
    private Integer nbJoueursMax;

    public Integer getNbJoueursMin() {
        return nbJoueursMin;
    }

    public Integer getNbJoueursMax() {
        return nbJoueursMax;
    }

    // Les setters sont utilisés par Jackson pour remplir l'objet à partir du JSON reçu
    public void setNbJoueursMin(Integer nbJoueursMin) {
        this.nbJoueursMin = nbJoueursMin;
    }

    public void setNbJoueursMax(Integer nbJoueursMax) {
        this.nbJoueursMax = nbJoueursMax;
    }
}
