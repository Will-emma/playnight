package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.JeuResumeDto;
import com.takima.backskeleton.DTO.JoueurResumeDto;
import com.takima.backskeleton.DTO.PartieDto;
import com.takima.backskeleton.DTO.ScoreDto;
import com.takima.backskeleton.DTO.SoireeCreationDto;
import com.takima.backskeleton.DTO.SoireeDetailDto;
import com.takima.backskeleton.DTO.SoireeDto;
import com.takima.backskeleton.models.TypeJeu;
import com.takima.backskeleton.services.SoireeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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

@WebMvcTest(SoireeController.class)
class SoireeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SoireeService soireeService;

    @Test
    void listSoireesReturnsSummaryDtosWithoutParties() throws Exception {
        when(soireeService.findAll()).thenReturn(List.of(soireeDto()));

        mockMvc.perform(get("/soirees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].lieu").value("Chez Léa"))
                .andExpect(jsonPath("$[0].joueurs[0].pseudo").value("Léa"))
                .andExpect(jsonPath("$[0].nbParties").value(3))
                .andExpect(jsonPath("$[0].parties").doesNotExist());
    }

    @Test
    void getSoireeReturnsDetailsWithScores() throws Exception {
        when(soireeService.getById(1L)).thenReturn(detailDto());

        mockMvc.perform(get("/soirees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parties[0].jeu.nom").value("Catan"))
                .andExpect(jsonPath("$.parties[0].scores[0].rang").value(1))
                .andExpect(jsonPath("$.parties[0].scores[0].joueur.pseudo").value("Léa"));
    }

    @Test
    void getSoireeReturns404WhenMissing() throws Exception {
        when(soireeService.getById(404L))
                .thenThrow(new NoSuchElementException("La soirée 404 n'existe pas"));

        mockMvc.perform(get("/soirees/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La soirée 404 n'existe pas"));
    }

    @Test
    void malformedIdReturnsJsonBadRequest() throws Exception {
        mockMvc.perform(get("/soirees/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La requête est invalide."));
    }

    @Test
    void unexpectedErrorReturnsGenericJsonWithoutInternalDetails() throws Exception {
        when(soireeService.getById(1L))
                .thenThrow(new IllegalStateException("sensitive SQL details"));

        mockMvc.perform(get("/soirees/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Une erreur interne est survenue."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("sensitive SQL details"))));
    }

    @Test
    void deleteSoireeReturnsNoContent() throws Exception {
        doNothing().when(soireeService).deleteById(4L);

        mockMvc.perform(delete("/soirees/4"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createSoireeReturnsCreatedWithDetails() throws Exception {
        when(soireeService.creerSoiree(any(SoireeCreationDto.class))).thenReturn(detailDto());

        mockMvc.perform(post("/soirees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.parties[0].jeu.nom").value("Catan"));
    }

    @Test
    void createSoireeReturns400WithMessageWhenBusinessRuleFails() throws Exception {
        when(soireeService.creerSoiree(any(SoireeCreationDto.class)))
                .thenThrow(new IllegalArgumentException("Le joueur 4 n'est pas présent à la soirée"));

        mockMvc.perform(post("/soirees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le joueur 4 n'est pas présent à la soirée"));
    }

    @Test
    void updateSoireeReturnsUpdatedDetails() throws Exception {
        when(soireeService.modifierSoiree(eq(1L), any(SoireeCreationDto.class))).thenReturn(detailDto());

        mockMvc.perform(put("/soirees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lieu").value("Chez Léa"));
    }

    @Test
    void updateSoireeReturns404WhenMissing() throws Exception {
        when(soireeService.modifierSoiree(eq(404L), any(SoireeCreationDto.class)))
                .thenThrow(new NoSuchElementException("La soirée 404 n'existe pas"));

        mockMvc.perform(put("/soirees/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La soirée 404 n'existe pas"));
    }

    // Corps de requête au format de docs/format-dto.md
    private static final String CREATION_JSON = """
            {
              "date": "2026-10-10T20:00:00",
              "lieu": "Chez Léa",
              "joueurIds": [1, 2, 3],
              "parties": [
                {
                  "jeuId": 3,
                  "duree": 45,
                  "scores": [
                    { "joueurId": 1, "rang": 1, "points": 10, "numeroEquipe": null },
                    { "joueurId": 2, "rang": 2, "points": 8, "numeroEquipe": null },
                    { "joueurId": 3, "rang": 3, "points": 4, "numeroEquipe": null }
                  ]
                }
              ]
            }
            """;

    private SoireeDto soireeDto() {
        return new SoireeDto(
                1L,
                LocalDateTime.of(2026, 10, 3, 19, 30),
                "Chez Léa",
                List.of(new JoueurResumeDto(1L, "Léa", null)),
                3);
    }

    private SoireeDetailDto detailDto() {
        return new SoireeDetailDto(
                1L,
                LocalDateTime.of(2026, 10, 3, 19, 30),
                "Chez Léa",
                List.of(new JoueurResumeDto(1L, "Léa", null)),
                List.of(new PartieDto(
                        2L,
                        30,
                        new JeuResumeDto(3L, "Catan", TypeJeu.SOCIETE, null),
                        List.of(new ScoreDto(
                                5L,
                                10,
                                1,
                                null,
                                new JoueurResumeDto(1L, "Léa", null))))));
    }
}
