package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.services.JoueurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@CrossOrigin
@RequestMapping("joueurs")
@RestController
public class JoueurController {
    private final JoueurService joueurService;

    public JoueurController(JoueurService joueurService) {
        this.joueurService = joueurService;
    }

    @GetMapping("")
    public List<JoueurDto> listJoueurs() {
        return joueurService.findAll();
    }

    @GetMapping("/{id}")
    public JoueurDto getJoueurById(@PathVariable Long id) {
        return joueurService.getById(id);
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public JoueurDto addJoueur(@RequestBody JoueurDto joueurDto) {
        return joueurService.addJoueur(joueurDto);
    }

    @PutMapping("/{id}")
    public JoueurDto updateJoueur(@RequestBody JoueurDto joueurDto, @PathVariable Long id) {
        return joueurService.updateJoueur(joueurDto, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJoueur(@PathVariable Long id) {
        joueurService.deleteById(id);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleJoueurNotFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }
}