package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.JeuResumeDto;
import com.takima.backskeleton.DTO.JoueurResumeDto;
import com.takima.backskeleton.DTO.PartieDto;
import com.takima.backskeleton.DTO.ScoreDto;
import com.takima.backskeleton.DTO.SoireeDetailDto;
import com.takima.backskeleton.DTO.SoireeDto;
import com.takima.backskeleton.models.TypeJeu;
import com.takima.backskeleton.services.SoireeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    void deleteSoireeReturnsNoContent() throws Exception {
        doNothing().when(soireeService).deleteById(4L);

        mockMvc.perform(delete("/soirees/4"))
                .andExpect(status().isNoContent());
    }

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
