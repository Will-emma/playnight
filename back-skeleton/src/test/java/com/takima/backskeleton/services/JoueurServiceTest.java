package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.models.Joueur;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoueurServiceTest {
    @Mock
    private JoueurDao joueurDao;

    @InjectMocks
    private JoueurService joueurService;

    @Test
    void findAllMapsPlayersToDtos() {
        when(joueurDao.findAll()).thenReturn(List.of(joueur(1L, "Lea", null)));

        List<JoueurDto> joueurs = joueurService.findAll();

        assertEquals(1, joueurs.size());
        assertEquals(1L, joueurs.get(0).getId());
        assertEquals("Lea", joueurs.get(0).getPseudo());
    }

    @Test
    void getByIdMapsPlayerToDto() {
        when(joueurDao.findById(1L)).thenReturn(Optional.of(joueur(1L, "Lea", null)));

        JoueurDto joueur = joueurService.getById(1L);

        assertEquals(1L, joueur.getId());
        assertEquals("Lea", joueur.getPseudo());
    }

    @Test
    void getByIdThrowsWhenPlayerDoesNotExist() {
        when(joueurDao.findById(404L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> joueurService.getById(404L));
        assertEquals("Le joueur 404 n'existe pas", exception.getMessage());
    }

    @Test
    void addJoueurIgnoresRequestIdAndReturnsSavedPlayer() {
        when(joueurDao.save(any(Joueur.class))).thenReturn(joueur(2L, "Max", null));

        JoueurDto created = joueurService.addJoueur(dto(99L, "Max", null));

        ArgumentCaptor<Joueur> captor = ArgumentCaptor.forClass(Joueur.class);
        verify(joueurDao).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(2L, created.getId());
        assertEquals("Max", created.getPseudo());
    }

    @Test
    void updateJoueurUsesPathIdInsteadOfRequestId() {
        when(joueurDao.findById(3L)).thenReturn(Optional.of(joueur(3L, "Ancien", null)));
        when(joueurDao.save(any(Joueur.class))).thenReturn(joueur(3L, "Sam", null));

        JoueurDto updated = joueurService.updateJoueur(dto(99L, "Sam", null), 3L);

        ArgumentCaptor<Joueur> captor = ArgumentCaptor.forClass(Joueur.class);
        verify(joueurDao).save(captor.capture());
        assertEquals(3L, captor.getValue().getId());
        assertEquals(3L, updated.getId());
        assertEquals("Sam", updated.getPseudo());
    }

    @Test
    void deleteByIdDeletesExistingPlayer() {
        when(joueurDao.findById(4L)).thenReturn(Optional.of(joueur(4L, "Tom", null)));

        joueurService.deleteById(4L);

        verify(joueurDao).deleteById(4L);
    }

    private Joueur joueur(Long id, String pseudo, String avatar) {
        return new Joueur.Builder()
                .id(id)
                .pseudo(pseudo)
                .avatar(avatar)
                .build();
    }

    private JoueurDto dto(Long id, String pseudo, String avatar) {
        return new JoueurDto.JoueurDtoBuilder()
                .id(id)
                .pseudo(pseudo)
                .avatar(avatar)
                .build();
    }
}