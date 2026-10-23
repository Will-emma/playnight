package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JeuDao;
import com.takima.backskeleton.DAO.PartieDao;
import com.takima.backskeleton.DAO.ScoreDao;
import com.takima.backskeleton.DAO.ClassementVictoiresProjection;
import com.takima.backskeleton.DTO.FicheJeuDto;
import com.takima.backskeleton.DTO.JeuDto;
import com.takima.backskeleton.DTO.MeilleurJoueurDto;
import com.takima.backskeleton.DTO.RawgImportDto;
import com.takima.backskeleton.DTO.RawgJeuDto;
import com.takima.backskeleton.models.Jeu;
import com.takima.backskeleton.models.Joueur;
import com.takima.backskeleton.models.Partie;
import com.takima.backskeleton.models.Score;
import com.takima.backskeleton.models.Soiree;
import com.takima.backskeleton.models.TypeJeu;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JeuServiceTest {
    @Mock
    private JeuDao jeuDao;

    @Mock
    private PartieDao partieDao;

    @Mock
    private RawgService rawgService;

    @Mock
    private ScoreDao scoreDao;

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
    void importerDepuisRawgSavesVideoGameWithRawgDataAndChosenPlayerCount() {
        when(rawgService.getById(3498L)).thenReturn(new RawgJeuDto(
                3498L, "Grand Theft Auto V", "https://example.com/gta.jpg", "PC, PlayStation 5", "2013-09-17"));
        when(jeuDao.existsByNomIgnoreCase("Grand Theft Auto V")).thenReturn(false);
        when(jeuDao.save(any(Jeu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JeuDto importe = jeuService.importerDepuisRawg(3498L, importDto(1, 4));

        assertEquals("Grand Theft Auto V", importe.getNom());
        assertEquals(TypeJeu.JEU_VIDEO, importe.getType());
        assertEquals("PC, PlayStation 5", importe.getPlateforme());
        assertEquals("https://example.com/gta.jpg", importe.getImage());
        assertEquals(1, importe.getNbJoueursMin());
        assertEquals(4, importe.getNbJoueursMax());
    }

    @Test
    void importerDepuisRawgRefusesGameAlreadyInLibrary() {
        when(rawgService.getById(3498L)).thenReturn(new RawgJeuDto(3498L, "Rocket League", null, null, null));
        when(jeuDao.existsByNomIgnoreCase("Rocket League")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> jeuService.importerDepuisRawg(3498L, importDto(2, 8)));
        verify(jeuDao, never()).save(any());
    }

    @Test
    void importerDepuisRawgRefusesInvalidPlayerCountWithoutCallingRawg() {
        assertThrows(IllegalArgumentException.class, () -> jeuService.importerDepuisRawg(1L, importDto(0, 4)));
        assertThrows(IllegalArgumentException.class, () -> jeuService.importerDepuisRawg(1L, importDto(4, 2)));
        assertThrows(IllegalArgumentException.class, () -> jeuService.importerDepuisRawg(1L, importDto(null, 4)));
        verify(rawgService, never()).getById(any());
        verify(jeuDao, never()).save(any());
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

    @Test
    void getFicheCountsGamesAndListsWinnersOfEachGame() {
        Joueur lea = joueur(1L, "Léa");
        Joueur tom = joueur(2L, "Tom");
        Partie recente = partie(LocalDateTime.of(2026, 10, 3, 20, 0), 60);
        ajouterScore(recente, tom, 1);
        ajouterScore(recente, lea, 2);
        Partie ancienne = partie(LocalDateTime.of(2026, 9, 12, 20, 0), 75);
        ajouterScore(ancienne, lea, 1);
        ajouterScore(ancienne, tom, 2);
        when(jeuDao.findById(1L)).thenReturn(Optional.of(jeu(1L, "Catan", TypeJeu.SOCIETE)));
        when(partieDao.findDetailsByJeuId(1L)).thenReturn(List.of(recente, ancienne));

        FicheJeuDto fiche = jeuService.getFiche(1L);

        assertEquals("Catan", fiche.getJeu().getNom());
        assertEquals(2L, fiche.getNbParties());
        assertEquals(LocalDateTime.of(2026, 10, 3, 20, 0), fiche.getParties().get(0).getDate());
        assertEquals(60, fiche.getParties().get(0).getDuree());
        assertEquals(List.of("Tom"), fiche.getParties().get(0).getVainqueurs());
        assertEquals(List.of("Léa"), fiche.getParties().get(1).getVainqueurs());
    }

    @Test
    void getFicheUsesAggregatedRankingAndPreservesHistory() {
        Joueur lea = joueur(1L, "Léa");
        Joueur tom = joueur(2L, "Tom");
        Joueur alex = joueur(3L, "Alex");
        Partie partie1 = partie(LocalDateTime.of(2026, 10, 3, 20, 0), 30);
        ajouterScore(partie1, tom, 1);
        Partie partie2 = partie(LocalDateTime.of(2026, 10, 3, 21, 0), 30);
        ajouterScore(partie2, tom, 1);
        ajouterScore(partie2, lea, 2);
        Partie partie3 = partie(LocalDateTime.of(2026, 10, 4, 20, 0), 30);
        ajouterScore(partie3, lea, 1);
        ajouterScore(partie3, alex, 1); // ex aequo : les deux gagnent
        when(jeuDao.findById(1L)).thenReturn(Optional.of(jeu(1L, "Catan", TypeJeu.SOCIETE)));
        when(partieDao.findDetailsByJeuId(1L)).thenReturn(List.of(partie1, partie2, partie3));
        when(scoreDao.findClassementVictoiresByJeuId(1L)).thenReturn(List.of(
                victoire(2L, "Tom", 2L), victoire(3L, "Alex", 1L), victoire(1L, "Léa", 1L)));

        FicheJeuDto fiche = jeuService.getFiche(1L);

        List<MeilleurJoueurDto> classement = fiche.getMeilleursJoueurs();
        assertEquals(3, classement.size());
        assertEquals("Tom", classement.get(0).getJoueur().getPseudo());
        assertEquals(2L, classement.get(0).getNbVictoires());
        // Alex et Léa ont 1 victoire chacun : départagés par ordre alphabétique
        assertEquals("Alex", classement.get(1).getJoueur().getPseudo());
        assertEquals("Léa", classement.get(2).getJoueur().getPseudo());
        assertEquals(List.of("Alex", "Léa"), fiche.getParties().get(2).getVainqueurs());
    }

    @Test
    void getFicheOfGameNeverPlayedReturnsEmptyLists() {
        when(jeuDao.findById(1L)).thenReturn(Optional.of(jeu(1L, "Catan", TypeJeu.SOCIETE)));
        when(partieDao.findDetailsByJeuId(1L)).thenReturn(List.of());

        FicheJeuDto fiche = jeuService.getFiche(1L);

        assertEquals(0L, fiche.getNbParties());
        assertTrue(fiche.getParties().isEmpty());
        assertTrue(fiche.getMeilleursJoueurs().isEmpty());
    }

    @Test
    void getFicheThrowsWhenGameDoesNotExist() {
        when(jeuDao.findById(404L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> jeuService.getFiche(404L));
        assertEquals("Le jeu 404 n'existe pas", exception.getMessage());
        verify(partieDao, never()).findDetailsByJeuId(any());
    }

    @Test
    void classementVictoiresMapsProjectionWithoutLoadingHistory() {
        when(jeuDao.findById(42L)).thenReturn(Optional.of(jeu(42L, "Catan", TypeJeu.SOCIETE)));
        when(scoreDao.findClassementVictoiresByJeuId(42L)).thenReturn(List.of(victoire(7L, "Léa", 3L)));
        var classement = jeuService.getClassementVictoires(42L);
        assertEquals(7L, classement.get(0).getJoueur().getId());
        assertEquals("Léa", classement.get(0).getJoueur().getPseudo());
        assertEquals("avatar.png", classement.get(0).getJoueur().getAvatar());
        assertEquals(3L, classement.get(0).getNbVictoires());
        verify(partieDao, never()).findDetailsByJeuId(any());
    }

    @Test
    void classementVictoiresOfExistingGameCanBeEmpty() {
        when(jeuDao.findById(42L)).thenReturn(Optional.of(jeu(42L, "Catan", TypeJeu.SOCIETE)));
        assertTrue(jeuService.getClassementVictoires(42L).isEmpty());
    }

    @Test
    void classementVictoiresMissingGameDoesNotQueryScores() {
        assertThrows(NoSuchElementException.class, () -> jeuService.getClassementVictoires(404L));
        verify(scoreDao, never()).findClassementVictoiresByJeuId(any());
    }

    private ClassementVictoiresProjection victoire(Long id, String pseudo, Long nombre) {
        return new ClassementVictoiresProjection() {
            public Long getJoueurId() { return id; }
            public String getPseudo() { return pseudo; }
            public String getAvatar() { return "avatar.png"; }
            public Long getNbVictoires() { return nombre; }
        };
    }

    private Partie partie(LocalDateTime date, int duree) {
        Soiree soiree = new Soiree.Builder().date(date).build();
        return new Partie.Builder().soiree(soiree).duree(duree).build();
    }

    private void ajouterScore(Partie partie, Joueur joueur, int rang) {
        partie.getScores().add(new Score.Builder().partie(partie).joueur(joueur).rang(rang).build());
    }

    private Joueur joueur(Long id, String pseudo) {
        return new Joueur.Builder().id(id).pseudo(pseudo).build();
    }

    private RawgImportDto importDto(Integer min, Integer max) {
        RawgImportDto importDto = new RawgImportDto();
        importDto.setNbJoueursMin(min);
        importDto.setNbJoueursMax(max);
        return importDto;
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
