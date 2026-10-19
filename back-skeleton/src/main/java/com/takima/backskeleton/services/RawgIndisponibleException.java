package com.takima.backskeleton.services;

// RAWG ne peut pas être utilisé : clé d'API absente ou refusée, ou API qui ne répond pas
public class RawgIndisponibleException extends RuntimeException {
    public RawgIndisponibleException(String message) {
        super(message);
    }
}
