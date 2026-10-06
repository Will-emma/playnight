package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DAO.SoireeDao;
import com.takima.backskeleton.DAO.SoireePartieCount;
import com.takima.backskeleton.DTO.SoireeDetailDto;
import com.takima.backskeleton.DTO.SoireeDto;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;
import com.takima.backskeleton.models.Soiree;
import com.takima.backskeleton.models.TypeJeu;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoireeServiceTest {
    @Mock
    private SoireeDao soireeDao;
    @Mock
    private PartieDao partieDao;
    @InjectMocks
    private SoireeService soireeService;

    @Test
    void findAllReturnsSummariesAndPartyCounts() {
        Soiree soiree = soiree(1L, "Chez Léa", List.of(joueur(1L, "Léa")));
        when(soireeDao.findAllWithJoueurs()).thenReturn(List.of(soiree));
        when(soireeDao.countPartiesBySoiree()).thenReturn(List.of(partieCount(1L, 3L)));

        List<SoireeDto> result = soireeService.findAll();

        assertEquals(1, result.size());
        assertEquals("Chez Léa", result.get(0).getLieu());
        assertEquals(1L, result.get(0).getJoueurs().get(0).getId());
        assertEquals(3L, result.get(0).getNbParties());
        verify(partieDao, never()).findDetailsBySoireeId(1L);
    }

    @Test
    void getByIdReturnsPartiesWithScoresSortedByRank() {
        Soiree soiree = soiree(1L, "Chez Léa", List.of(joueur(1L, "Léa")));
        Partie partie = mock(Partie.class);
        Jeu jeu = jeu(2L, "Catan", TypeJeu.SOCIETE);
        Score second = score(2L, 2, joueur(2L, "Max"));
        Score first = score(1L, 1, joueur(1L, "Léa"));
        when(soireeDao.findWithJoueursById(1L)).thenReturn(Optional.of(soiree));
        when(partieDao.findDetailsBySoireeId(1L)).thenReturn(List.of(partie));
        when(partie.getId()).thenReturn(3L);
        when(partie.getDuree()).thenReturn(45);
        when(partie.getJeu()).thenReturn(jeu);
        when(partie.getScores()).thenReturn(List.of(second, first));

        SoireeDetailDto result = soireeService.getById(1L);

        assertEquals("Chez Léa", result.getLieu());
        assertEquals(1, result.getJoueurs().size());
        assertEquals(3L, result.getParties().get(0).getId());
        assertEquals("Catan", result.getParties().get(0).getJeu().getNom());
        assertEquals(List.of(1, 2), result.getParties().get(0).getScores().stream()
                .map(score -> score.getRang())
                .toList());
    }

    @Test
    void getByIdThrowsWhenPartyDoesNotExist() {
        when(soireeDao.findWithJoueursById(404L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> soireeService.getById(404L));

        assertEquals("La soirée 404 n'existe pas", exception.getMessage());
        verify(partieDao, never()).findDetailsBySoireeId(404L);
    }

    @Test
    void deleteByIdDeletesExistingSoiree() {
        Soiree soiree = mock(Soiree.class);
        when(soireeDao.findWithJoueursById(4L)).thenReturn(Optional.of(soiree));

        soireeService.deleteById(4L);

        verify(soireeDao).delete(soiree);
    }

    @Test
    void deleteByIdThrowsWhenSoireeDoesNotExist() {
        when(soireeDao.findWithJoueursById(404L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> soireeService.deleteById(404L));

        assertEquals("La soirée 404 n'existe pas", exception.getMessage());
        verify(soireeDao, never()).delete(org.mockito.ArgumentMatchers.any(Soiree.class));
    }

    private Soiree soiree(Long id, String lieu, List<Joueur> joueurs) {
        Soiree soiree = mock(Soiree.class);
        when(soiree.getId()).thenReturn(id);
        when(soiree.getDate()).thenReturn(LocalDateTime.of(2026, 10, 3, 19, 30));
        when(soiree.getLieu()).thenReturn(lieu);
        when(soiree.getJoueurs()).thenReturn(joueurs);
        return soiree;
    }

    private Joueur joueur(Long id, String pseudo) {
        return new Joueur.Builder().id(id).pseudo(pseudo).build();
    }

    private Jeu jeu(Long id, String nom, TypeJeu type) {
        Jeu jeu = mock(Jeu.class);
        when(jeu.getId()).thenReturn(id);
        when(jeu.getNom()).thenReturn(nom);
        when(jeu.getType()).thenReturn(type);
        return jeu;
    }

    private Score score(Long id, int rang, Joueur joueur) {
        Score score = mock(Score.class);
        when(score.getId()).thenReturn(id);
        when(score.getRang()).thenReturn(rang);
        when(score.getJoueur()).thenReturn(joueur);
        return score;
    }

    private SoireePartieCount partieCount(Long soireeId, Long nbParties) {
        return new SoireePartieCount() {
            @Override
            public Long getSoireeId() {
                return soireeId;
            }

            @Override
            public Long getNbParties() {
                return nbParties;
            }
        };
    }
}
