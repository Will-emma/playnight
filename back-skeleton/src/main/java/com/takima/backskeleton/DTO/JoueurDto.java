package com.takima.backskeleton.DTO;

public class JoueurDto {
    private Long id;
    private String pseudo;
    private String avatar;

    public Long getId() {
        return id;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public static final class JoueurDtoBuilder {
        private Long id;
        private String pseudo;
        private String avatar;

        public JoueurDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public JoueurDtoBuilder pseudo(String pseudo) {
            this.pseudo = pseudo;
            return this;
        }

        public JoueurDtoBuilder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public JoueurDto build() {
            JoueurDto joueurDto = new JoueurDto();
            joueurDto.id = this.id;
            joueurDto.pseudo = this.pseudo;
            joueurDto.avatar = this.avatar;
            return joueurDto;
        }
    }
}