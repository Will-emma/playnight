package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.Soiree;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;
import com.takima.backskeleton.models.TypeJeu;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvatarIntegrationTest {
    private static final Path DOSSIER;
    static {
        try { DOSSIER = Files.createTempDirectory("playnight-avatars-test-"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        registre.add("playnight.avatars.directory", DOSSIER::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired JoueurDao joueurDao;
    @Autowired JoueurService joueurService;
    @Autowired EntityManager em;
    @Autowired ClassementService classementService;
    private Long id;

    @BeforeEach
    void preparer() {
        id = joueurDao.save(new Joueur.Builder().pseudo("Avatar test " + UUID.randomUUID()).build()).getId();
    }

    @Test
    void choixGaleriePersisteEtApparaitDansListeEtProfil() throws Exception {
        for (int choix : new int[]{1, 12}) {
            String avatar = String.format("/assets/avatars/avatar-%02d.png", choix);
            mvc.perform(put("/joueurs/{id}/avatar", id).contentType("application/json").content("{\"avatarId\":" + choix + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.avatar").value(avatar));
            em.clear();
            assertEquals(avatar, joueurService.getById(id).getAvatar());
            mvc.perform(get("/joueurs/{id}/profil", id)).andExpect(jsonPath("$.joueur.avatar").value(avatar));
            mvc.perform(get("/joueurs")).andExpect(jsonPath("$[?(@.id == " + id + ")].avatar", hasItem(avatar)));
        }
    }

    @Test
    void importPhotoPersistantEtServiAvecLeBonType() throws Exception {
        mvc.perform(multipart("/joueurs/{id}/avatar", id).file(photo()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatar").isString());
        em.clear();
        String url = joueurService.getById(id).getAvatar();
        assertTrue(url.startsWith("http://localhost:8080/joueurs/avatars/"));
        mvc.perform(get(url.replace("http://localhost:8080", "")))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/joueurs/{id}/profil", id)).andExpect(jsonPath("$.joueur.avatar").value(url));
    }

    @Test
    void rollbackSqlSupprimeLeNouveauFichier() throws Exception {
        mvc.perform(multipart("/joueurs/{id}/avatar", id).file(photo())).andExpect(status().isOk());
        String url = joueurService.getById(id).getAvatar();
        Path fichier = DOSSIER.resolve(url.substring(url.lastIndexOf('/') + 1));
        assertTrue(Files.exists(fichier));
        TestTransaction.end();
        TestTransaction.start();
        assertFalse(Files.exists(fichier));
    }

    @Test
    void refuseUnChoixHorsGalerieEtLesCheminsArbitraires() throws Exception {
        for (String json : new String[]{"{\"avatarId\":0}", "{\"avatarId\":13}", "{}", "{\"avatarId\":\"../secret\"}"}) {
            mvc.perform(put("/joueurs/{id}/avatar", id).contentType("application/json").content(json))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/joueurs/avatars/pom.xml")).andExpect(status().isNotFound());
    }

    @Test
    void refuseFauxPngEtPhotoTropLourdeSansChangerAvatar() throws Exception {
        mvc.perform(multipart("/joueurs/{id}/avatar", id).file(
                new MockMultipartFile("fichier", "fake.png", "image/png", "texte".getBytes())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        mvc.perform(multipart("/joueurs/{id}/avatar", id).file(
                new MockMultipartFile("fichier", "big.png", "image/png", new byte[AvatarStockage.MAX_OCTETS + 1])))
                .andExpect(status().isPayloadTooLarge());
        assertNull(joueurService.getById(id).getAvatar());
    }

    @Test
    void joueurAbsentOuPhotoManquante() throws Exception {
        mvc.perform(put("/joueurs/-1/avatar").contentType("application/json").content("{\"avatarId\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(multipart("/joueurs/-1/avatar").file(photo())).andExpect(status().isNotFound());
        mvc.perform(multipart("/joueurs/{id}/avatar", id)).andExpect(status().isBadRequest());
    }

    @Test
    void nouvelAvatarVisibleDansSoireesEtClassementsSansRecalculElo() throws Exception {
        Joueur a = joueurDao.findById(id).orElseThrow();
        Joueur b = joueurDao.save(new Joueur.Builder().pseudo("Adversaire avatar " + UUID.randomUUID()).build());
        Jeu jeu = new Jeu.Builder().nom("Jeu avatar").type(TypeJeu.SOCIETE).nbJoueursMin(2).nbJoueursMax(2).build();
        em.persist(jeu);
        Soiree soiree = new Soiree.Builder().date(LocalDateTime.of(2026, 1, 1, 20, 0)).lieu("Test avatar").joueurs(List.of(a, b)).build();
        em.persist(soiree);
        Partie partie = new Partie.Builder().jeu(jeu).soiree(soiree).duree(20).build();
        em.persist(partie);
        em.persist(new Score.Builder().joueur(a).partie(partie).rang(1).build());
        em.persist(new Score.Builder().joueur(b).partie(partie).rang(2).build());
        classementService.recalculer();
        int eloAvant = classementService.getClassement(jeu.getId()).get(0).elo();
        mvc.perform(put("/joueurs/{id}/avatar", id).contentType("application/json").content("{\"avatarId\":4}"))
                .andExpect(status().isOk());
        em.clear();
        String avatar = "/assets/avatars/avatar-04.png";
        for (String url : List.of("/jeux/" + jeu.getId() + "/classement", "/jeux/" + jeu.getId() + "/fiche", "/soirees/" + soiree.getId())) {
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("$..avatar", hasItem(avatar)));
        }
        assertEquals(eloAvant, classementService.getClassement(jeu.getId()).get(0).elo());
    }

    private MockMultipartFile photo() throws Exception {
        var sortie = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", sortie);
        return new MockMultipartFile("fichier", "ma-photo.png", "image/png", sortie.toByteArray());
    }
}
