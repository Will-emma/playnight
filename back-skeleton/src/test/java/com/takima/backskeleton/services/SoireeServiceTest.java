package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DAO.ScoreDao;
import com.takima.backskeleton.DAO.SoireeDao;
import com.takima.backskeleton.DAO.SoireePartieCount;
import com.takima.backskeleton.DTO.PartieCreationDto;
import com.takima.backskeleton.DTO.ScoreCreationDto;
import com.takima.backskeleton.DTO.SoireeCreationDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoireeServiceTest {
    @Mock
    private SoireeDao soireeDao;
    @Mock
    private PartieDao partieDao;
    @Mock
    private ScoreDao scoreDao;
    @Mock
    private JoueurDao joueurDao;
    @Mock
    private JeuDao jeuDao;
    @Mock
    private ClassementService classementService;
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

    // ---------- Création et modification (POST / PUT) ----------

    @Test
    void creerSoireeSavesSoireePartiesAndScores() {
        joueursEnBase(1L, 2L, 3L);
        when(jeuDao.findById(5L)).thenReturn(Optional.of(jeuReel(5L, "Catan", 3, 4)));
        SoireeCreationDto dto = creation(List.of(1L, 2L, 3L), partie(5L, 1L, 2L, 3L));

        SoireeDetailDto result = soireeService.creerSoiree(dto);

        verify(soireeDao).save(any(Soiree.class));
        verify(partieDao).save(any(Partie.class));
        verify(scoreDao, times(3)).save(any(Score.class));
        assertEquals("Chez Léa", result.getLieu());
        assertEquals(3, result.getJoueurs().size());
        assertEquals("Catan", result.getParties().get(0).getJeu().getNom());
        assertEquals(3, result.getParties().get(0).getScores().size());
    }

    @Test
    void creerSoireeRejectsSoireeWithoutPlayer() {
        SoireeCreationDto dto = creation(List.of());

        assertRegleMetier("Une soirée doit avoir au moins un joueur présent", dto);
    }

    @Test
    void creerSoireeRejectsMissingDate() {
        SoireeCreationDto dto = creation(List.of(1L));
        dto.setDate(null);

        assertRegleMetier("La date et le lieu de la soirée sont obligatoires", dto);
    }

    @Test
    void creerSoireeRejectsScoreOfAbsentPlayer() {
        // le joueur 4 a un score mais n'est pas dans les présents
        SoireeCreationDto dto = creation(List.of(1L, 2L, 3L), partie(5L, 1L, 2L, 4L));

        assertRegleMetier("Le joueur 4 n'est pas présent à la soirée", dto);
    }

    @Test
    void creerSoireeRejectsTwoScoresForSamePlayerInOnePartie() {
        SoireeCreationDto dto = creation(List.of(1L, 2L, 3L), partie(5L, 1L, 2L, 1L));

        assertRegleMetier("Le joueur 1 a plusieurs scores dans la même partie", dto);
    }

    @Test
    void creerSoireeRejectsWrongNumberOfPlayersForGame() {
        joueursEnBase(1L, 2L);
        when(jeuDao.findById(5L)).thenReturn(Optional.of(jeuReel(5L, "Catan", 3, 4)));
        SoireeCreationDto dto = creation(List.of(1L, 2L), partie(5L, 1L, 2L));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> soireeService.creerSoiree(dto));

        assertEquals("Le jeu Catan se joue de 3 à 4 joueurs, pas 2", exception.getMessage());
        // la soirée est déjà sauvée à ce moment-là : en vrai, c'est @Transactional qui l'annule
        verify(partieDao, never()).save(any(Partie.class));
    }

    @Test
    void modifierSoireeReplacesOldPartiesWithNewOnes() {
        Soiree existante = new Soiree.Builder()
                .date(LocalDateTime.of(2026, 10, 3, 19, 30))
                .lieu("Ancien lieu")
                .joueurs(List.of())
                .build();
        when(soireeDao.findWithJoueursById(1L)).thenReturn(Optional.of(existante));
        joueursEnBase(1L, 2L);
        when(jeuDao.findById(8L)).thenReturn(Optional.of(jeuReel(8L, "Street Fighter 6", 2, 2)));
        SoireeCreationDto dto = creation(List.of(1L, 2L), partie(8L, 1L, 2L));

        SoireeDetailDto result = soireeService.modifierSoiree(1L, dto);

        verify(partieDao).deleteBySoireeId(1L);
        assertEquals("Chez Léa", result.getLieu());
        assertEquals(2, result.getJoueurs().size());
        assertEquals(1, result.getParties().size());
        assertEquals("Street Fighter 6", result.getParties().get(0).getJeu().getNom());
    }

    @Test
    void modifierSoireeThrowsWhenSoireeDoesNotExist() {
        when(soireeDao.findWithJoueursById(404L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> soireeService.modifierSoiree(404L, creation(List.of(1L))));

        assertEquals("La soirée 404 n'existe pas", exception.getMessage());
        verify(partieDao, never()).deleteBySoireeId(404L);
    }

    // Vérifie qu'une règle métier est refusée avec le bon message, sans rien enregistrer
    private void assertRegleMetier(String messageAttendu, SoireeCreationDto dto) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> soireeService.creerSoiree(dto));

        assertEquals(messageAttendu, exception.getMessage());
        verify(soireeDao, never()).save(any(Soiree.class));
    }

    // Requête de création : soirée "Chez Léa" avec les joueurs présents et les parties données
    private SoireeCreationDto creation(List<Long> joueurIds, PartieCreationDto... parties) {
        SoireeCreationDto dto = new SoireeCreationDto();
        dto.setDate(LocalDateTime.of(2026, 10, 10, 20, 0));
        dto.setLieu("Chez Léa");
        dto.setJoueurIds(joueurIds);
        dto.setParties(List.of(parties));
        return dto;
    }

    // Partie de 30 minutes sur le jeu donné, avec un score par joueur (rang 1, 2, 3... dans l'ordre)
    private PartieCreationDto partie(Long jeuId, Long... joueurIds) {
        List<ScoreCreationDto> scores = new ArrayList<>();
        for (int i = 0; i < joueurIds.length; i++) {
            ScoreCreationDto score = new ScoreCreationDto();
            score.setJoueurId(joueurIds[i]);
            score.setRang(i + 1);
            scores.add(score);
        }
        PartieCreationDto partie = new PartieCreationDto();
        partie.setJeuId(jeuId);
        partie.setDuree(30);
        partie.setScores(scores);
        return partie;
    }

    private void joueursEnBase(Long... ids) {
        for (Long id : ids) {
            when(joueurDao.findById(id)).thenReturn(Optional.of(joueur(id, "Joueur " + id)));
        }
    }

    private Jeu jeuReel(Long id, String nom, int nbJoueursMin, int nbJoueursMax) {
        return new Jeu.Builder()
                .id(id)
                .nom(nom)
                .type(TypeJeu.SOCIETE)
                .nbJoueursMin(nbJoueursMin)
                .nbJoueursMax(nbJoueursMax)
                .build();
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
