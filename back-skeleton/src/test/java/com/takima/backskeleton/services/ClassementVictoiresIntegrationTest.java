package com.takima.backskeleton.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.takima.backskeleton.DAO.*;
import com.takima.backskeleton.DTO.*;
import com.takima.backskeleton.models.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// À exécuter sur une base PostgreSQL de test ; chaque scénario est annulé.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClassementVictoiresIntegrationTest {
    @Autowired private EntityManager em;
    @Autowired private JeuDao jeuDao;
    @Autowired private JoueurDao joueurDao;
    @Autowired private ScoreDao scoreDao;
    @Autowired private JeuService jeuService;
    @Autowired private SoireeService soireeService;
    @Autowired private ClassementService classementService;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    private Jeu jeu;
    private List<Joueur> joueurs;

    @BeforeEach
    void preparer() {
        jeu = jeuDao.save(new Jeu.Builder().nom("Victoires " + UUID.randomUUID())
                .type(TypeJeu.SOCIETE).nbJoueursMin(2).nbJoueursMax(8).build());
        joueurs = new ArrayList<>();
        String suffixe = " " + UUID.randomUUID();
        for (String pseudo : List.of("Alice", "Benoît", "Chloé", "Dina", "Sans victoire", "Jamais joué")) {
            joueurs.add(joueurDao.save(new Joueur.Builder().pseudo(pseudo + suffixe).avatar("/avatar.png").build()));
        }
    }

    @Test
    void aggregationParJeuCompteIndividuelsEquipesEtPremiersExAequo() throws Exception {
        // 1 victoire individuelle, puis 1 victoire pour chaque membre de l'équipe 1.
        partie(jeu, new int[]{0, 4}, new int[]{1, 2}, null);
        partie(jeu, new int[]{0, 1, 2, 4}, new int[]{1, 1, 2, 2}, new int[]{1, 1, 2, 2});
        // Plusieurs premiers : une victoire chacun, quel que soit leur nombre de points.
        partie(jeu, new int[]{0, 1, 2, 4}, new int[]{1, 1, 1, 2}, null);
        partie(jeu, new int[]{2, 3, 4}, new int[]{1, 1, 2}, null);
        partie(jeu, new int[]{0, 4}, new int[]{2, 3}, null); // aucun premier
        partie(jeu, new int[]{}, new int[]{}, null); // sans résultat
        Jeu autre = jeuDao.save(new Jeu.Builder().nom("Autre jeu").type(TypeJeu.SOCIETE)
                .nbJoueursMin(2).nbJoueursMax(8).build());
        partie(autre, new int[]{4, 0}, new int[]{1, 2}, null);
        em.flush();
        em.clear();

        var classement = scoreDao.findClassementVictoiresByJeuId(jeu.getId());
        assertEquals(List.of(3L, 2L, 2L, 1L), classement.stream().map(ClassementVictoiresProjection::getNbVictoires).toList());
        assertEquals(joueurs.subList(0, 4).stream().map(Joueur::getId).toList(),
                classement.stream().map(ClassementVictoiresProjection::getJoueurId).toList());
        assertEquals("/avatar.png", classement.get(0).getAvatar());

        var api = mvc.perform(get("/jeux/{id}/classement-victoires", jeu.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].nbVictoires").value(3))
                .andExpect(jsonPath("$[0].joueur.id").value(joueurs.get(0).getId()))
                .andReturn().getResponse().getContentAsString();
        var fiche = mvc.perform(get("/jeux/{id}/fiche", jeu.getId())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(json.readTree(api), json.readTree(fiche).get("meilleursJoueurs"));
    }

    @Test
    void jeuSansVictoireRenvoieListeVideEtJeuAbsentRenvoie404() throws Exception {
        partie(jeu, new int[]{0, 1}, new int[]{2, 2}, null);
        mvc.perform(get("/jeux/{id}/classement-victoires", jeu.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/jeux/-1/classement-victoires")).andExpect(status().isNotFound());
    }

    @Test
    void joueurSansResultatPeutEtreSupprimeMaisUnVainqueurResteProtege() {
        partie(jeu, new int[]{0, 1}, new int[]{1, 2}, null);
        joueurDao.deleteById(joueurs.get(5).getId());
        joueurDao.flush();
        var classement = jeuService.getClassementVictoires(jeu.getId());
        assertEquals(1, classement.size());
        assertEquals(joueurs.get(0).getId(), classement.get(0).getJoueur().getId());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            joueurDao.deleteById(joueurs.get(0).getId());
            joueurDao.flush();
        });
    }

    @Test
    void modificationEtSuppressionDeSoireeActualisentLesVictoires() {
        var dto = soiree(1, 2);
        Long id = soireeService.creerSoiree(dto).getId();
        assertEquals(joueurs.get(0).getId(), jeuService.getClassementVictoires(jeu.getId()).get(0).getJoueur().getId());
        soireeService.modifierSoiree(id, soiree(2, 1));
        var apres = jeuService.getClassementVictoires(jeu.getId());
        assertEquals(1, apres.size());
        assertEquals(joueurs.get(1).getId(), apres.get(0).getJoueur().getId());
        assertEquals(1L, apres.get(0).getNbVictoires());
        soireeService.deleteById(id);
        assertTrue(jeuService.getClassementVictoires(jeu.getId()).isEmpty());
    }

    @Test
    void egaliteCompteDeuxVictoiresSansChangerLaRegleElo() throws Exception {
        soireeService.creerSoiree(soiree(1, 1));
        assertEquals(List.of(1L, 1L), jeuService.getClassementVictoires(jeu.getId()).stream()
                .map(MeilleurJoueurDto::getNbVictoires).toList());
        assertTrue(classementService.getClassement(jeu.getId()).stream().allMatch(c -> c.elo() == 1000 && c.rang() == 1));
        mvc.perform(get("/jeux/{id}/classement", jeu.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].elo").value(1000)).andExpect(jsonPath("$[1].elo").value(1000));
    }

    @Test
    void lectureDuClassementNeModifiePasLElo() throws Exception {
        soireeService.creerSoiree(soiree(1, 2));
        var avant = classementService.getClassement(jeu.getId());
        jeuService.getClassementVictoires(jeu.getId());
        jeuService.getFiche(jeu.getId());
        assertEquals(avant, classementService.getClassement(jeu.getId()));
        mvc.perform(get("/jeux/{id}/classement", jeu.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].elo").value(1016)).andExpect(jsonPath("$[1].elo").value(984));
    }

    private void partie(Jeu cible, int[] participants, int[] rangs, int[] equipes) {
        Soiree soiree = new Soiree.Builder().date(LocalDateTime.of(2026, 10, 24, 20, 0))
                .lieu("Test").joueurs(List.of()).build();
        em.persist(soiree);
        Partie partie = new Partie.Builder().jeu(cible).soiree(soiree).duree(30).build();
        em.persist(partie);
        for (int i = 0; i < participants.length; i++) {
            em.persist(new Score.Builder().partie(partie).joueur(joueurs.get(participants[i]))
                    .rang(rangs[i]).numeroEquipe(equipes == null ? null : equipes[i]).points(99 - i).build());
        }
    }

    private SoireeCreationDto soiree(int rangA, int rangB) {
        List<ScoreCreationDto> scores = new ArrayList<>();
        int[] rangs = {rangA, rangB};
        for (int i = 0; i < 2; i++) {
            ScoreCreationDto score = new ScoreCreationDto();
            score.setJoueurId(joueurs.get(i).getId());
            score.setRang(rangs[i]);
            scores.add(score);
        }
        PartieCreationDto partie = new PartieCreationDto();
        partie.setJeuId(jeu.getId()); partie.setDuree(30); partie.setScores(scores);
        SoireeCreationDto dto = new SoireeCreationDto();
        dto.setDate(LocalDateTime.of(2026, 10, 24, 20, 0)); dto.setLieu("Test");
        dto.setJoueurIds(joueurs.subList(0, 2).stream().map(Joueur::getId).toList());
        dto.setParties(List.of(partie));
        return dto;
    }
}
