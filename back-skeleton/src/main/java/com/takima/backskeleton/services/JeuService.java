package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DTO.FicheJeuDto;
import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.DTO.JeuMapper;
import com.takima.backskeleton.DTO.MeilleurJoueurDto;
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

    public JeuService(JeuDao jeuDao, PartieDao partieDao) {
        this.jeuDao = jeuDao;
        this.partieDao = partieDao;
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
        Jeu jeu = JeuMapper.fromDto(jeuDto, null);
        return JeuMapper.toDto(jeuDao.save(jeu));
    }

    @Transactional
    public JeuDto updateJeu(JeuDto jeuDto, Long id) {
        findJeuById(id);
        Jeu jeu = JeuMapper.fromDto(jeuDto, id);
        return JeuMapper.toDto(jeuDao.save(jeu));
    }

    @Transactional
    public void deleteById(Long id) {
        findJeuById(id);
        jeuDao.deleteById(id);
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
