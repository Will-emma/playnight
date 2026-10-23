package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DTO.*;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.TypeJeu;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogueJeuxIntegrationTest {
    @Autowired private DataSource dataSource;
    @Autowired private JeuDao jeuDao;
    @Autowired private JeuService jeuService;
    @Autowired private JoueurDao joueurDao;
    @Autowired private ClassementService classementService;
    @Autowired private ObjectMapper json;
    @Autowired private MockMvc mvc;

    @Test
    void baseNeuveEtDeuxExecutionsConserventLesDonneesDeDemo() throws Exception {
        dansSchemaTemporaire(connexion -> {
            executer(connexion, "1_TABLES.sql");
            executer(connexion, "2_DEFAULT_ENTRIES.sql");
            executer(connexion, "3_CLASSEMENTS.sql");
            var jeuxAvant = jeux(connexion);
            var avatarsAvant = lignes(connexion, "SELECT pseudo, avatar FROM joueurs ORDER BY id");
            executer(connexion, "4_AJOUT_JEUX_SOCIETE.sql");
            assertEquals(12, jeux(connexion).size());
            assertEquals(jeuxAvant, jeux(connexion).subList(0, 8));
            assertEquals(avatarsAvant, lignes(connexion, "SELECT pseudo, avatar FROM joueurs ORDER BY id"));
            var apres = lignes(connexion, "SELECT * FROM jeux ORDER BY id");
            executer(connexion, "4_AJOUT_JEUX_SOCIETE.sql");
            assertEquals(apres, lignes(connexion, "SELECT * FROM jeux ORDER BY id"));
            assertEquals(4L, lignes(connexion, "SELECT count(*) FROM jeux WHERE description IS NOT NULL AND duree_estimee > 0").get(0).get(0));
            executer(connexion, "5_IMAGES_JEUX_SOCIETE.sql");
            assertEquals(8L, lignes(connexion, "SELECT count(*) FROM jeux WHERE type = 'SOCIETE' AND image LIKE '/assets/jeux/%.png'").get(0).get(0));
            var apresImages = lignes(connexion, "SELECT * FROM jeux ORDER BY id");
            executer(connexion, "4_AJOUT_JEUX_SOCIETE.sql");
            executer(connexion, "5_IMAGES_JEUX_SOCIETE.sql");
            assertEquals(apresImages, lignes(connexion, "SELECT * FROM jeux ORDER BY id"));
        });
    }

    @Test
    void baseExistanteRespecteLesJeuxPersonnalisesEtLesNomsEquivalents() throws Exception {
        dansSchemaTemporaire(connexion -> {
            executer(connexion, "1_TABLES.sql");
            try (var requete = connexion.createStatement()) {
                requete.execute("""
                    INSERT INTO jeux(nom,type,nb_joueurs_min,nb_joueurs_max,image) VALUES
                    (' uno ', 'SOCIETE', 2, 4, 'image-personnelle.png'),
                    ('Echecs', 'SOCIETE', 2, 2, 'mon-echiquier.png'),
                    ('Jeu personnel', 'SOCIETE', 1, 6, NULL)
                    """);
            }
            var avant = jeux(connexion);
            executer(connexion, "4_AJOUT_JEUX_SOCIETE.sql");
            executer(connexion, "4_AJOUT_JEUX_SOCIETE.sql");
            assertEquals(5, jeux(connexion).size());
            assertEquals(avant, jeux(connexion).subList(0, 3));
            assertEquals(1L, lignes(connexion, "SELECT count(*) FROM jeux WHERE lower(btrim(nom)) = 'uno'").get(0).get(0));
            assertEquals(1L, lignes(connexion, "SELECT count(*) FROM jeux WHERE translate(lower(btrim(nom)), 'éèêë', 'eeee') = 'echecs'").get(0).get(0));
        });
    }

    @Test
    void imagesManquantesOuTemporairesCompleteesSansRemplacerLesImagesPersonnelles() throws Exception {
        dansSchemaTemporaire(connexion -> {
            executer(connexion, "1_TABLES.sql");
            try (var requete = connexion.createStatement()) {
                requete.execute("""
                    INSERT INTO jeux(nom,type,nb_joueurs_min,nb_joueurs_max,image) VALUES
                    ('Catan', 'SOCIETE', 3, 4, NULL),
                    (' Dixit ', 'SOCIETE', 3, 8, ''),
                    ('Codenames', 'SOCIETE', 4, 8, 'https://placehold.co/300x200?text=Codenames'),
                    ('7 Wonders', 'SOCIETE', 3, 7, '/image-personnelle.png'),
                    ('Catan', 'JEU_VIDEO', 1, 4, NULL)
                    """);
            }
            executer(connexion, "5_IMAGES_JEUX_SOCIETE.sql");
            assertEquals(List.of(
                    List.of("/assets/jeux/catan.png"), List.of("/assets/jeux/dixit.png"),
                    List.of("/assets/jeux/codenames.png"), List.of("/image-personnelle.png")
            ), lignes(connexion, "SELECT image FROM jeux WHERE type = 'SOCIETE' ORDER BY id"));
            assertNull(lignes(connexion, "SELECT image FROM jeux WHERE type = 'JEU_VIDEO'").get(0).get(0));
            var apres = lignes(connexion, "SELECT * FROM jeux ORDER BY id");
            executer(connexion, "5_IMAGES_JEUX_SOCIETE.sql");
            assertEquals(apres, lignes(connexion, "SELECT * FROM jeux ORDER BY id"));
        });
    }

    @ParameterizedTest
    @CsvSource({"Undercover,3,12,15", "UNO,2,10,20", "Dames,2,2,20", "Échecs,2,2,30"})
    void jeuxAccessiblesParApiEtCompatiblesAvecPartiesScoresEtElo(String nom, int min, int max, int duree) throws Exception {
        var jeu = jeuDao.findAllByOrderByNomAsc().stream().filter(j -> nom.equals(j.getNom())).findFirst().orElseThrow();
        assertEquals(TypeJeu.SOCIETE, jeu.getType());
        assertEquals(min, jeu.getNbJoueursMin());
        assertEquals(max, jeu.getNbJoueursMax());
        assertEquals(duree, jeu.getDureeEstimee());
        assertFalse(jeu.getDescription().isBlank());
        mvc.perform(get("/jeux").param("type", "SOCIETE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].nom", hasItem(nom)));
        mvc.perform(get("/jeux/{id}/fiche", jeu.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.jeu.nom").value(nom))
                .andExpect(jsonPath("$.jeu.description").value(jeu.getDescription()))
                .andExpect(jsonPath("$.jeu.dureeEstimee").value(duree))
                .andExpect(jsonPath("$.nbParties").value(0));
        mvc.perform(get("/jeux/{id}/classement", jeu.getId())).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        List<Long> joueurs = new ArrayList<>();
        List<ScoreCreationDto> scores = new ArrayList<>();
        for (int i = 0; i < min; i++) {
            Long id = joueurDao.save(new Joueur.Builder().pseudo("Catalogue " + UUID.randomUUID()).build()).getId();
            joueurs.add(id);
            ScoreCreationDto score = new ScoreCreationDto();
            score.setJoueurId(id);
            // Undercover : deux civils gagnent contre un infiltré, via les équipes existantes.
            score.setRang(nom.equals("Undercover") && i < 2 ? 1 : i == 0 ? 1 : 2);
            if (nom.equals("Undercover")) score.setNumeroEquipe(i < 2 ? 1 : 2);
            scores.add(score);
        }
        PartieCreationDto partie = new PartieCreationDto();
        partie.setJeuId(jeu.getId()); partie.setDuree(duree); partie.setScores(scores);
        SoireeCreationDto soiree = new SoireeCreationDto();
        soiree.setDate(LocalDateTime.of(2026, 10, 20, 20, 0)); soiree.setLieu("Test catalogue");
        soiree.setJoueurIds(joueurs); soiree.setParties(List.of(partie));
        mvc.perform(post("/soirees").contentType("application/json").content(json.writeValueAsString(soiree)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.parties[0].jeu.nom").value(nom));
        var classement = classementService.getClassement(jeu.getId());
        assertEquals(min, classement.size());
        assertEquals(1016, classement.get(0).elo());
        assertEquals(984, classement.get(classement.size() - 1).elo());
        assertTrue(classement.stream().allMatch(c -> c.nbParties() == 1));
        mvc.perform(get("/jeux/{id}/fiche", jeu.getId())).andExpect(jsonPath("$.nbParties").value(1));
        // La protection de l'historique du ticket de suppression reste effective.
        // Le test enveloppe l'API dans sa propre transaction : force la limite
        // SQL que le commit du service impose normalement sur une vraie requête.
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            jeuService.deleteById(jeu.getId());
            jeuDao.flush();
        });
    }

    @Test
    void jeuJamaisJouePeutEtreSupprimeEtNestPasRecreeAuRecalcul() throws Exception {
        var jeu = jeuDao.findAllByOrderByNomAsc().stream().filter(j -> j.getNom().equals("UNO")).findFirst().orElseThrow();
        Long id = jeu.getId();
        mvc.perform(delete("/jeux/{id}", id)).andExpect(status().isNoContent());
        classementService.recalculer();
        mvc.perform(get("/jeux/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void metadonneesRestentFacultativesEtDureePositive() throws Exception {
        mvc.perform(post("/jeux").contentType("application/json").content("""
                {"nom":"Jeu sans métadonnées", "type":"SOCIETE", "nbJoueursMin":2, "nbJoueursMax":2}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.description").isEmpty());
        mvc.perform(post("/jeux").contentType("application/json").content("""
                {"nom":"Durée invalide", "type":"SOCIETE", "nbJoueursMin":2, "nbJoueursMax":2, "dureeEstimee":0}
                """)).andExpect(status().isBadRequest());
        var jeu = jeuDao.findAllByOrderByNomAsc().stream().filter(j -> j.getNom().equals("Dames")).findFirst().orElseThrow();
        mvc.perform(put("/jeux/{id}", jeu.getId()).contentType("application/json").content("""
                {"nom":"Dames", "type":"SOCIETE", "nbJoueursMin":2, "nbJoueursMax":2, "description":"Ma variante", "dureeEstimee":25}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Ma variante"))
                .andExpect(jsonPath("$.dureeEstimee").value(25));
    }

    private List<List<Object>> jeux(Connection connexion) throws Exception {
        return lignes(connexion, "SELECT id,nom,type,plateforme,nb_joueurs_min,nb_joueurs_max,image FROM jeux ORDER BY id");
    }

    private List<List<Object>> lignes(Connection connexion, String sql) throws Exception {
        var lignes = new ArrayList<List<Object>>();
        try (var requete = connexion.createStatement(); var resultat = requete.executeQuery(sql)) {
            while (resultat.next()) {
                var ligne = new ArrayList<>();
                for (int i = 1; i <= resultat.getMetaData().getColumnCount(); i++) ligne.add(resultat.getObject(i));
                lignes.add(ligne);
            }
        }
        return lignes;
    }

    private void executer(Connection connexion, String script) throws Exception {
        // PostgreSQL interprète le script entier, y compris les blocs DO $$ de la démo.
        try (var requete = connexion.createStatement()) {
            requete.execute(Files.readString(Path.of("initdb", script)));
        }
    }

    @FunctionalInterface
    private interface ScenarioSql { void executer(Connection connexion) throws Exception; }

    private void dansSchemaTemporaire(ScenarioSql scenario) throws Exception {
        // Schéma créé exclusivement pour ce test ; aucune table existante n'est supprimée.
        String schema = "catalogue_test_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connexion = dataSource.getConnection(); var requete = connexion.createStatement()) {
            connexion.setAutoCommit(true);
            requete.execute("CREATE SCHEMA " + schema);
            try {
                requete.execute("SET search_path TO " + schema);
                scenario.executer(connexion);
            } finally {
                requete.execute("SET search_path TO DEFAULT");
                requete.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }
}
