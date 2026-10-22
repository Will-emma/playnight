package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.DTO.JoueurMapper;
import com.takima.backskeleton.models.Joueur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.NoSuchElementException;

@Service
public class AvatarService {
    private final JoueurDao joueurDao;
    private final AvatarStockage stockage;
    private final String urlApi;

    public AvatarService(JoueurDao joueurDao, AvatarStockage stockage,
                         @Value("${playnight.public-api-url:http://localhost:8080}") String urlApi) {
        this.joueurDao = joueurDao;
        this.stockage = stockage;
        this.urlApi = urlApi.replaceAll("/+$", "");
    }

    @Transactional
    public JoueurDto choisir(Long joueurId, Integer avatarId) {
        Joueur joueur = trouver(joueurId);
        if (avatarId == null || avatarId < 1 || avatarId > NB_AVATARS_GALERIE) {
            throw new IllegalArgumentException("Choisissez un avatar de la galerie.");
        }
        joueur.modifierAvatar(urlGalerie(avatarId));
        joueurDao.flush();
        return JoueurMapper.toDto(joueur);
    }

    @Transactional
    public JoueurDto importer(Long joueurId, MultipartFile fichier) {
        Joueur joueur = trouver(joueurId);
        String nom = stockage.enregistrer(fichier);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int statut) {
                if (statut != STATUS_COMMITTED) stockage.annuler(nom);
            }
        });
        joueur.modifierAvatar(urlApi + "/joueurs/avatars/" + nom);
        joueurDao.flush();
        return JoueurMapper.toDto(joueur);
    }

    // Les 12 avatars de la galerie sont servis par le front (src/assets/avatars, liste dans utils/avatar.ts)
    public static final int NB_AVATARS_GALERIE = 12;

    public static String urlGalerie(int avatarId) {
        return String.format("/assets/avatars/avatar-%02d.png", avatarId);
    }

    private Joueur trouver(Long id) {
        return joueurDao.findById(id).orElseThrow(() -> new NoSuchElementException("Le joueur " + id + " n'existe pas"));
    }
}
