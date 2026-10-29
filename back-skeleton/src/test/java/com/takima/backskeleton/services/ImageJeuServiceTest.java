package com.takima.backskeleton.services;

import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.controllers.ApiExceptionHandler;
import com.takima.backskeleton.controllers.ImageJeuController;
import com.takima.backskeleton.controllers.JeuController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Stockage limité au répertoire temporaire JUnit, service métier simulé : aucune base.
@ExtendWith(MockitoExtension.class)
class ImageJeuServiceTest {
    @TempDir Path dossier;
    @Mock JeuService jeux;
    @Mock ClassementService classements;
    ImageJeuService images;
    MockMvc mvc;

    @BeforeEach
    void preparer() {
        images = new ImageJeuService(jeux, dossier.toString(), "http://localhost:8080/");
        mvc = MockMvcBuilders.standaloneSetup(new JeuController(jeux, classements), new ImageJeuController(images))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void nettoyer() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void creationMultipartEtLectureDeLImagePersistante() throws Exception {
        when(jeux.addJeu(any())).thenAnswer(invocation -> {
            JeuDto jeu = invocation.getArgument(0);
            jeu.setId(89L);
            return jeu;
        });
        String reponse = mvc.perform(multipart("/jeux").file(donnees()).file(photo()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("SOCIETE"))
                .andExpect(jsonPath("$.id").value(89))
                .andReturn().getResponse().getContentAsString();
        String url = new com.fasterxml.jackson.databind.ObjectMapper().readTree(reponse).get("image").asText();
        assertTrue(url.startsWith("http://localhost:8080/jeux/images/"));
        mvc.perform(get(url.replace("http://localhost:8080", "")))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        String nom = url.substring(url.lastIndexOf('/') + 1);
        assertNotNull(ImageIO.read(new ImageJeuService(jeux, dossier.toString(), "http://localhost:8080")
                .lire(nom).getInputStream()));
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        assertTrue(Files.exists(dossier.resolve(nom)));
    }

    @Test
    void conserveLePostJsonExistantSansStockage() throws Exception {
        when(jeux.addJeu(any())).thenReturn(new JeuDto.JeuDtoBuilder().id(42L).nom("Jeu URL").build());
        mvc.perform(post("/jeux").contentType("application/json").content("""
                {"nom":"Jeu URL","type":"SOCIETE","nbJoueursMin":1,"nbJoueursMax":4,"image":"https://example.com/jeu.png"}
                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(42));
        try (var fichiers = Files.list(dossier)) { assertEquals(0, fichiers.count()); }
    }

    @Test
    void rollbackDeLaCreationSupprimeLImageNouvellementImportee() throws Exception {
        when(jeux.addJeu(any())).thenThrow(new IllegalArgumentException("Le nom du jeu est obligatoire."));
        mvc.perform(multipart("/jeux").file(donnees()).file(photo())).andExpect(status().isBadRequest());
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        try (var fichiers = Files.list(dossier)) { assertEquals(0, fichiers.count()); }
    }

    @Test
    void refuseLesFauxFichiersLesImagesTropLourdesEtLesPartiesManquantes() throws Exception {
        mvc.perform(multipart("/jeux").file(donnees()).file(new MockMultipartFile("fichier", "faux.png", "image/png", "texte".getBytes())))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/jeux").file(donnees()).file(new MockMultipartFile("fichier", "gros.png", "image/png", new byte[AvatarStockage.MAX_OCTETS + 1])))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(multipart("/jeux").file(donnees())).andExpect(status().isBadRequest());
        verifyNoInteractions(jeux);
    }

    @Test
    void refuseLesCheminsArbitraires() throws Exception {
        mvc.perform(get("/jeux/images/pom.xml")).andExpect(status().isNotFound());
        verifyNoInteractions(jeux);
    }

    private MockMultipartFile donnees() {
        return new MockMultipartFile("jeu", "jeu.json", "application/json", """
                {"nom":"Mon jeu","type":"SOCIETE","nbJoueursMin":2,"nbJoueursMax":4}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private MockMultipartFile photo() throws Exception {
        var octets = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", octets);
        return new MockMultipartFile("fichier", "jeu.png", "image/png", octets.toByteArray());
    }
}
