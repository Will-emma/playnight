package com.takima.backskeleton.services;

import com.takima.backskeleton.DTO.JeuDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageJeuService {
    private final JeuService jeuService;
    private final AvatarStockage stockage;
    private final String urlApi;

    public ImageJeuService(JeuService jeuService,
                           @Value("${playnight.jeux.images.directory:./uploads/jeux}") String dossier,
                           @Value("${playnight.public-api-url:http://localhost:8080}") String urlApi) {
        this.jeuService = jeuService;
        // Même validation et réencodage que les portraits, dans un dossier indépendant.
        this.stockage = new AvatarStockage(dossier);
        this.urlApi = urlApi.replaceAll("/+$", "");
    }

    @Transactional
    public JeuDto creer(JeuDto jeu, MultipartFile fichier) {
        if (jeu == null) throw new IllegalArgumentException("Les informations du jeu sont obligatoires.");
        String nom = stockage.enregistrer(fichier);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int statut) {
                if (statut != STATUS_COMMITTED) stockage.annuler(nom);
            }
        });
        jeu.setImage(urlApi + "/jeux/images/" + nom);
        return jeuService.addJeu(jeu);
    }

    public Resource lire(String nom) {
        return stockage.lire(nom);
    }
}
