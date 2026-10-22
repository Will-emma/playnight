package com.takima.backskeleton.DTO;

import java.util.List;

public record ProgressionEloDto(ClassementGlobalDto classement, int meilleurElo,
                               int variationTotale, int nbParties, List<PointEloDto> points) { }
