package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.FicheJeuDto;
import com.takima.backskeleton.DTO.ClassementDto;
import com.takima.backskeleton.DTO.MeilleurJoueurDto;
import com.takima.backskeleton.services.ClassementService;
import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.DTO.RawgImportDto;
import com.takima.backskeleton.DTO.RawgJeuDto;
import com.takima.backskeleton.models.TypeJeu;
import com.takima.backskeleton.services.JeuService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RequestMapping("jeux")
@RestController
public class JeuController {
    private final JeuService jeuService;

    private final ClassementService classementService;

    public JeuController(JeuService jeuService, ClassementService classementService) {
        this.jeuService = jeuService;
        this.classementService = classementService;
    }

    @GetMapping("")
    public List<JeuDto> listJeux(@RequestParam(required = false) TypeJeu type) {
        return jeuService.findAll(type);
    }

    @GetMapping("/{id}")
    public JeuDto getJeuById(@PathVariable Long id) {
        return jeuService.getById(id);
    }

    @GetMapping("/{id}/fiche")
    public FicheJeuDto getFicheJeu(@PathVariable Long id) {
        return jeuService.getFiche(id);
    }

    @GetMapping("/{id}/classement")
    public List<ClassementDto> getClassement(@PathVariable Long id) {
        return classementService.getClassement(id);
    }

    @GetMapping("/{id}/classement-victoires")
    public List<MeilleurJoueurDto> getClassementVictoires(@PathVariable Long id) {
        return jeuService.getClassementVictoires(id);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public JeuDto addJeu(@RequestBody JeuDto jeuDto) {
        return jeuService.addJeu(jeuDto);
    }

    // Recherche de jeux vidéo sur RAWG, pas encore dans la ludothèque : GET /jeux/rawg?recherche=zelda
    @GetMapping("/rawg")
    public List<RawgJeuDto> rechercherSurRawg(@RequestParam String recherche) {
        return jeuService.rechercherSurRawg(recherche);
    }

    // Import d'un jeu RAWG dans la ludothèque, avec le nombre de joueurs en corps de requête
    @PostMapping("/rawg/{idRawg}")
    @ResponseStatus(HttpStatus.CREATED)
    public JeuDto importerDepuisRawg(@PathVariable Long idRawg, @RequestBody RawgImportDto importDto) {
        return jeuService.importerDepuisRawg(idRawg, importDto);
    }

    @PutMapping("/{id}")
    public JeuDto updateJeu(@RequestBody JeuDto jeuDto, @PathVariable Long id) {
        return jeuService.updateJeu(jeuDto, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJeu(@PathVariable Long id) {
        jeuService.deleteById(id);
    }
}
