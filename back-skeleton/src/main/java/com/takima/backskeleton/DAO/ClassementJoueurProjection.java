package com.takima.backskeleton.DAO;

public interface ClassementJoueurProjection {
    Long getJeuId();
    String getNom();
    String getType();
    String getImage();
    Integer getElo();
    Integer getNbParties();
    Long getRang();
}
