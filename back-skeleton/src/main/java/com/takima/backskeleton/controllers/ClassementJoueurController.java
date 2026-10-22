package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.ClassementJoueurDto;
import com.takima.backskeleton.DTO.ProgressionEloDto;
import com.takima.backskeleton.services.ProgressionEloService;
import com.takima.backskeleton.DTO.ClassementGlobalDto;
import com.takima.backskeleton.services.ClassementService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("joueurs")
public class ClassementJoueurController {
    private final ClassementService classementService;

    private final ProgressionEloService progressionService;

    public ClassementJoueurController(ClassementService classementService, ProgressionEloService progressionService) {
        this.classementService = classementService;
        this.progressionService = progressionService;
    }

    @GetMapping("/{id}/progression-elo")
    public ProgressionEloDto progression(@PathVariable Long id) {
        return progressionService.getProgression(id);
    }

    @GetMapping("/{id}/classement-global")
    public ClassementGlobalDto classementGlobal(@PathVariable Long id) {
        return classementService.getClassementGlobal(id);
    }

    @GetMapping("/{id}/classements")
    public List<ClassementJoueurDto> classements(@PathVariable Long id) {
        return classementService.getClassementsJoueur(id);
    }
}
