package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.FicheJeuDto;
import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.DTO.JoueurResumeDto;
import com.takima.backskeleton.DTO.MeilleurJoueurDto;
import com.takima.backskeleton.DTO.PartieJoueeDto;
import com.takima.backskeleton.DTO.RawgImportDto;
import com.takima.backskeleton.DTO.RawgJeuDto;
import com.takima.backskeleton.models.TypeJeu;
import com.takima.backskeleton.services.JeuService;
import com.takima.backskeleton.services.RawgIndisponibleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JeuController.class)
class JeuControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JeuService jeuService;

    @MockBean
    private com.takima.backskeleton.services.ClassementService classementService;

    @Test
    void listJeuxReturnsDtoJsonWithContractFieldNames() throws Exception {
        when(jeuService.findAll(null)).thenReturn(List.of(jeuDto(1L, "Catan", TypeJeu.SOCIETE)));

        mockMvc.perform(get("/jeux"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("Catan"))
                .andExpect(jsonPath("$[0].type").value("SOCIETE"))
                .andExpect(jsonPath("$[0].nbJoueursMin").value(2))
                .andExpect(jsonPath("$[0].nbJoueursMax").value(4))
                .andExpect(jsonPath("$[0].plateforme").doesNotExist());
    }

    @Test
    void listJeuxPassesTypeFilterToService() throws Exception {
        when(jeuService.findAll(TypeJeu.JEU_VIDEO))
                .thenReturn(List.of(jeuDto(5L, "Mario Kart 8 Deluxe", TypeJeu.JEU_VIDEO)));

        mockMvc.perform(get("/jeux").param("type", "JEU_VIDEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].type").value("JEU_VIDEO"));
    }

    @Test
    void listJeuxReturns400ForUnknownType() throws Exception {
        mockMvc.perform(get("/jeux").param("type", "AUTRE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJeuReturns404WhenMissing() throws Exception {
        when(jeuService.getById(404L))
                .thenThrow(new NoSuchElementException("Le jeu 404 n'existe pas"));

        mockMvc.perform(get("/jeux/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Le jeu 404 n'existe pas"));
    }

    @Test
    void createJeuDeserializesDtoAndReturnsCreated() throws Exception {
        JeuDto marioKart = new JeuDto.JeuDtoBuilder()
                .id(5L)
                .nom("Mario Kart 8 Deluxe")
                .type(TypeJeu.JEU_VIDEO)
                .plateforme("Nintendo Switch")
                .nbJoueursMin(1)
                .nbJoueursMax(4)
                .image("https://example.com/mariokart.png")
                .build();
        when(jeuService.addJeu(any(JeuDto.class))).thenReturn(marioKart);

        mockMvc.perform(post("/jeux")
                        .contentType("application/json")
                        .content("""
                                {"nom":"Mario Kart 8 Deluxe","type":"JEU_VIDEO","plateforme":"Nintendo Switch",
                                 "nbJoueursMin":1,"nbJoueursMax":4,"image":"https://example.com/mariokart.png"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nom").value("Mario Kart 8 Deluxe"))
                .andExpect(jsonPath("$.type").value("JEU_VIDEO"))
                .andExpect(jsonPath("$.plateforme").value("Nintendo Switch"))
                .andExpect(jsonPath("$.nbJoueursMin").value(1))
                .andExpect(jsonPath("$.nbJoueursMax").value(4))
                .andExpect(jsonPath("$.image").value("https://example.com/mariokart.png"));
    }

    @Test
    void updateJeuUsesPutAndPathId() throws Exception {
        when(jeuService.updateJeu(any(JeuDto.class), eq(3L)))
                .thenReturn(jeuDto(3L, "Codenames", TypeJeu.SOCIETE));

        mockMvc.perform(put("/jeux/3")
                        .contentType("application/json")
                        .content("""
                                {"nom":"Codenames","type":"SOCIETE","nbJoueursMin":2,"nbJoueursMax":4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.nom").value("Codenames"));
    }

    @Test
    void deleteJeuReturnsNoContent() throws Exception {
        doNothing().when(jeuService).deleteById(4L);

        mockMvc.perform(delete("/jeux/4"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getFicheJeuReturnsJsonWithContractFieldNames() throws Exception {
        FicheJeuDto fiche = new FicheJeuDto(
                jeuDto(1L, "Catan", TypeJeu.SOCIETE),
                1L,
                List.of(new MeilleurJoueurDto(new JoueurResumeDto(1L, "Léa", "https://example.com/lea.png"), 1L)),
                List.of(new PartieJoueeDto(7L, LocalDateTime.of(2026, 9, 12, 20, 0), 75, List.of("Léa"))));
        when(jeuService.getFiche(1L)).thenReturn(fiche);

        mockMvc.perform(get("/jeux/1/fiche"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeu.id").value(1))
                .andExpect(jsonPath("$.jeu.nom").value("Catan"))
                .andExpect(jsonPath("$.nbParties").value(1))
                .andExpect(jsonPath("$.meilleursJoueurs[0].joueur.id").value(1))
                .andExpect(jsonPath("$.meilleursJoueurs[0].joueur.pseudo").value("Léa"))
                .andExpect(jsonPath("$.meilleursJoueurs[0].joueur.avatar").value("https://example.com/lea.png"))
                .andExpect(jsonPath("$.meilleursJoueurs[0].nbVictoires").value(1))
                .andExpect(jsonPath("$.parties[0].partieId").value(7))
                .andExpect(jsonPath("$.parties[0].date").value("2026-09-12T20:00:00"))
                .andExpect(jsonPath("$.parties[0].duree").value(75))
                .andExpect(jsonPath("$.parties[0].vainqueurs[0]").value("Léa"));
    }

    @Test
    void getFicheJeuReturns404WhenMissing() throws Exception {
        when(jeuService.getFiche(404L))
                .thenThrow(new NoSuchElementException("Le jeu 404 n'existe pas"));

        mockMvc.perform(get("/jeux/404/fiche"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Le jeu 404 n'existe pas"));
    }

    @Test
    void rechercherSurRawgReturnsResultsWithContractFieldNames() throws Exception {
        when(jeuService.rechercherSurRawg("zelda")).thenReturn(List.of(new RawgJeuDto(
                22511L, "The Legend of Zelda: Breath of the Wild", "https://example.com/zelda.jpg",
                "Nintendo Switch, Wii U", "2017-03-03")));

        mockMvc.perform(get("/jeux/rawg").param("recherche", "zelda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRawg").value(22511))
                .andExpect(jsonPath("$[0].nom").value("The Legend of Zelda: Breath of the Wild"))
                .andExpect(jsonPath("$[0].image").value("https://example.com/zelda.jpg"))
                .andExpect(jsonPath("$[0].plateforme").value("Nintendo Switch, Wii U"))
                .andExpect(jsonPath("$[0].dateSortie").value("2017-03-03"));
    }

    @Test
    void rechercherSurRawgReturns503WhenRawgIsUnavailable() throws Exception {
        when(jeuService.rechercherSurRawg("zelda"))
                .thenThrow(new RawgIndisponibleException("RAWG ne répond pas pour le moment, réessayez plus tard."));

        mockMvc.perform(get("/jeux/rawg").param("recherche", "zelda"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("RAWG ne répond pas pour le moment, réessayez plus tard."));
    }

    @Test
    void importerDepuisRawgReadsPlayerCountAndReturnsCreated() throws Exception {
        when(jeuService.importerDepuisRawg(eq(22511L), any(RawgImportDto.class)))
                .thenReturn(jeuDto(9L, "The Legend of Zelda: Breath of the Wild", TypeJeu.JEU_VIDEO));

        mockMvc.perform(post("/jeux/rawg/22511")
                        .contentType("application/json")
                        .content("""
                                {"nbJoueursMin":1,"nbJoueursMax":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.type").value("JEU_VIDEO"));
    }

    @Test
    void importerDepuisRawgReturns400WithMessageWhenGameAlreadyExists() throws Exception {
        when(jeuService.importerDepuisRawg(eq(22511L), any(RawgImportDto.class)))
                .thenThrow(new IllegalArgumentException("« Zelda » est déjà dans la ludothèque."));

        mockMvc.perform(post("/jeux/rawg/22511")
                        .contentType("application/json")
                        .content("{\"nbJoueursMin\":1,\"nbJoueursMax\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("« Zelda » est déjà dans la ludothèque."));
    }

    @Test
    void classementVictoiresReturnsExistingPlayerContract() throws Exception {
        when(jeuService.getClassementVictoires(42L)).thenReturn(List.of(
                new MeilleurJoueurDto(new JoueurResumeDto(7L, "Léa", "/avatar.png"), 3L)));
        mockMvc.perform(get("/jeux/42/classement-victoires"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].joueur.id").value(7))
                .andExpect(jsonPath("$[0].joueur.pseudo").value("Léa"))
                .andExpect(jsonPath("$[0].joueur.avatar").value("/avatar.png"))
                .andExpect(jsonPath("$[0].nbVictoires").value(3))
                .andExpect(jsonPath("$[0].elo").doesNotExist());
    }

    @Test
    void classementVictoiresEmpty() throws Exception {
        when(jeuService.getClassementVictoires(42L)).thenReturn(List.of());
        mockMvc.perform(get("/jeux/42/classement-victoires"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void classementVictoiresMissingGame() throws Exception {
        when(jeuService.getClassementVictoires(404L)).thenThrow(new NoSuchElementException("Le jeu 404 n'existe pas"));
        mockMvc.perform(get("/jeux/404/classement-victoires"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Le jeu 404 n'existe pas"));
    }

    @Test
    void classementEloReturnsContract() throws Exception {
        when(classementService.getClassement(1L)).thenReturn(List.of(
                new com.takima.backskeleton.DTO.ClassementDto(2L, "Léa", "avatar.png", 1016, 1, 1)));
        mockMvc.perform(get("/jeux/1/classement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].joueurId").value(2))
                .andExpect(jsonPath("$[0].pseudo").value("Léa"))
                .andExpect(jsonPath("$[0].avatar").value("avatar.png"))
                .andExpect(jsonPath("$[0].elo").value(1016))
                .andExpect(jsonPath("$[0].nbParties").value(1))
                .andExpect(jsonPath("$[0].rang").value(1));
    }

    @Test
    void classementEloEmpty() throws Exception {
        when(classementService.getClassement(1L)).thenReturn(List.of());
        mockMvc.perform(get("/jeux/1/classement"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void classementEloMissingGame() throws Exception {
        when(classementService.getClassement(404L)).thenThrow(new NoSuchElementException("Le jeu 404 n'existe pas"));
        mockMvc.perform(get("/jeux/404/classement"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Le jeu 404 n'existe pas"));
    }

    private JeuDto jeuDto(Long id, String nom, TypeJeu type) {
        return new JeuDto.JeuDtoBuilder()
                .id(id)
                .nom(nom)
                .type(type)
                .nbJoueursMin(2)
                .nbJoueursMax(4)
                .build();
    }
}
