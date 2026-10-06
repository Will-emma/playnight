package com.takima.backskeleton.controllers;

import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.services.JoueurService;
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