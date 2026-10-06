package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.TypeJeu;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JeuServiceTest {
    @Mock
    private JeuDao jeuDao;

    @InjectMocks
    private JeuService jeuService;

    @Test
    void findAllWithoutTypeReturnsAllGamesSortedByName() {
        when(jeuDao.findAllByOrderByNomAsc()).thenReturn(List.of(
                jeu(1L, "Catan", TypeJeu.SOCIETE),
                jeu(5L, "Mario Kart 8 Deluxe", TypeJeu.JEU_VIDEO)));

        List<JeuDto> jeux = jeuService.findAll(null);

        assertEquals(2, jeux.size());
        assertEquals("Catan", jeux.get(0).getNom());
        assertEquals("Mario Kart 8 Deluxe", jeux.get(1).getNom());
        verify(jeuDao, never()).findByTypeOrderByNomAsc(any());
    }

    @Test
    void findAllWithTypeUsesTypeFilter() {
        when(jeuDao.findByTypeOrderByNomAsc(TypeJeu.JEU_VIDEO))
                .thenReturn(List.of(jeu(5L, "Mario Kart 8 Deluxe", TypeJeu.JEU_VIDEO)));

        List<JeuDto> jeux = jeuService.findAll(TypeJeu.JEU_VIDEO);

        assertEquals(1, jeux.size());
        assertEquals(TypeJeu.JEU_VIDEO, jeux.get(0).getType());
        verify(jeuDao, never()).findAllByOrderByNomAsc();
    }

    @Test
    void getByIdMapsAllFieldsToDto() {
        Jeu marioKart = new Jeu.Builder()
                .id(5L)
                .nom("Mario Kart 8 Deluxe")
                .type(TypeJeu.JEU_VIDEO)
                .plateforme("Nintendo Switch")
                .nbJoueursMin(1)
                .nbJoueursMax(4)
                .image("https://example.com/mariokart.png")
                .build();
        when(jeuDao.findById(5L)).thenReturn(Optional.of(marioKart));

        JeuDto jeu = jeuService.getById(5L);

        assertEquals(5L, jeu.getId());
        assertEquals("Mario Kart 8 Deluxe", jeu.getNom());
        assertEquals(TypeJeu.JEU_VIDEO, jeu.getType());
        assertEquals("Nintendo Switch", jeu.getPlateforme());
        assertEquals(1, jeu.getNbJoueursMin());
        assertEquals(4, jeu.getNbJoueursMax());
        assertEquals("https://example.com/mariokart.png", jeu.getImage());
    }

    @Test
    void getByIdThrowsWhenGameDoesNotExist() {
        when(jeuDao.findById(404L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> jeuService.getById(404L));
        assertEquals("Le jeu 404 n'existe pas", exception.getMessage());
    }

    @Test
    void addJeuIgnoresRequestIdAndReturnsSavedGame() {
        when(jeuDao.save(any(Jeu.class))).thenReturn(jeu(2L, "Dixit", TypeJeu.SOCIETE));

        JeuDto created = jeuService.addJeu(dto(99L, "Dixit", TypeJeu.SOCIETE));

        ArgumentCaptor<Jeu> captor = ArgumentCaptor.forClass(Jeu.class);
        verify(jeuDao).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(2L, created.getId());
        assertEquals("Dixit", created.getNom());
    }

    @Test
    void updateJeuUsesPathIdInsteadOfRequestId() {
        when(jeuDao.findById(3L)).thenReturn(Optional.of(jeu(3L, "Ancien nom", TypeJeu.SOCIETE)));
        when(jeuDao.save(any(Jeu.class))).thenReturn(jeu(3L, "Codenames", TypeJeu.SOCIETE));

        JeuDto updated = jeuService.updateJeu(dto(99L, "Codenames", TypeJeu.SOCIETE), 3L);

        ArgumentCaptor<Jeu> captor = ArgumentCaptor.forClass(Jeu.class);
        verify(jeuDao).save(captor.capture());
        assertEquals(3L, captor.getValue().getId());
        assertEquals(3L, updated.getId());
        assertEquals("Codenames", updated.getNom());
    }

    @Test
    void updateJeuThrowsAndDoesNotSaveWhenGameDoesNotExist() {
        when(jeuDao.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> jeuService.updateJeu(dto(null, "Catan", TypeJeu.SOCIETE), 404L));
        verify(jeuDao, never()).save(any());
    }

    @Test
    void deleteByIdDeletesExistingGame() {
        when(jeuDao.findById(4L)).thenReturn(Optional.of(jeu(4L, "Catan", TypeJeu.SOCIETE)));

        jeuService.deleteById(4L);

        verify(jeuDao).deleteById(4L);
    }

    private Jeu jeu(Long id, String nom, TypeJeu type) {
        return new Jeu.Builder()
                .id(id)
                .nom(nom)
                .type(type)
                .nbJoueursMin(2)
                .nbJoueursMax(4)
                .build();
    }

    private JeuDto dto(Long id, String nom, TypeJeu type) {
        return new JeuDto.JeuDtoBuilder()
                .id(id)
                .nom(nom)
                .type(type)
                .nbJoueursMin(2)
                .nbJoueursMax(4)
                .build();
    }
}
