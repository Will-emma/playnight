package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DAO.ScoreDao;
import com.takima.backskeleton.DAO.SoireeDao;
import com.takima.backskeleton.DAO.SoireePartieCount;
import com.takima.backskeleton.DTO.PartieCreationDto;
import com.takima.backskeleton.DTO.ScoreCreationDto;
import com.takima.backskeleton.DTO.SoireeCreationDto;
import com.takima.backskeleton.DTO.SoireeDetailDto;
import com.takima.backskeleton.DTO.SoireeDto;
import com.takima.backskeleton.DTO.SoireeMapper;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;
import com.takima.backskeleton.models.Soiree;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SoireeService {
    private final SoireeDao soireeDao;
    private final PartieDao partieDao;
    private final ScoreDao scoreDao;
    private final JoueurDao joueurDao;
    private final JeuDao jeuDao;

    public SoireeService(SoireeDao soireeDao, PartieDao partieDao, ScoreDao scoreDao,
                         JoueurDao joueurDao, JeuDao jeuDao) {
        this.soireeDao = soireeDao;
        this.partieDao = partieDao;
        this.scoreDao = scoreDao;
        this.joueurDao = joueurDao;
        this.jeuDao = jeuDao;
    }

    @Transactional(readOnly = true)
    public List<SoireeDto> findAll() {
        List<Soiree> soirees = soireeDao.findAllWithJoueurs();
        Map<Long, Long> nbPartiesParSoiree = soireeDao.countPartiesBySoiree().stream()
                .collect(Collectors.toMap(SoireePartieCount::getSoireeId, SoireePartieCount::getNbParties));

        return soirees.stream()
                .map(soiree -> SoireeMapper.toDto(
                        soiree,
                        nbPartiesParSoiree.getOrDefault(soiree.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SoireeDetailDto getById(Long id) {
        Soiree soiree = findSoireeById(id);
        return SoireeMapper.toDetailDto(soiree, partieDao.findDetailsBySoireeId(id));
    }

    // Crée toute la soirée (présents, parties, scores) en une seule transaction :
    // si une étape échoue, rien n'est enregistré en base.
    @Transactional
    public SoireeDetailDto creerSoiree(SoireeCreationDto dto) {
        // 0. On vérifie les règles métier avant d'enregistrer quoi que ce soit
        verifierSoiree(dto);

        // 1. La soirée et ses joueurs présents
        Soiree soiree = new Soiree.Builder()
                .date(dto.getDate())
                .lieu(dto.getLieu())
                .joueurs(findJoueursByIds(dto.getJoueurIds()))
                .build();
        soireeDao.save(soiree);

        // 2. Chaque partie, puis ses scores
        enregistrerParties(soiree, dto.getParties());

        // 3. On renvoie la soirée au format du détail
        return SoireeMapper.toDetailDto(soiree, soiree.getParties());
    }

    // Modifie une soirée existante avec le même format que la création.
    // Plus simple que de comparer l'ancien et le nouveau contenu : on supprime les anciennes
    // parties (la base supprime leurs scores, grâce au "on delete cascade") et on recrée les nouvelles.
    @Transactional
    public SoireeDetailDto modifierSoiree(Long id, SoireeCreationDto dto) {
        Soiree soiree = findSoireeById(id); // 404 si la soirée n'existe pas
        verifierSoiree(dto);

        partieDao.deleteBySoireeId(id);
        soiree.modifier(dto.getDate(), dto.getLieu(), findJoueursByIds(dto.getJoueurIds()));
        enregistrerParties(soiree, dto.getParties());

        return SoireeMapper.toDetailDto(soiree, soiree.getParties());
    }

    @Transactional
    public void deleteById(Long id) {
        soireeDao.delete(findSoireeById(id));
    }

    // Enregistre chaque partie de la soirée, puis ses scores (utilisé par la création et la modification)
    private void enregistrerParties(Soiree soiree, List<PartieCreationDto> partiesDto) {
        for (PartieCreationDto partieDto : partiesDto) {
            Jeu jeu = findJeuById(partieDto.getJeuId());
            verifierNbJoueurs(jeu, partieDto.getScores().size());

            Partie partie = new Partie.Builder()
                    .duree(partieDto.getDuree())
                    .jeu(jeu)
                    .soiree(soiree)
                    .build();
            partieDao.save(partie);
            soiree.getParties().add(partie);

            for (ScoreCreationDto scoreDto : partieDto.getScores()) {
                Score score = new Score.Builder()
                        .points(scoreDto.getPoints())
                        .rang(scoreDto.getRang())
                        .numeroEquipe(scoreDto.getNumeroEquipe())
                        .joueur(findJoueurById(scoreDto.getJoueurId()))
                        .partie(partie)
                        .build();
                scoreDao.save(score);
                partie.getScores().add(score);
            }
        }
    }

    // Règles 1 à 3 : on regarde seulement le contenu de la requête, sans aller en base
    private void verifierSoiree(SoireeCreationDto dto) {
        // Champs obligatoires (sinon la base refuserait l'enregistrement)
        if (dto.getDate() == null || dto.getLieu() == null || dto.getLieu().isBlank()) {
            throw new IllegalArgumentException("La date et le lieu de la soirée sont obligatoires");
        }
        // Les listes doivent être envoyées, même vides ([])
        if (dto.getParties() == null || dto.getParties().stream().anyMatch(partie -> partie.getScores() == null)) {
            throw new IllegalArgumentException("La liste des parties et celle des scores de chaque partie sont obligatoires");
        }

        // Règle 1 : au moins un joueur présent
        if (dto.getJoueurIds() == null || dto.getJoueurIds().isEmpty()) {
            throw new IllegalArgumentException("Une soirée doit avoir au moins un joueur présent");
        }

        for (PartieCreationDto partieDto : dto.getParties()) {
            // Champ obligatoire en base, avec "duree > 0"
            if (partieDto.getDuree() == null || partieDto.getDuree() <= 0) {
                throw new IllegalArgumentException("La durée de chaque partie est obligatoire et doit être positive");
            }
            Set<Long> joueursDeLaPartie = new HashSet<>();

            for (ScoreCreationDto scoreDto : partieDto.getScores()) {
                Long joueurId = scoreDto.getJoueurId();

                // Champ obligatoire en base
                if (scoreDto.getRang() == null) {
                    throw new IllegalArgumentException("Le rang de chaque score est obligatoire");
                }

                // Règle 2 : le joueur du score doit faire partie des présents
                if (!dto.getJoueurIds().contains(joueurId)) {
                    throw new IllegalArgumentException("Le joueur " + joueurId + " n'est pas présent à la soirée");
                }

                // Règle 3 : un seul score par joueur dans une partie
                // (add renvoie false si le joueur était déjà dans l'ensemble)
                if (!joueursDeLaPartie.add(joueurId)) {
                    throw new IllegalArgumentException("Le joueur " + joueurId + " a plusieurs scores dans la même partie");
                }
            }
        }
    }

    // Règle 4 : le nombre de joueurs de la partie doit être autorisé par le jeu
    private void verifierNbJoueurs(Jeu jeu, int nbJoueurs) {
        if (nbJoueurs < jeu.getNbJoueursMin() || nbJoueurs > jeu.getNbJoueursMax()) {
            throw new IllegalArgumentException("Le jeu " + jeu.getNom() + " se joue de "
                    + jeu.getNbJoueursMin() + " à " + jeu.getNbJoueursMax() + " joueurs, pas " + nbJoueurs);
        }
    }

    private Soiree findSoireeById(Long id) {
        return soireeDao.findWithJoueursById(id)
                .orElseThrow(() -> new NoSuchElementException("La soirée " + id + " n'existe pas"));
    }

    private List<Joueur> findJoueursByIds(List<Long> ids) {
        return ids.stream()
                .map(this::findJoueurById)
                .toList();
    }

    private Joueur findJoueurById(Long id) {
        return joueurDao.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Le joueur " + id + " n'existe pas"));
    }

    private Jeu findJeuById(Long id) {
        return jeuDao.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Le jeu " + id + " n'existe pas"));
    }
}
