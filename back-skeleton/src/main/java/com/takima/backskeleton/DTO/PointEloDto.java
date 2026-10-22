package com.takima.backskeleton.DTO;

import java.time.LocalDateTime;

// Le point initial (1000) n'a ni partie ni date.
public record PointEloDto(Long partieId, LocalDateTime date, String jeu, int elo, int variation) { }
