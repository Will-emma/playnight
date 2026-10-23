package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DTO.FicheJeuDto;
import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.DTO.JeuMapper;
import com.takima.backskeleton.DTO.MeilleurJoueurDto;
import com.takima.backskeleton.DTO.RawgImportDto;
import com.takima.backskeleton.DTO.RawgJeuDto;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;
import com.takima.backskeleton.models.TypeJeu;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class JeuService {
    private final JeuDao jeuDao;
    private final PartieDao partieDao;
    private final RawgService rawgService;

    public JeuService(JeuDao jeuDao, PartieDao partieDao, RawgService rawgService) {
        this.jeuDao = jeuDao;
        this.partieDao = partieDao;
        this.rawgService = rawgService;
    }

    public List<JeuDto> findAll(TypeJeu type) {
        List<Jeu> jeux = type == null
                ? jeuDao.findAllByOrderByNomAsc()
                : jeuDao.findByTypeOrderByNomAsc(type);
        return jeux.stream()
                .map(JeuMapper::toDto)
                .toList();
    }

    public JeuDto getById(Long id) {
        return JeuMapper.toDto(findJeuById(id));
    }

    @Transactional(readOnly = true)
    public FicheJeuDto getFiche(Long id) {
        Jeu jeu = findJeuById(id);
        List<Partie> parties = partieDao.findDetailsByJeuId(id);
        return JeuMapper.toFicheDto(jeu, parties, classerVainqueurs(parties));
    }

    @Transactional
    public JeuDto addJeu(JeuDto jeuDto) {
        verifierDureeEstimee(jeuDto);
        Jeu jeu = JeuMapper.fromDto(jeuDto, null);
        return JeuMapper.toDto(jeuDao.save(jeu));
    }

    @Transactional
    public JeuDto updateJeu(JeuDto jeuDto, Long id) {
        findJeuById(id);
        verifierDureeEstimee(jeuDto);
        Jeu jeu = JeuMapper.fromDto(jeuDto, id);
        return JeuMapper.toDto(jeuDao.save(jeu));
    }

    public List<RawgJeuDto> rechercherSurRawg(String recherche) {
        return rawgService.rechercher(recherche);
    }

    // Ajoute à la ludothèque un jeu vidéo trouvé sur RAWG, avec le nombre de joueurs saisi par l'utilisateur
    @Transactional
    public JeuDto importerDepuisRawg(Long idRawg, RawgImportDto importDto) {
        Integer min = importDto.getNbJoueursMin();
        Integer max = importDto.getNbJoueursMax();
        // Mêmes règles que la table jeux (nb_joueurs_min >= 1, nb_joueurs_max >= nb_joueurs_min)
        if (min == null || max == null || min < 1 || max < min) {
            throw new IllegalArgumentException("Le nombre de joueurs est invalide : au moins 1, et le maximum ne peut pas être inférieur au minimum.");
        }

        RawgJeuDto jeuRawg = rawgService.getById(idRawg);
        if (jeuDao.existsByNomIgnoreCase(jeuRawg.getNom())) {
            throw new IllegalArgumentException("« " + jeuRawg.getNom() + " » est déjà dans la ludothèque.");
        }

        Jeu jeu = new Jeu.Builder()
                .nom(jeuRawg.getNom())
                .type(TypeJeu.JEU_VIDEO)
                .plateforme(jeuRawg.getPlateforme())
                .nbJoueursMin(min)
                .nbJoueursMax(max)
                .image(jeuRawg.getImage())
                .build();
        return JeuMapper.toDto(jeuDao.save(jeu));
    }

    @Transactional
    public void deleteById(Long id) {
        findJeuById(id);
        jeuDao.deleteById(id);
    }

    private void verifierDureeEstimee(JeuDto jeuDto) {
        if (jeuDto.getDureeEstimee() != null && jeuDto.getDureeEstimee() <= 0) {
            throw new IllegalArgumentException("La durée estimée doit être un nombre de minutes strictement positif.");
        }
    }

    private Jeu findJeuById(Long id) {
        return jeuDao.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Le jeu " + id + " n'existe pas"));
    }

    // Compte les victoires (scores au rang 1) de chaque joueur sur ces parties,
    // du plus grand nombre de victoires au plus petit, puis par pseudo en cas d'égalité
    private List<MeilleurJoueurDto> classerVainqueurs(List<Partie> parties) {
        Map<Joueur, Long> nbVictoires = parties.stream()
                .flatMap(partie -> partie.getScores().stream())
                .filter(Score::estVainqueur)
                .collect(Collectors.groupingBy(Score::getJoueur, Collectors.counting()));

        return nbVictoires.entrySet().stream()
                .sorted(Map.Entry.<Joueur, Long>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getPseudo()))
                .map(entry -> JeuMapper.toMeilleurJoueurDto(entry.getKey(), entry.getValue()))
                .toList();
    }
}
