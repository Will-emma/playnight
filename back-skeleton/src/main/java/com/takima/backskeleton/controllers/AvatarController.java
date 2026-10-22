package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.AvatarChoixDto;
import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.services.AvatarService;
import com.takima.backskeleton.services.AvatarStockage;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;
import java.time.Duration;

@CrossOrigin
@RestController
@RequestMapping("joueurs")
public class AvatarController {
    private final AvatarService avatarService;
    private final AvatarStockage stockage;

    public AvatarController(AvatarService avatarService, AvatarStockage stockage) {
        this.avatarService = avatarService;
        this.stockage = stockage;
    }

    @PutMapping("/{id}/avatar")
    public JoueurDto choisir(@PathVariable Long id, @RequestBody AvatarChoixDto choix) {
        return avatarService.choisir(id, choix.avatarId());
    }

    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JoueurDto importer(@PathVariable Long id, @RequestParam("fichier") MultipartFile fichier) {
        return avatarService.importer(id, fichier);
    }

    @GetMapping(value = "/avatars/{nom}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> photo(@PathVariable String nom) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(stockage.lire(nom));
    }
}
