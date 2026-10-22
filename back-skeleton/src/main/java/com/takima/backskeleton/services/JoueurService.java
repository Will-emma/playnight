package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DAO.ScoreDao;
import com.takima.backskeleton.DTO.JeuFavoriDto;
import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.DTO.JoueurMapper;
import com.takima.backskeleton.DTO.ProfilJoueurDto;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Score;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class JoueurService {
    private final JoueurDao joueurDao;
    private final ScoreDao scoreDao;

    public JoueurService(JoueurDao joueurDao, ScoreDao scoreDao) {
        this.joueurDao = joueurDao;
        this.scoreDao = scoreDao;
    }

    public List<JoueurDto> findAll() {
        return joueurDao.findAll().stream()
                .map(JoueurMapper::toDto)
                .toList();
    }

    public JoueurDto getById(Long id) {
        return JoueurMapper.toDto(findJoueurById(id));
    }

    @Transactional(readOnly = true)
    public ProfilJoueurDto getProfil(Long id) {
        Joueur joueur = findJoueurById(id);
        // Un score par partie jouée, déjà triés du plus récent au plus ancien
        List<Score> scores = scoreDao.findDetailsByJoueurId(id);
        long nbVictoires = scores.stream()
                .filter(Score::estVainqueur)
                .count();
        return JoueurMapper.toProfilDto(joueur, scores, nbVictoires, classerJeuxFavoris(scores));
    }

    @Transactional
    public JoueurDto addJoueur(JoueurDto joueurDto) {
        // Sans avatar fourni, le joueur reçoit un portrait de la galerie
        if (joueurDto.getAvatar() == null || joueurDto.getAvatar().isBlank()) {
            joueurDto.setAvatar(avatarParDefaut());
        }
        Joueur joueur = JoueurMapper.fromDto(joueurDto, null);
        return JoueurMapper.toDto(joueurDao.save(joueur));
    }

    @Transactional
    public JoueurDto updateJoueur(JoueurDto joueurDto, Long id) {
        findJoueurById(id);
        Joueur joueur = JoueurMapper.fromDto(joueurDto, id);
        return JoueurMapper.toDto(joueurDao.save(joueur));
    }

    @Transactional
    public void deleteById(Long id) {
        findJoueurById(id);
        joueurDao.deleteById(id);
    }

    // Avatar de la galerie le moins porté (le plus petit numéro en cas d'égalité) : les joueurs gardent des portraits différents
    // tant que c'est possible. Même règle que le choix proposé par le formulaire d'ajout du front (avatarConseille)
    private String avatarParDefaut() {
        Map<String, Long> nbPorteurs = joueurDao.findAll().stream()
                .filter(joueur -> joueur.getAvatar() != null)
                .collect(Collectors.groupingBy(Joueur::getAvatar, Collectors.counting()));
        int moinsPorte = 1;
        for (int avatarId = 2; avatarId <= AvatarService.NB_AVATARS_GALERIE; avatarId++) {
            if (nbPorteurs.getOrDefault(AvatarService.urlGalerie(avatarId), 0L)
                    < nbPorteurs.getOrDefault(AvatarService.urlGalerie(moinsPorte), 0L)) {
                moinsPorte = avatarId;
            }
        }
        return AvatarService.urlGalerie(moinsPorte);
    }

    private Joueur findJoueurById(Long id) {
        return joueurDao.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Le joueur " + id + " n'existe pas"));
    }

    // Compte les parties jouées sur chaque jeu,
    // du jeu le plus joué au moins joué, puis par nom en cas d'égalité
    private List<JeuFavoriDto> classerJeuxFavoris(List<Score> scores) {
        Map<Jeu, Long> nbPartiesParJeu = scores.stream()
                .collect(Collectors.groupingBy(score -> score.getPartie().getJeu(), Collectors.counting()));

        return nbPartiesParJeu.entrySet().stream()
                .sorted(Map.Entry.<Jeu, Long>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getNom()))
                .map(entry -> JoueurMapper.toJeuFavoriDto(entry.getKey(), entry.getValue()))
                .toList();
    }
}
