package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.JoueurDao;
import com.takima.backskeleton.DAO.ScoreDao;
import com.takima.backskeleton.DTO.JeuFavoriDto;
import com.takima.backskeleton.DTO.JoueurDto;
import com.takima.backskeleton.DTO.PartieHistoriqueDto;
import com.takima.backskeleton.DTO.ProfilJoueurDto;
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
import java.util.ArrayList;
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
class JoueurServiceTest {
    @Mock
    private JoueurDao joueurDao;

    @Mock
    private ScoreDao scoreDao;

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
    void addJoueurWithoutAvatarGetsLeastUsedGalleryAvatar() {
        // avatar-01 porté deux fois, avatar-02 une fois : avatar-03 est le premier que personne ne porte
        when(joueurDao.findAll()).thenReturn(List.of(
                joueur(1L, "Léa", "/assets/avatars/avatar-01.png"),
                joueur(2L, "Max", "/assets/avatars/avatar-01.png"),
                joueur(3L, "Sam", "/assets/avatars/avatar-02.png"),
                joueur(4L, "Noa", "https://example.com/photo.png"),
                joueur(5L, "Tom", null)));
        when(joueurDao.save(any(Joueur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        joueurService.addJoueur(dto(null, "Inès", "  "));

        ArgumentCaptor<Joueur> captor = ArgumentCaptor.forClass(Joueur.class);
        verify(joueurDao).save(captor.capture());
        assertEquals("/assets/avatars/avatar-03.png", captor.getValue().getAvatar());
    }

    @Test
    void addJoueurWithoutAvatarReusesGalleryWhenAllAvatarsAreTaken() {
        // Les 12 avatars sont portés, avatar-05 une fois de plus : on repart du plus petit numéro le moins porté
        List<Joueur> joueurs = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            joueurs.add(joueur((long) i, "J" + i, String.format("/assets/avatars/avatar-%02d.png", i)));
        }
        joueurs.add(joueur(13L, "J13", "/assets/avatars/avatar-05.png"));
        when(joueurDao.findAll()).thenReturn(joueurs);
        when(joueurDao.save(any(Joueur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        joueurService.addJoueur(dto(null, "Nouveau", null));

        ArgumentCaptor<Joueur> captor = ArgumentCaptor.forClass(Joueur.class);
        verify(joueurDao).save(captor.capture());
        assertEquals("/assets/avatars/avatar-01.png", captor.getValue().getAvatar());
    }

    @Test
    void addJoueurKeepsGivenAvatar() {
        when(joueurDao.save(any(Joueur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        joueurService.addJoueur(dto(null, "Max", "https://example.com/max.png"));

        ArgumentCaptor<Joueur> captor = ArgumentCaptor.forClass(Joueur.class);
        verify(joueurDao).save(captor.capture());
        assertEquals("https://example.com/max.png", captor.getValue().getAvatar());
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

    @Test
    void getProfilCountsGamesAndWinsAndKeepsHistoryOrder() {
        Joueur lea = joueur(1L, "Léa", null);
        Jeu catan = jeu(1L, "Catan");
        Jeu codenames = jeu(2L, "Codenames");
        Score recent = score(lea, partie(codenames, LocalDateTime.of(2026, 10, 3, 20, 0)), 1, 9);
        Score ancien = score(lea, partie(catan, LocalDateTime.of(2026, 9, 12, 20, 0)), 2, null);
        when(joueurDao.findById(1L)).thenReturn(Optional.of(lea));
        when(scoreDao.findDetailsByJoueurId(1L)).thenReturn(List.of(recent, ancien));

        ProfilJoueurDto profil = joueurService.getProfil(1L);

        assertEquals("Léa", profil.getJoueur().getPseudo());
        assertEquals(2L, profil.getNbParties());
        assertEquals(1L, profil.getNbVictoires());
        List<PartieHistoriqueDto> historique = profil.getHistorique();
        assertEquals(2, historique.size());
        assertEquals(LocalDateTime.of(2026, 10, 3, 20, 0), historique.get(0).getDate());
        assertEquals("Codenames", historique.get(0).getJeu().getNom());
        assertEquals(1, historique.get(0).getRang());
        assertEquals(9, historique.get(0).getPoints());
        assertEquals("Catan", historique.get(1).getJeu().getNom());
        assertEquals(2, historique.get(1).getRang());
        assertNull(historique.get(1).getPoints());
    }

    @Test
    void getProfilRanksFavoriteGamesByGamesPlayedThenByName() {
        Joueur lea = joueur(1L, "Léa", null);
        Jeu catan = jeu(1L, "Catan");
        Jeu codenames = jeu(2L, "Codenames");
        Jeu azul = jeu(3L, "Azul");
        LocalDateTime date = LocalDateTime.of(2026, 10, 3, 20, 0);
        when(joueurDao.findById(1L)).thenReturn(Optional.of(lea));
        when(scoreDao.findDetailsByJoueurId(1L)).thenReturn(List.of(
                score(lea, partie(codenames, date), 1, null),
                score(lea, partie(catan, date), 2, null),
                score(lea, partie(codenames, date), 3, null),
                score(lea, partie(azul, date), 1, null)));

        ProfilJoueurDto profil = joueurService.getProfil(1L);

        List<JeuFavoriDto> favoris = profil.getJeuxFavoris();
        assertEquals(3, favoris.size());
        assertEquals("Codenames", favoris.get(0).getJeu().getNom());
        assertEquals(2L, favoris.get(0).getNbParties());
        // Azul et Catan ont 1 partie chacun : départagés par ordre alphabétique
        assertEquals("Azul", favoris.get(1).getJeu().getNom());
        assertEquals("Catan", favoris.get(2).getJeu().getNom());
        assertEquals(2L, profil.getNbVictoires());
    }

    @Test
    void getProfilOfPlayerWithoutGamesReturnsZerosAndEmptyLists() {
        when(joueurDao.findById(1L)).thenReturn(Optional.of(joueur(1L, "Léa", null)));
        when(scoreDao.findDetailsByJoueurId(1L)).thenReturn(List.of());

        ProfilJoueurDto profil = joueurService.getProfil(1L);

        assertEquals(0L, profil.getNbParties());
        assertEquals(0L, profil.getNbVictoires());
        assertTrue(profil.getJeuxFavoris().isEmpty());
        assertTrue(profil.getHistorique().isEmpty());
    }

    @Test
    void getProfilThrowsWhenPlayerDoesNotExist() {
        when(joueurDao.findById(404L)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> joueurService.getProfil(404L));
        assertEquals("Le joueur 404 n'existe pas", exception.getMessage());
        verify(scoreDao, never()).findDetailsByJoueurId(any());
    }

    private Partie partie(Jeu jeu, LocalDateTime date) {
        Soiree soiree = new Soiree.Builder().date(date).build();
        return new Partie.Builder().soiree(soiree).jeu(jeu).build();
    }

    private Score score(Joueur joueur, Partie partie, int rang, Integer points) {
        return new Score.Builder().joueur(joueur).partie(partie).rang(rang).points(points).build();
    }

    private Jeu jeu(Long id, String nom) {
        return new Jeu.Builder().id(id).nom(nom).type(TypeJeu.SOCIETE).build();
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