package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.JeuFavoriDto;
import com.takima.backskeleton.DTO.JeuResumeDto;
import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.DTO.JoueurResumeDto;
import com.takima.backskeleton.DTO.PartieHistoriqueDto;
import com.takima.backskeleton.DTO.ProfilJoueurDto;
import com.takima.backskeleton.models.TypeJeu;
import com.takima.backskeleton.services.JoueurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JoueurController.class)
class JoueurControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JoueurService joueurService;

    @Test
    void listJoueursReturnsDtoJson() throws Exception {
        when(joueurService.findAll()).thenReturn(List.of(joueurDto(1L, "Lea", null)));

        mockMvc.perform(get("/joueurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].pseudo").value("Lea"))
                .andExpect(jsonPath("$[0].avatar").doesNotExist());
    }

    @Test
    void getJoueurReturns404WhenMissing() throws Exception {
        when(joueurService.getById(404L))
                .thenThrow(new NoSuchElementException("Le joueur 404 n'existe pas"));

        mockMvc.perform(get("/joueurs/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Le joueur 404 n'existe pas"));
    }

    @Test
    void getProfilJoueurReturnsProfileJson() throws Exception {
        JeuResumeDto codenames = new JeuResumeDto(3L, "Codenames", TypeJeu.SOCIETE, "https://example.com/codenames.png");
        ProfilJoueurDto profil = new ProfilJoueurDto(
                new JoueurResumeDto(1L, "Léa", "https://example.com/lea.png"),
                5L,
                2L,
                List.of(new JeuFavoriDto(codenames, 3L)),
                List.of(new PartieHistoriqueDto(7L, LocalDateTime.of(2026, 9, 12, 20, 0), codenames, 1, 9)));
        when(joueurService.getProfil(1L)).thenReturn(profil);

        mockMvc.perform(get("/joueurs/1/profil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joueur.id").value(1))
                .andExpect(jsonPath("$.joueur.pseudo").value("Léa"))
                .andExpect(jsonPath("$.joueur.avatar").value("https://example.com/lea.png"))
                .andExpect(jsonPath("$.nbParties").value(5))
                .andExpect(jsonPath("$.nbVictoires").value(2))
                .andExpect(jsonPath("$.jeuxFavoris[0].jeu.id").value(3))
                .andExpect(jsonPath("$.jeuxFavoris[0].jeu.nom").value("Codenames"))
                .andExpect(jsonPath("$.jeuxFavoris[0].jeu.type").value("SOCIETE"))
                .andExpect(jsonPath("$.jeuxFavoris[0].nbParties").value(3))
                .andExpect(jsonPath("$.historique[0].partieId").value(7))
                .andExpect(jsonPath("$.historique[0].date").value("2026-09-12T20:00:00"))
                .andExpect(jsonPath("$.historique[0].jeu.nom").value("Codenames"))
                .andExpect(jsonPath("$.historique[0].rang").value(1))
                .andExpect(jsonPath("$.historique[0].points").value(9));
    }

    @Test
    void getProfilJoueurReturns404WhenMissing() throws Exception {
        when(joueurService.getProfil(404L))
                .thenThrow(new NoSuchElementException("Le joueur 404 n'existe pas"));

        mockMvc.perform(get("/joueurs/404/profil"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Le joueur 404 n'existe pas"));
    }

    @Test
    void createJoueurDeserializesDtoAndReturnsCreated() throws Exception {
        when(joueurService.addJoueur(any(JoueurDto.class)))
                .thenReturn(joueurDto(2L, "Max", "https://example.com/avatar.png"));

        mockMvc.perform(post("/joueurs")
                        .contentType("application/json")
                        .content("""
                                {"pseudo":"Max","avatar":"https://example.com/avatar.png"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.pseudo").value("Max"))
                .andExpect(jsonPath("$.avatar").value("https://example.com/avatar.png"));
    }

    @Test
    void malformedJsonReturnsJsonBadRequest() throws Exception {
        mockMvc.perform(post("/joueurs")
                        .contentType("application/json")
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La requête est invalide."));
    }

    @Test
    void uniquenessConflictReturnsConflictWithoutDatabaseDetails() throws Exception {
        when(joueurService.addJoueur(any(JoueurDto.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate pseudo detail"));

        mockMvc.perform(post("/joueurs")
                        .contentType("application/json")
                        .content("{\"pseudo\":\"Max\",\"avatar\":null}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("La requête entre en conflit avec les données existantes."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("duplicate pseudo detail"))));
    }

    @Test
    void updateJoueurUsesPutAndPathId() throws Exception {
        when(joueurService.updateJoueur(any(JoueurDto.class), eq(3L)))
                .thenReturn(joueurDto(3L, "Sam", null));

        mockMvc.perform(put("/joueurs/3")
                        .contentType("application/json")
                        .content("""
                                {"pseudo":"Sam","avatar":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.pseudo").value("Sam"));
    }

    @Test
    void deleteJoueurReturnsNoContent() throws Exception {
        doNothing().when(joueurService).deleteById(4L);

        mockMvc.perform(delete("/joueurs/4"))
                .andExpect(status().isNoContent());
    }

    private JoueurDto joueurDto(Long id, String pseudo, String avatar) {
        return new JoueurDto.JoueurDtoBuilder()
                .id(id)
                .pseudo(pseudo)
                .avatar(avatar)
                .build();
    }
}
