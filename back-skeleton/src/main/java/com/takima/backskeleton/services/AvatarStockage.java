package com.takima.backskeleton.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AvatarStockage {
    public static final int MAX_OCTETS = 2 * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.of(
            "png", "image/png", "jpeg", "image/jpeg", "jpg", "image/jpeg", "webp", "image/webp");
    private final Path dossier;

    public AvatarStockage(@Value("${playnight.avatars.directory:./uploads/avatars}") String dossier) {
        this.dossier = Path.of(dossier).toAbsolutePath().normalize();
    }

    public String enregistrer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) throw invalide();
        if (fichier.getSize() > MAX_OCTETS) throw new AvatarTropVolumineuxException();
        try (var flux = fichier.getInputStream()) {
            byte[] octets = flux.readNBytes(MAX_OCTETS + 1);
            if (octets.length > MAX_OCTETS) throw new AvatarTropVolumineuxException();
            BufferedImage image = decoder(octets, fichier.getContentType());
            // Réencodage systématique : retire métadonnées et contenu annexe, et limite le poids servi.
            double facteur = Math.min(1, 1024.0 / Math.max(image.getWidth(), image.getHeight()));
            BufferedImage normalisee = new BufferedImage(Math.max(1, (int) (image.getWidth() * facteur)),
                    Math.max(1, (int) (image.getHeight() * facteur)), BufferedImage.TYPE_INT_ARGB);
            Graphics2D dessin = normalisee.createGraphics();
            try {
                dessin.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                dessin.drawImage(image, 0, 0, normalisee.getWidth(), normalisee.getHeight(), null);
            } finally {
                dessin.dispose();
                image.flush();
            }
            Files.createDirectories(dossier);
            String nom = UUID.randomUUID() + ".png";
            Path temporaire = Files.createTempFile(dossier, "avatar-", ".tmp");
            try {
                if (!ImageIO.write(normalisee, "png", temporaire.toFile())) throw new IOException("Encodeur PNG absent");
                Files.move(temporaire, dossier.resolve(nom), StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporaire);
                normalisee.flush();
            }
            return nom;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer la photo.", e);
        }
    }

    private BufferedImage decoder(byte[] octets, String typeDeclare) {
        try (ImageInputStream entree = ImageIO.createImageInputStream(new ByteArrayInputStream(octets))) {
            var lecteurs = ImageIO.getImageReaders(entree);
            if (!lecteurs.hasNext()) throw invalide();
            ImageReader lecteur = lecteurs.next();
            try {
                String typeReel = TYPES.get(lecteur.getFormatName().toLowerCase(Locale.ROOT));
                if (typeReel == null || !typeReel.equals(typeDeclare)) throw invalide();
                lecteur.setInput(entree, true, true);
                int largeur = lecteur.getWidth(0);
                int hauteur = lecteur.getHeight(0);
                if (largeur < 1 || hauteur < 1 || largeur > 4096 || hauteur > 4096
                        || (long) largeur * hauteur > 16_000_000) {
                    throw new IllegalArgumentException("La photo doit mesurer au maximum 4096 pixels de côté et 16 mégapixels.");
                }
                BufferedImage image = lecteur.read(0);
                if (image == null) throw invalide();
                return image;
            } finally {
                lecteur.dispose();
            }
        } catch (IOException | IndexOutOfBoundsException e) {
            throw invalide();
        }
    }

    public Resource lire(String nom) {
        Path chemin = cheminAutorise(nom);
        if (!Files.isRegularFile(chemin, LinkOption.NOFOLLOW_LINKS)) throw introuvable();
        try {
            return new ByteArrayResource(Files.readAllBytes(chemin));
        } catch (IOException e) {
            throw introuvable();
        }
    }

    // Utilisé uniquement pour annuler un nouveau fichier si la transaction SQL échoue.
    public void annuler(String nom) {
        try {
            Files.deleteIfExists(cheminAutorise(nom));
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'annuler l'import de la photo.", e);
        }
    }

    private Path cheminAutorise(String nom) {
        if (nom == null || !nom.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png")) {
            throw introuvable();
        }
        return dossier.resolve(nom);
    }

    private IllegalArgumentException invalide() {
        return new IllegalArgumentException("Sélectionnez une image JPEG, PNG ou WebP valide dont le contenu correspond au type annoncé.");
    }

    private NoSuchElementException introuvable() { return new NoSuchElementException("Cette photo n'existe pas."); }
}
