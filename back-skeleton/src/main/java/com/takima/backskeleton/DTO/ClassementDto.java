package com.takima.backskeleton.DTO;

// Contrat du classement Elo ; les ex aequo partagent le rang (1, 2, 2, 4…).
public record ClassementDto(Long joueurId, String pseudo, String avatar, int elo, int nbParties, int rang) { }
