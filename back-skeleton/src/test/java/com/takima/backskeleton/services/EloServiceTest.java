package com.takima.backskeleton.services;

import com.takima.backskeleton.models.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EloServiceTest {
    private final EloService service = new EloService();
    private final Jeu jeu = new Jeu.Builder().id(1L).build();
    private final Map<Long, Classement> classement = new HashMap<>();

    @Test
    void initialisation() {
        Classement c = new Classement(joueur(1), jeu);
        assertEquals(1000, c.getElo());
        assertEquals(0, c.getNbParties());
    }

    @Test
    void victoireEtDefaite() {
        jouer(score(1, 1, null), score(2, 2, null));
        verifier(1, 1016, 1);
        verifier(2, 984, 1);
    }

    @Test
    void egalite() {
        jouer(score(1, 1, null), score(2, 1, null));
        verifier(1, 1000, 1);
        verifier(2, 1000, 1);
    }

    @Test
    void egaliteEntreElosDifferents() {
        jouer(score(1, 1, null), score(2, 2, null));
        jouer(score(1, 1, null), score(2, 1, null));
        verifier(1, 1015, 2);
        verifier(2, 985, 2);
    }

    @Test
    void multijoueur() {
        jouer(score(1, 1, null), score(2, 2, null), score(3, 3, null));
        verifier(1, 1016, 1);
        verifier(2, 1000, 1);
        verifier(3, 984, 1);
    }

    @Test
    void multijoueurAvecExAequo() {
        jouer(score(1, 1, null), score(2, 1, null), score(3, 3, null));
        verifier(1, 1008, 1);
        verifier(2, 1008, 1);
        verifier(3, 984, 1);
    }

    @Test
    void equipesEtCompteurUnique() {
        jouer(score(1, 1, 10), score(2, 1, 10), score(3, 2, 20), score(4, 2, 20));
        verifier(1, 1016, 1);
        verifier(2, 1016, 1);
        verifier(3, 984, 1);
        verifier(4, 984, 1);
    }

    @Test
    void equipesInegalesEtTroisEquipes() {
        jouer(score(1, 1, 10), score(2, 1, 10), score(3, 2, 20), score(4, 3, 30));
        verifier(1, 1016, 1);
        verifier(2, 1016, 1);
        verifier(3, 1000, 1);
        verifier(4, 984, 1);
    }

    @Test
    void moyenneEquipeEtEgalite() {
        Classement fort = new Classement(joueur(1), jeu);
        fort.enregistrerPartie(1200);
        classement.put(1L, fort);
        jouer(score(1, 1, 10), score(2, 1, 10), score(3, 1, 20));
        verifier(1, 1196, 2);
        verifier(2, 996, 1);
        verifier(3, 1004, 1);
    }

    @Test
    void ordreDesScoresSansInfluence() {
        List<Score> scores = List.of(score(1, 1, null), score(2, 2, null), score(3, 3, null));
        jouer(scores.toArray(Score[]::new));
        Map<Long, Classement> inverse = new HashMap<>();
        Partie partie = partie(score(3, 3, null), score(2, 2, null), score(1, 1, null));
        service.appliquer(partie, inverse);
        classement.forEach((id, c) -> assertEquals(c.getElo(), inverse.get(id).getElo()));
    }

    @Test
    void aucunResultatExploitable() {
        jouer();
        jouer(score(1, 1, null));
        jouer(score(1, 1, 10), score(2, 1, 10));
        jouer(score(1, null, null), score(2, 2, null));
        jouer(score(1, 0, null), score(2, 2, null));
        jouer(score(1, 1, null), score(1, 2, null));
        jouer(score(1, 1, 10), score(2, 2, 10), score(3, 3, 20));
        jouer(score(1, 1, 10), score(2, 2, null));
        assertTrue(classement.isEmpty());
    }

    private Joueur joueur(long id) { return new Joueur.Builder().id(id).pseudo("J" + id).build(); }
    private Score score(long id, Integer rang, Integer equipe) {
        return new Score.Builder().joueur(joueur(id)).rang(rang).numeroEquipe(equipe).build();
    }
    private Partie partie(Score... scores) {
        Partie partie = new Partie.Builder().jeu(jeu).build();
        partie.getScores().addAll(List.of(scores));
        return partie;
    }
    private void jouer(Score... scores) { service.appliquer(partie(scores), classement); }
    private void verifier(long id, int elo, int nb) {
        assertEquals(elo, classement.get(id).getElo());
        assertEquals(nb, classement.get(id).getNbParties());
    }
}
