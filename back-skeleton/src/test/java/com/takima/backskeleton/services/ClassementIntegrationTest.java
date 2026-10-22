package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.*;
import com.takima.backskeleton.DTO.*;
import com.takima.backskeleton.models.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.transaction.TestTransaction;
import javax.sql.DataSource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// Vraie base PostgreSQL, schéma initdb. Toutes les données de chaque test sont annulées.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClassementIntegrationTest {
    @Autowired private SoireeService soireeService;
    @Autowired private ClassementService classementService;
    @Autowired private ProgressionEloService progressionEloService;
    @Autowired private JeuDao jeuDao;
    @Autowired private JoueurDao joueurDao;
    @Autowired private SoireeDao soireeDao;
    @Autowired private PartieDao partieDao;
    @Autowired private EntityManager em;
    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;
    private Joueur a;
    private Joueur b;
    private Jeu jeu;
    private Jeu autreJeu;

    @BeforeEach
    void preparer() {
        a = joueurDao.save(new Joueur.Builder().pseudo("Elo A " + UUID.randomUUID()).avatar("avatar.png").build());
        b = joueurDao.save(new Joueur.Builder().pseudo("Elo B " + UUID.randomUUID()).build());
        jeu = jeuDao.save(jeu("Test Elo"));
        autreJeu = jeuDao.save(jeu("Autre jeu Elo"));
    }

    @Test
    void creationEtIndependanceParJeu() {
        soireeService.creerSoiree(dto(jeu, false, 1));
        soireeService.creerSoiree(dto(autreJeu, true, 2));
        verifier(jeu, a, 1016, 1);
        verifier(jeu, b, 984, 1);
        verifier(autreJeu, a, 984, 1);
        verifier(autreJeu, b, 1016, 1);
    }

    @Test
    void modificationInverseLesResultatsSansDoubleComptage() {
        Long id = soireeService.creerSoiree(dto(jeu, false, 1)).getId();
        // Charge volontairement les anciens scores dans le contexte JPA.
        soireeService.getById(id);
        soireeService.modifierSoiree(id, dto(jeu, true, 1));
        verifier(jeu, a, 984, 1);
        verifier(jeu, b, 1016, 1);
        classementService.recalculer();
        verifier(jeu, a, 984, 1);
    }

    @Test
    void modificationChangeDeJeuEtRetireAncienClassement() {
        Long id = soireeService.creerSoiree(dto(jeu, false, 1)).getId();
        soireeService.modifierSoiree(id, dto(autreJeu, false, 1));
        assertTrue(classementService.getClassement(jeu.getId()).isEmpty());
        verifier(autreJeu, a, 1016, 1);
    }

    @Test
    void suppressionRejoueLesPartiesSuivantes() {
        Long premiere = soireeService.creerSoiree(dto(jeu, false, 1)).getId();
        Long seconde = soireeService.creerSoiree(dto(jeu, false, 2)).getId();
        verifier(jeu, a, 1031, 2);
        soireeService.getById(premiere);
        soireeService.deleteById(premiere);
        verifier(jeu, a, 1016, 1);
        soireeService.deleteById(seconde);
        assertTrue(classementService.getClassement(jeu.getId()).isEmpty());
    }

    @Test
    void retraitDeToutesLesParties() {
        Long id = soireeService.creerSoiree(dto(jeu, false, 1)).getId();
        SoireeCreationDto vide = dto(jeu, false, 1);
        vide.setParties(List.of());
        soireeService.modifierSoiree(id, vide);
        assertTrue(classementService.getClassement(jeu.getId()).isEmpty());
    }

    @Test
    void ordreChronologiquePuisSoireeStableApresModification() {
        // Insère la seconde soirée en premier ; la date prime sur l'id de partie.
        Long seconde = soireeService.creerSoiree(dto(jeu, true, 2)).getId();
        Long premiere = soireeService.creerSoiree(dto(jeu, false, 1)).getId();
        verifier(jeu, a, 999, 2);
        soireeService.modifierSoiree(premiere, dto(jeu, false, 1));
        verifier(jeu, a, 999, 2);
        // À date égale, l'id de soirée prime même après recréation de ses parties.
        soireeService.modifierSoiree(seconde, dto(jeu, true, 1));
        verifier(jeu, a, 1001, 2);
        soireeService.modifierSoiree(seconde, dto(jeu, true, 1));
        verifier(jeu, a, 1001, 2);
    }

    @Test
    void egalitesRangEtOrdreDeterministe() {
        SoireeCreationDto egalite = dto(jeu, false, 1);
        egalite.getParties().get(0).getScores().forEach(s -> s.setRang(1));
        soireeService.creerSoiree(egalite);
        List<ClassementDto> resultat = classementService.getClassement(jeu.getId());
        assertEquals(2, resultat.size());
        assertEquals(a.getId(), resultat.get(0).joueurId());
        assertTrue(resultat.stream().allMatch(c -> c.rang() == 1 && c.elo() == 1000 && c.nbParties() == 1));
    }

    @Test
    void jeuSansPartieEtJeuAbsent() {
        assertTrue(classementService.getClassement(jeu.getId()).isEmpty());
        assertThrows(NoSuchElementException.class, () -> classementService.getClassement(-1L));
    }

    @Test
    void repriseHistoriqueAuDemarrageEtRecalculIdempotent() {
        // Résultats historiques écrits sans passer par le service de soirées.
        Soiree s = soireeDao.save(new Soiree.Builder().date(LocalDateTime.of(2026, 1, 1, 20, 0))
                .lieu("Historique").joueurs(List.of(a, b)).build());
        Partie p = partieDao.save(new Partie.Builder().soiree(s).jeu(jeu).duree(20).build());
        em.persist(new Score.Builder().partie(p).joueur(a).rang(1).build());
        em.persist(new Score.Builder().partie(p).joueur(b).rang(2).build());
        new ClassementInitialisation(classementService).run(null);
        verifier(jeu, a, 1016, 1);
        classementService.recalculer();
        verifier(jeu, a, 1016, 1);
    }

    @Test
    void endpointAvecVraisResultatsPostgresql() throws Exception {
        soireeService.creerSoiree(dto(jeu, false, 1));
        mockMvc.perform(get("/jeux/" + jeu.getId() + "/classement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].joueurId").value(a.getId()))
                .andExpect(jsonPath("$[0].elo").value(1016))
                .andExpect(jsonPath("$[0].avatar").value("avatar.png"))
                .andExpect(jsonPath("$[1].elo").value(984));
    }

    @Test
    void plusieursPartiesDansUneSoiree() {
        SoireeCreationDto dto = dto(jeu, false, 1);
        dto.setParties(List.of(dto.getParties().get(0), dto(jeu, false, 1).getParties().get(0)));
        Long id = soireeService.creerSoiree(dto).getId();
        verifier(jeu, a, 1031, 2);
        soireeService.modifierSoiree(id, dto);
        verifier(jeu, a, 1031, 2);
    }

    @Test
    void resultatsEquipeViaServiceDeSoiree() {
        Joueur c = joueurDao.save(new Joueur.Builder().pseudo("Elo C " + UUID.randomUUID()).build());
        SoireeCreationDto dto = dto(jeu, false, 1);
        dto.setJoueurIds(List.of(a.getId(), b.getId(), c.getId()));
        List<ScoreCreationDto> scores = new ArrayList<>(dto.getParties().get(0).getScores());
        scores.get(0).setNumeroEquipe(1);
        scores.get(1).setNumeroEquipe(2);
        ScoreCreationDto sc = new ScoreCreationDto();
        sc.setJoueurId(c.getId()); sc.setRang(1); sc.setNumeroEquipe(1);
        scores.add(sc);
        dto.getParties().get(0).setScores(scores);
        soireeService.creerSoiree(dto);
        verifier(jeu, a, 1016, 1);
        verifier(jeu, c, 1016, 1);
        verifier(jeu, b, 984, 1);
    }

    @Test
    void aucunClassementPourUnePartieSolo() {
        SoireeCreationDto dto = dto(jeu, false, 1);
        dto.getParties().get(0).setScores(List.of(dto.getParties().get(0).getScores().get(0)));
        soireeService.creerSoiree(dto);
        assertTrue(classementService.getClassement(jeu.getId()).isEmpty());
    }

    @Test
    void erreurSurLaDeuxiemePartieAnnuleTouteLaSoiree() {
        long avant = soireeDao.count();
        SoireeCreationDto dto = dto(jeu, false, 1);
        PartieCreationDto inconnue = dto(jeu, false, 1).getParties().get(0);
        inconnue.setJeuId(-1L);
        dto.setParties(List.of(dto.getParties().get(0), inconnue));
        assertThrows(NoSuchElementException.class, () -> soireeService.creerSoiree(dto));
        // Le proxy transactionnel a marqué la transaction pour rollback malgré
        // l'enregistrement de la première partie avant l'erreur.
        TestTransaction.end();
        TestTransaction.start();
        assertEquals(avant, soireeDao.count());
        assertFalse(jeuDao.existsById(jeu.getId()));
    }

    @Test
    void verrouConserveJusquaLaFinDeTransaction() throws Exception {
        soireeService.creerSoiree(dto(jeu, false, 1));
        // Une autre connexion représente une seconde instance de l'API.
        try (var connexion = dataSource.getConnection()) {
            connexion.setAutoCommit(false);
            try (var requete = connexion.prepareStatement("SELECT pg_try_advisory_xact_lock(240024)")) {
                try (var resultat = requete.executeQuery()) {
                    assertTrue(resultat.next());
                    assertFalse(resultat.getBoolean(1));
                }
                TestTransaction.end();
                try (var resultat = requete.executeQuery()) {
                    assertTrue(resultat.next());
                    assertTrue(resultat.getBoolean(1));
                }
            } finally {
                connexion.rollback();
            }
        } finally {
            if (!TestTransaction.isActive()) TestTransaction.start();
        }
    }

    @Test
    void classementPersonnelCorrespondExactementAuxClassementsDesJeux() throws Exception {
        soireeService.creerSoiree(dto(jeu, false, 1));
        soireeService.creerSoiree(dto(autreJeu, true, 2));
        List<ClassementJoueurDto> personnels = classementService.getClassementsJoueur(a.getId());
        assertEquals(2, personnels.size());
        for (ClassementJoueurDto personnel : personnels) {
            ClassementDto dansJeu = classementService.getClassement(personnel.jeu().getId()).stream()
                    .filter(c -> c.joueurId().equals(a.getId())).findFirst().orElseThrow();
            assertEquals(dansJeu.elo(), personnel.elo());
            assertEquals(dansJeu.rang(), personnel.rang());
            assertEquals(dansJeu.nbParties(), personnel.nbParties());
        }
        mockMvc.perform(get("/joueurs/" + a.getId() + "/classements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jeu.id").value(autreJeu.getId()))
                .andExpect(jsonPath("$[0].jeu.nom").value("Autre jeu Elo"))
                .andExpect(jsonPath("$[0].jeu.type").value("SOCIETE"))
                .andExpect(jsonPath("$[0].elo").value(984))
                .andExpect(jsonPath("$[0].rang").value(2))
                .andExpect(jsonPath("$[0].nbParties").value(1));
    }

    @Test
    void rangPersonnelInclutTousLesAdversairesEtLesExAequo() {
        Joueur c = joueurDao.save(new Joueur.Builder().pseudo("Elo C " + UUID.randomUUID()).build());
        Joueur d = joueurDao.save(new Joueur.Builder().pseudo("Elo D " + UUID.randomUUID()).build());
        List<Joueur> joueurs = List.of(a, b, c, d);
        int[] elos = {1000, 1200, 1100, 1100};
        for (int i = 0; i < joueurs.size(); i++) {
            Classement classement = new Classement(joueurs.get(i), jeu);
            classement.enregistrerPartie(elos[i]);
            em.persist(classement);
        }
        em.flush();
        assertEquals(4, classementService.getClassementsJoueur(a.getId()).get(0).rang());
        assertEquals(2, classementService.getClassementsJoueur(c.getId()).get(0).rang());
        assertEquals(2, classementService.getClassementsJoueur(d.getId()).get(0).rang());
    }

    @Test
    void endpointPersonnelSansClassementOuJoueurAbsent() throws Exception {
        mockMvc.perform(get("/joueurs/" + a.getId() + "/classements"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/joueurs/-1/classements"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Le joueur -1 n'existe pas"));
    }

    @Test
    void classementPersonnelSuitLesResultatsAjoutesModifiesEtSupprimes() {
        Long id = soireeService.creerSoiree(dto(jeu, false, 1)).getId();
        assertEquals(1016, classementService.getClassementsJoueur(a.getId()).get(0).elo());
        soireeService.modifierSoiree(id, dto(jeu, true, 1));
        assertEquals(984, classementService.getClassementsJoueur(a.getId()).get(0).elo());
        soireeService.deleteById(id);
        assertTrue(classementService.getClassementsJoueur(a.getId()).isEmpty());
    }

    @Test
    void globalEstLaMoyenneParJeuEtRespecteLesExAequo() throws Exception {
        // Projection de test isolée, annulée avec toute la transaction en fin de test.
        em.createQuery("delete from Classement").executeUpdate();
        Joueur c = joueurDao.save(new Joueur.Builder().pseudo("Global C " + UUID.randomUUID()).build());
        Joueur d = joueurDao.save(new Joueur.Builder().pseudo("Global D " + UUID.randomUUID()).build());
        classer(a, jeu, 1001, 1);
        classer(a, autreJeu, 1002, 10);
        classer(b, jeu, 1002, 1);
        classer(c, jeu, 1100, 1);
        classer(d, jeu, 999, 1);
        em.flush();
        // 1001,5 -> 1002, sans pondérer par les dix parties du second jeu.
        assertEquals(1002, classementService.getClassementGlobal(a.getId()).elo());
        assertEquals(2, classementService.getClassementGlobal(a.getId()).rang());
        assertEquals(2, classementService.getClassementGlobal(b.getId()).rang());
        assertEquals(4, classementService.getClassementGlobal(d.getId()).rang());
        mockMvc.perform(get("/joueurs/" + a.getId() + "/classement-global"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elo").value(1002))
                .andExpect(jsonPath("$.rang").value(2));
    }

    @Test
    void globalSansResultatEtJoueurInexistant() throws Exception {
        assertEquals(1000, classementService.getClassementGlobal(a.getId()).elo());
        assertNull(classementService.getClassementGlobal(a.getId()).rang());
        mockMvc.perform(get("/joueurs/" + a.getId() + "/classement-global"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elo").value(1000))
                .andExpect(jsonPath("$.rang").isEmpty());
        mockMvc.perform(get("/joueurs/-1/classement-global")).andExpect(status().isNotFound());
    }

    @Test
    void globalSuitLesModificationsEtSuppressionsDeResultats() {
        Long id = soireeService.creerSoiree(dto(jeu, false, 1)).getId();
        assertEquals(1016, classementService.getClassementGlobal(a.getId()).elo());
        soireeService.modifierSoiree(id, dto(jeu, true, 1));
        assertEquals(984, classementService.getClassementGlobal(a.getId()).elo());
        soireeService.deleteById(id);
        assertEquals(1000, classementService.getClassementGlobal(a.getId()).elo());
        assertNull(classementService.getClassementGlobal(a.getId()).rang());
    }

    @Test
    void progressionGlobaleRejoueLesJeuxEtCorrespondAuScoreActuel() throws Exception {
        soireeService.creerSoiree(dto(jeu, false, 1));
        soireeService.creerSoiree(dto(autreJeu, true, 2));
        var progression = progressionEloService.getProgression(a.getId());
        assertEquals(List.of(1000, 1016, 1000), progression.points().stream().map(PointEloDto::elo).toList());
        assertEquals(List.of(0, 16, -16), progression.points().stream().map(PointEloDto::variation).toList());
        assertEquals(1016, progression.meilleurElo());
        assertEquals(0, progression.variationTotale());
        assertEquals(2, progression.nbParties());
        assertEquals(classementService.getClassementGlobal(a.getId()), progression.classement());
        verifier(jeu, a, 1016, 1); // consulter la courbe n'écrit rien dans les classements
        mockMvc.perform(get("/joueurs/" + a.getId() + "/progression-elo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.classement.elo").value(1000))
                .andExpect(jsonPath("$.points[0].elo").value(1000))
                .andExpect(jsonPath("$.points[1].jeu").value("Test Elo"))
                .andExpect(jsonPath("$.points[1].date").value("2026-01-01T20:00:00"))
                .andExpect(jsonPath("$.nbParties").value(2));
    }

    @Test
    void progressionInclutLHistoriqueDesAdversaires() {
        Joueur c = joueurDao.save(new Joueur.Builder().pseudo("Historique adversaire " + UUID.randomUUID()).build());
        SoireeCreationDto avant = dto(jeu, false, 1);
        avant.setJoueurIds(List.of(b.getId(), c.getId()));
        avant.getParties().get(0).getScores().get(0).setJoueurId(b.getId());
        avant.getParties().get(0).getScores().get(1).setJoueurId(c.getId());
        soireeService.creerSoiree(avant);
        soireeService.creerSoiree(dto(jeu, false, 2));
        var progression = progressionEloService.getProgression(a.getId());
        assertEquals(1, progression.nbParties());
        assertEquals(1017, progression.points().get(1).elo());
        assertEquals(classementService.getClassementGlobal(a.getId()).elo(), progression.points().get(1).elo());
    }

    @Test
    void progressionReflèteLesCorrectionsEtSuppressions() {
        Long id = soireeService.creerSoiree(dto(jeu, false, 1)).getId();
        soireeService.modifierSoiree(id, dto(jeu, true, 1));
        assertEquals(List.of(1000, 984), progressionEloService.getProgression(a.getId()).points().stream().map(PointEloDto::elo).toList());
        soireeService.deleteById(id);
        assertTrue(progressionEloService.getProgression(a.getId()).points().isEmpty());
    }

    @Test
    void progressionSansPartieEtJoueurAbsent() throws Exception {
        var progression = progressionEloService.getProgression(a.getId());
        assertTrue(progression.points().isEmpty());
        assertEquals(0, progression.nbParties());
        assertEquals(1000, progression.meilleurElo());
        mockMvc.perform(get("/joueurs/-1/progression-elo")).andExpect(status().isNotFound());
        mockMvc.perform(get("/joueurs/" + a.getId() + "/progression-elo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.points").isEmpty());
    }

    @Test
    void progressionIgnoreLesPartiesSansAdversaire() {
        SoireeCreationDto solo = dto(jeu, false, 1);
        solo.getParties().get(0).setScores(List.of(solo.getParties().get(0).getScores().get(0)));
        soireeService.creerSoiree(solo);
        assertEquals(0, progressionEloService.getProgression(a.getId()).nbParties());
    }

    private void classer(Joueur joueur, Jeu jeu, int elo, int nbParties) {
        Classement classement = new Classement(joueur, jeu);
        for (int i = 0; i < nbParties; i++) classement.enregistrerPartie(elo);
        em.persist(classement);
    }

    private Jeu jeu(String nom) {
        return new Jeu.Builder().nom(nom).type(TypeJeu.SOCIETE).nbJoueursMin(1).nbJoueursMax(8).build();
    }
    private SoireeCreationDto dto(Jeu jeu, boolean inverse, int jour) {
        ScoreCreationDto sa = new ScoreCreationDto();
        sa.setJoueurId(a.getId()); sa.setRang(inverse ? 2 : 1);
        ScoreCreationDto sb = new ScoreCreationDto();
        sb.setJoueurId(b.getId()); sb.setRang(inverse ? 1 : 2);
        PartieCreationDto p = new PartieCreationDto();
        p.setJeuId(jeu.getId()); p.setDuree(20); p.setScores(List.of(sa, sb));
        SoireeCreationDto dto = new SoireeCreationDto();
        dto.setDate(LocalDateTime.of(2026, 1, jour, 20, 0)); dto.setLieu("Test Elo");
        dto.setJoueurIds(List.of(a.getId(), b.getId())); dto.setParties(List.of(p));
        return dto;
    }
    private void verifier(Jeu jeu, Joueur joueur, int elo, int nb) {
        ClassementDto c = classementService.getClassement(jeu.getId()).stream()
                .filter(l -> l.joueurId().equals(joueur.getId())).findFirst().orElseThrow();
        assertEquals(elo, c.elo());
        assertEquals(nb, c.nbParties());
    }
}
