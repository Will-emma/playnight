package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DTO.PointEloDto;
import com.takima.backskeleton.DTO.ProgressionEloDto;
import com.takima.backskeleton.models.Classement;
import com.takima.backskeleton.models.Partie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ProgressionEloService {
    private final JoueurDao joueurDao;
    private final PartieDao partieDao;
    private final EloService eloService;
    private final ClassementService classementService;

    public ProgressionEloService(JoueurDao joueurDao, PartieDao partieDao, EloService eloService,
                                ClassementService classementService) {
        this.joueurDao = joueurDao;
        this.partieDao = partieDao;
        this.eloService = eloService;
        this.classementService = classementService;
    }

    // La courbe et le score actuel lisent le même instantané, même si des résultats
    // sont enregistrés pendant la consultation. Aucune écriture dans classements.
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProgressionEloDto getProgression(Long joueurId) {
        if (!joueurDao.existsById(joueurId)) throw new NoSuchElementException("Le joueur " + joueurId + " n'existe pas");
        Map<Long, Map<Long, Classement>> parJeu = new LinkedHashMap<>();
        var points = new ArrayList<PointEloDto>();
        int precedent = 1000;
        int record = 1000;
        for (Partie partie : partieDao.findHistoriqueEloPourJoueur(joueurId)) {
            Map<Long, Classement> classements = parJeu.computeIfAbsent(partie.getJeu().getId(), id -> new LinkedHashMap<>());
            Classement avant = classements.get(joueurId);
            int nbAvant = avant == null ? 0 : avant.getNbParties();
            // Réutilise exactement le calcul existant sur des objets transitoires.
            eloService.appliquer(partie, classements);
            Classement apres = classements.get(joueurId);
            if (apres == null || apres.getNbParties() == nbAvant) continue;
            if (points.isEmpty()) points.add(new PointEloDto(null, null, null, 1000, 0));
            int global = moyenne(joueurId, parJeu);
            points.add(new PointEloDto(partie.getId(), partie.getSoiree().getDate(), partie.getJeu().getNom(),
                    global, global - precedent));
            precedent = global;
            record = Math.max(record, global);
        }
        return new ProgressionEloDto(classementService.getClassementGlobal(joueurId), record,
                precedent - 1000, Math.max(0, points.size() - 1), points);
    }

    private int moyenne(Long joueurId, Map<Long, Map<Long, Classement>> parJeu) {
        long somme = 0;
        int nbJeux = 0;
        for (Map<Long, Classement> classements : parJeu.values()) {
            Classement classement = classements.get(joueurId);
            if (classement != null && classement.getNbParties() > 0) {
                somme += classement.getElo();
                nbJeux++;
            }
        }
        // Même moyenne et même arrondi que ROUND(AVG(elo)) dans PostgreSQL.
        return BigDecimal.valueOf(somme).divide(BigDecimal.valueOf(nbJeux), 0, RoundingMode.HALF_UP).intValueExact();
    }
}
