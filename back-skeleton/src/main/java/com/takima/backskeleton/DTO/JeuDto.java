package com.takima.backskeleton.DTO;

import com.takima.backskeleton.models.TypeJeu;

public class JeuDto {
    private Long id;
    private String nom;
    private TypeJeu type;
    private String plateforme;
    private Integer nbJoueursMin;
    private Integer nbJoueursMax;
    private String image;
    private String description;
    private Integer dureeEstimee;

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public TypeJeu getType() {
        return type;
    }

    public String getPlateforme() {
        return plateforme;
    }

    public Integer getNbJoueursMin() {
        return nbJoueursMin;
    }

    public Integer getNbJoueursMax() {
        return nbJoueursMax;
    }

    public String getImage() {
        return image;
    }

    public String getDescription() { return description; }
    public Integer getDureeEstimee() { return dureeEstimee; }
    public void setDescription(String description) { this.description = description; }
    public void setDureeEstimee(Integer dureeEstimee) { this.dureeEstimee = dureeEstimee; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setType(TypeJeu type) {
        this.type = type;
    }

    public void setPlateforme(String plateforme) {
        this.plateforme = plateforme;
    }

    public void setNbJoueursMin(Integer nbJoueursMin) {
        this.nbJoueursMin = nbJoueursMin;
    }

    public void setNbJoueursMax(Integer nbJoueursMax) {
        this.nbJoueursMax = nbJoueursMax;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public static final class JeuDtoBuilder {
        private Long id;
        private String nom;
        private TypeJeu type;
        private String plateforme;
        private Integer nbJoueursMin;
        private Integer nbJoueursMax;
        private String image;
        private String description;
        private Integer dureeEstimee;

        public JeuDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public JeuDtoBuilder nom(String nom) {
            this.nom = nom;
            return this;
        }

        public JeuDtoBuilder type(TypeJeu type) {
            this.type = type;
            return this;
        }

        public JeuDtoBuilder plateforme(String plateforme) {
            this.plateforme = plateforme;
            return this;
        }

        public JeuDtoBuilder nbJoueursMin(Integer nbJoueursMin) {
            this.nbJoueursMin = nbJoueursMin;
            return this;
        }

        public JeuDtoBuilder nbJoueursMax(Integer nbJoueursMax) {
            this.nbJoueursMax = nbJoueursMax;
            return this;
        }

        public JeuDtoBuilder image(String image) {
            this.image = image;
            return this;
        }

        public JeuDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public JeuDtoBuilder dureeEstimee(Integer dureeEstimee) {
            this.dureeEstimee = dureeEstimee;
            return this;
        }

        public JeuDto build() {
            JeuDto jeuDto = new JeuDto();
            jeuDto.id = this.id;
            jeuDto.nom = this.nom;
            jeuDto.type = this.type;
            jeuDto.plateforme = this.plateforme;
            jeuDto.nbJoueursMin = this.nbJoueursMin;
            jeuDto.nbJoueursMax = this.nbJoueursMax;
            jeuDto.image = this.image;
            jeuDto.description = this.description;
            jeuDto.dureeEstimee = this.dureeEstimee;
            return jeuDto;
        }
    }
}
