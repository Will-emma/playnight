package com.takima.backskeleton.DTO;

// Le rang est absent tant que le joueur n'a aucun résultat classé.
public record ClassementGlobalDto(int elo, Integer rang) { }
