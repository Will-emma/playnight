package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.ClassementDao;
import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DTO.ClassementJoueurDto;
import com.takima.backskeleton.DTO.ClassementGlobalDto;
import com.takima.backskeleton.DTO.JeuResumeDto;
import com.takima.backskeleton.models.TypeJeu;
import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DTO.ClassementDto;
import com.takima.backskeleton.models.Classement;
import com.takima.backskeleton.models.Partie;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ClassementService {
    private final ClassementDao classementDao;
    private final JeuDao jeuDao;
    private final JoueurDao joueurDao;
    private final PartieDao partieDao;
    private final EloService eloService;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    public ClassementService(ClassementDao classementDao, JeuDao jeuDao, PartieDao partieDao,
                             EloService eloService, EntityManager entityManager, JdbcTemplate jdbcTemplate, JoueurDao joueurDao) {
        this.classementDao = classementDao;
        this.jeuDao = jeuDao;
        this.joueurDao = joueurDao;
        this.partieDao = partieDao;
        this.eloService = eloService;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    // Verrou partagé par toutes les écritures de résultats, conservé jusqu'au commit/rollback.
    @Transactional(propagation = Propagation.MANDATORY)
    public void verrouillerHistorique() {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(240024)", rs -> { return null; });
    }

    @Transactional
    public void recalculer() {
        verrouillerHistorique();
        entityManager.flush();
        // Écarte les collections de scores en cache après la suppression/recréation d'une soirée.
        entityManager.clear();
        Map<Long, Map<Long, Classement>> parJeu = new LinkedHashMap<>();
        for (Partie partie : partieDao.findHistoriqueElo()) {
            eloService.appliquer(partie, parJeu.computeIfAbsent(partie.getJeu().getId(), id -> new LinkedHashMap<>()));
        }
        classementDao.deleteAllInBatch();
        classementDao.saveAll(parJeu.values().stream().flatMap(m -> m.values().stream()).toList());
    }

    @Transactional(readOnly = true)
    public ClassementGlobalDto getClassementGlobal(Long joueurId) {
        if (!joueurDao.existsById(joueurId)) throw new NoSuchElementException("Le joueur " + joueurId + " n'existe pas");
        return classementDao.findClassementGlobal(joueurId)
                .map(c -> new ClassementGlobalDto(c.getElo(), Math.toIntExact(c.getRang())))
                .orElseGet(() -> new ClassementGlobalDto(1000, null));
    }

    @Transactional(readOnly = true)
    public List<ClassementJoueurDto> getClassementsJoueur(Long joueurId) {
        if (!joueurDao.existsById(joueurId)) throw new NoSuchElementException("Le joueur " + joueurId + " n'existe pas");
        return classementDao.findClassementsByJoueurId(joueurId).stream().map(c -> new ClassementJoueurDto(
                new JeuResumeDto(c.getJeuId(), c.getNom(), TypeJeu.valueOf(c.getType()), c.getImage()),
                c.getElo(), Math.toIntExact(c.getRang()), c.getNbParties())).toList();
    }

    @Transactional(readOnly = true)
    public List<ClassementDto> getClassement(Long jeuId) {
        if (!jeuDao.existsById(jeuId)) throw new NoSuchElementException("Le jeu " + jeuId + " n'existe pas");
        List<Classement> classements = classementDao.findClassementByJeuId(jeuId);
        List<ClassementDto> resultat = new ArrayList<>();
        int rang = 0;
        Integer precedent = null;
        for (int i = 0; i < classements.size(); i++) {
            Classement c = classements.get(i);
            if (!c.getElo().equals(precedent)) rang = i + 1;
            resultat.add(new ClassementDto(c.getJoueur().getId(), c.getJoueur().getPseudo(),
                    c.getJoueur().getAvatar(), c.getElo(), c.getNbParties(), rang));
            precedent = c.getElo();
        }
        return resultat;
    }
}
