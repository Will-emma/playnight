package com.takima.backskeleton.DTO;

// Un jeu vidéo trouvé sur RAWG, au format PlayNight (pas encore dans la ludothèque)
public class RawgJeuDto {
    private final Long idRawg;
    private final String nom;
    private final String image;
    private final String plateforme;
    private final String dateSortie;

    public RawgJeuDto(Long idRawg, String nom, String image, String plateforme, String dateSortie) {
        this.idRawg = idRawg;
        this.nom = nom;
        this.image = image;
        this.plateforme = plateforme;
        this.dateSortie = dateSortie;
    }

    public Long getIdRawg() {
        return idRawg;
    }

    public String getNom() {
        return nom;
    }

    // null si RAWG n'a pas d'image pour ce jeu
    public String getImage() {
        return image;
    }

    // Les premières plateformes séparées par des virgules ("PC, PlayStation 5, Xbox One, Nintendo Switch"), ou null
    public String getPlateforme() {
        return plateforme;
    }

    // "2017-03-03", ou null si la date est inconnue ; aide à distinguer les jeux de même nom
    public String getDateSortie() {
        return dateSortie;
    }
}
