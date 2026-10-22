package com.takima.backskeleton.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

class AvatarStockageTest {
    @TempDir Path dossier;

    @Test
    void pngEtJpegSontReencodesAvecUnNomSur() throws Exception {
        AvatarStockage stockage = new AvatarStockage(dossier.toString());
        for (String format : new String[]{"png", "jpeg"}) {
            String nom = stockage.enregistrer(photo(format, 40, 30));
            assertTrue(nom.matches("[0-9a-f-]{36}\\.png"));
            BufferedImage image = ImageIO.read(stockage.lire(nom).getInputStream());
            assertEquals(40, image.getWidth());
            assertEquals(30, image.getHeight());
            // Une nouvelle instance retrouve les photos après redémarrage.
            assertTrue(new AvatarStockage(dossier.toString()).lire(nom).contentLength() > 0);
        }
    }

    @Test
    void webpEstDecodeEtConvertiEnPng() throws Exception {
        byte[] webp = Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        AvatarStockage stockage = new AvatarStockage(dossier.toString());
        String nom = stockage.enregistrer(new MockMultipartFile("fichier", "photo.webp", "image/webp", webp));
        assertNotNull(ImageIO.read(stockage.lire(nom).getInputStream()));
    }

    @Test
    void redimensionneLesPhotos() throws Exception {
        AvatarStockage stockage = new AvatarStockage(dossier.toString());
        String nom = stockage.enregistrer(photo("png", 1500, 750));
        BufferedImage image = ImageIO.read(stockage.lire(nom).getInputStream());
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
    }

    @Test
    void refuseLesFichiersInvalidesOuMensongers() throws Exception {
        AvatarStockage stockage = new AvatarStockage(dossier.toString());
        assertThrows(IllegalArgumentException.class, () -> stockage.enregistrer(
                new MockMultipartFile("fichier", "photo.png", "image/png", "<script>alert(1)</script>".getBytes())));
        assertThrows(IllegalArgumentException.class, () -> stockage.enregistrer(
                new MockMultipartFile("fichier", "photo.gif", "image/gif", photo("png", 1, 1).getBytes())));
        assertThrows(IllegalArgumentException.class, () -> stockage.enregistrer(
                new MockMultipartFile("fichier", "photo.jpg", "image/jpeg", photo("png", 1, 1).getBytes())));
        assertThrows(IllegalArgumentException.class, () -> stockage.enregistrer(
                new MockMultipartFile("fichier", "vide.png", "image/png", new byte[0])));
        assertThrows(IllegalArgumentException.class, () -> stockage.enregistrer(photo("png", 4097, 1)));
        try (var fichiers = Files.list(dossier)) { assertEquals(0, fichiers.count()); }
    }

    @Test
    void refusePlusDeDeuxMo() {
        AvatarStockage stockage = new AvatarStockage(dossier.toString());
        assertThrows(AvatarTropVolumineuxException.class, () -> stockage.enregistrer(
                new MockMultipartFile("fichier", "photo.png", "image/png", new byte[AvatarStockage.MAX_OCTETS + 1])));
    }

    @Test
    void neLitAucunCheminArbitraire() throws Exception {
        AvatarStockage stockage = new AvatarStockage(dossier.toString());
        assertThrows(NoSuchElementException.class, () -> stockage.lire("../pom.xml"));
        assertThrows(NoSuchElementException.class, () -> stockage.lire("/etc/passwd"));
        assertThrows(NoSuchElementException.class, () -> stockage.lire("photo.svg"));
        String nom = "00000000-0000-0000-0000-000000000000.png";
        assertThrows(NoSuchElementException.class, () -> stockage.lire(nom));
        Path autre = dossier.resolve("secret.txt");
        Files.writeString(autre, "secret");
        Files.createSymbolicLink(dossier.resolve(nom), autre);
        assertThrows(NoSuchElementException.class, () -> stockage.lire(nom));
    }

    private MockMultipartFile photo(String format, int largeur, int hauteur) throws Exception {
        var sortie = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB), format, sortie));
        return new MockMultipartFile("fichier", "../../nom-dangereux." + format, "image/" + format, sortie.toByteArray());
    }
}
