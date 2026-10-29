package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.services.ImageJeuService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@CrossOrigin
@RestController
@RequestMapping("jeux")
public class ImageJeuController {
    private final ImageJeuService service;

    public ImageJeuController(ImageJeuService service) {
        this.service = service;
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public JeuDto creer(@RequestPart("jeu") JeuDto jeu, @RequestPart("fichier") MultipartFile fichier) {
        return service.creer(jeu, fichier);
    }

    @GetMapping(value = "/images/{nom}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> image(@PathVariable String nom) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(service.lire(nom));
    }
}
