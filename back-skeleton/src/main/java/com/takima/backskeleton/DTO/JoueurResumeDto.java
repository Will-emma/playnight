package com.takima.backskeleton.DTO;

public class JoueurResumeDto {
    private final Long id;
    private final String pseudo;
    private final String avatar;

    public JoueurResumeDto(Long id, String pseudo, String avatar) {
        this.id = id;
        this.pseudo = pseudo;
        this.avatar = avatar;
    }

    public Long getId() {
        return id;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getAvatar() {
        return avatar;
    }
}
