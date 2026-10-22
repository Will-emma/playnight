package com.takima.backskeleton.services;

public class AvatarTropVolumineuxException extends IllegalArgumentException {
    public AvatarTropVolumineuxException() {
        super("La photo ne doit pas dépasser 2 Mo.");
    }
}
