package com.takima.backskeleton.services;

import com.takima.backskeleton.DTO.RawgJeuDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

// Client HTTP simulé : aucune requête RAWG réelle, aucun contexte Spring Boot ni base.
class RawgServiceTest {
    private MockRestServiceServer serveur;
    private RawgService service;

    @BeforeEach
    void preparer() {
        RestTemplate client = new RestTemplate();
        serveur = MockRestServiceServer.bindTo(client).build();
        service = new RawgService(new RestTemplateBuilder(rest -> rest.setRequestFactory(client.getRequestFactory())),
                "cle-factice", "https://rawg.invalid/api");
    }

    @Test
    void rechercheMonopolyConserveImagesEtPlateformesSansImporter() {
        serveur.expect(requestTo("https://rawg.invalid/api/games?key=cle-factice&search=Monopoly&page_size=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"results":[{"id":20823,"name":"Monopoly","background_image":"https://example.com/monopoly.jpg",
                        "released":"2008-10-20","platforms":[{"platform":{"name":"PC"}},
                        {"platform":{"name":"Nintendo Switch"}}]}]}
                        """, MediaType.APPLICATION_JSON));
        var jeux = service.rechercher("  Monopoly  ");
        assertEquals(1, jeux.size());
        RawgJeuDto jeu = jeux.get(0);
        assertEquals(20823L, jeu.getIdRawg());
        assertEquals("Monopoly", jeu.getNom());
        assertEquals("https://example.com/monopoly.jpg", jeu.getImage());
        assertEquals("PC, Nintendo Switch", jeu.getPlateforme());
        serveur.verify();
    }

    @Test
    void lectureDuJeuVideoClassiqueConserveLeMappingEtLesChampsFacultatifs() {
        serveur.expect(requestTo("https://rawg.invalid/api/games/3498?key=cle-factice"))
                .andRespond(withSuccess("""
                        {"id":3498,"name":"Grand Theft Auto V","platforms":[]}
                        """, MediaType.APPLICATION_JSON));
        RawgJeuDto jeu = service.getById(3498L);
        assertEquals("Grand Theft Auto V", jeu.getNom());
        assertNull(jeu.getImage());
        assertNull(jeu.getPlateforme());
        assertNull(jeu.getDateSortie());
        serveur.verify();
    }

    @Test
    void rechercheVideNeDeclencheAucuneRequete() {
        assertThrows(IllegalArgumentException.class, () -> service.rechercher("  "));
        serveur.verify();
    }

    @Test
    void erreurRawgResteUneIndisponibiliteComprehensible() {
        serveur.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThrows(RawgIndisponibleException.class, () -> service.rechercher("Monopoly"));
        serveur.verify();
    }

    @Test
    void jeuRawgInconnuRenvoieUneErreurMetier() {
        serveur.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThrows(NoSuchElementException.class, () -> service.getById(404L));
        serveur.verify();
    }
}
