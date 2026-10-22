package com.takima.backskeleton.services;

import com.takima.backskeleton.models.Classement;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EloService {
    public static final int K = 32;

    // La map contient uniquement les joueurs de ce jeu. Voir docs/modele-donnees.md.
    public void appliquer(Partie partie, Map<Long, Classement> classements) {
        List<Score> scores = partie.getScores();
        if (scores == null || scores.size() < 2) return;
        Set<Long> joueurs = new HashSet<>();
        boolean enEquipe = scores.stream().anyMatch(s -> s.getNumeroEquipe() != null);
        Map<Long, List<Score>> groupes = new LinkedHashMap<>();
        for (Score score : scores) {
            if (score.getJoueur() == null || score.getJoueur().getId() == null
                    || score.getRang() == null || score.getRang() < 1
                    || !joueurs.add(score.getJoueur().getId())
                    || (enEquipe && score.getNumeroEquipe() == null)) return;
            long cle = enEquipe ? score.getNumeroEquipe().longValue() : score.getJoueur().getId();
            groupes.computeIfAbsent(cle, k -> new ArrayList<>()).add(score);
        }
        List<List<Score>> adversaires = new ArrayList<>(groupes.values());
        if (adversaires.size() < 2 || adversaires.stream().anyMatch(equipe ->
                equipe.stream().anyMatch(s -> !s.getRang().equals(equipe.get(0).getRang())))) return;

        // Tous les Elo attendus sont figés avant la moindre mise à jour.
        double[] elos = adversaires.stream().mapToDouble(equipe -> equipe.stream()
                .mapToInt(s -> classements.computeIfAbsent(s.getJoueur().getId(),
                        id -> new Classement(s.getJoueur(), partie.getJeu())).getElo())
                .average().orElseThrow()).toArray();
        for (int i = 0; i < adversaires.size(); i++) {
            double variation = 0;
            int rang = adversaires.get(i).get(0).getRang();
            for (int j = 0; j < adversaires.size(); j++) {
                if (i == j) continue;
                int autreRang = adversaires.get(j).get(0).getRang();
                double resultat = rang < autreRang ? 1 : rang == autreRang ? 0.5 : 0;
                double attendu = 1 / (1 + Math.pow(10, (elos[j] - elos[i]) / 400));
                variation += resultat - attendu;
            }
            // Même variation pour tous les membres ; arrondi symétrique à l'entier le plus proche.
            double delta = K * variation / (adversaires.size() - 1);
            int arrondi = (int) (Math.signum(delta) * Math.floor(Math.abs(delta) + 0.5));
            for (Score score : adversaires.get(i)) {
                Classement classement = classements.get(score.getJoueur().getId());
                classement.enregistrerPartie(classement.getElo() + arrondi);
            }
        }
    }
}
