package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.models.TypeJeu;
import com.takima.backskeleton.services.JeuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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
