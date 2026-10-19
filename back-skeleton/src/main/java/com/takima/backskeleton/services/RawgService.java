package com.takima.backskeleton.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.takima.backskeleton.DTO.RawgJeuDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

// Appelle l'API publique RAWG (https://rawg.io/apidocs) et convertit ses réponses au format PlayNight
@Service
public class RawgService {
    private static final int NB_RESULTATS = 10;
    // Au-delà, les plateformes suivantes sont ignorées pour garder un texte lisible sur les cartes
    private static final int NB_PLATEFORMES_MAX = 4;

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String url;

    public RawgService(RestTemplateBuilder restTemplateBuilder,
                       @Value("${rawg.api-key}") String apiKey,
                       @Value("${rawg.url}") String url) {
        // Sans délai maximum, une API RAWG très lente bloquerait la requête PlayNight indéfiniment
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey;
        this.url = url;
    }

    // Recherche de jeux par nom : GET {url}/games?search=...
    public List<RawgJeuDto> rechercher(String recherche) {
        if (recherche == null || recherche.isBlank()) {
            throw new IllegalArgumentException("Saisissez le nom d'un jeu à rechercher.");
        }
        // Les {variables} sont encodées par RestTemplate (espaces, accents, &…)
        JsonNode reponse = appeler("/games?key={key}&search={search}&page_size={taille}",
                apiKey, recherche.trim(), NB_RESULTATS);

        List<RawgJeuDto> jeux = new ArrayList<>();
        for (JsonNode jeu : reponse.path("results")) {
            jeux.add(toDto(jeu));
        }
        return jeux;
    }

    // Un jeu précis par son identifiant RAWG : GET {url}/games/{id}
    public RawgJeuDto getById(Long idRawg) {
        try {
            return toDto(appeler("/games/{id}?key={key}", idRawg, apiKey));
        } catch (HttpClientErrorException.NotFound exception) {
            throw new NoSuchElementException("Le jeu " + idRawg + " n'existe pas sur RAWG");
        }
    }

    private JsonNode appeler(String chemin, Object... variables) {
        if (apiKey.isBlank()) {
            throw new RawgIndisponibleException("L'import RAWG n'est pas configuré : ajoutez RAWG_API_KEY dans le fichier .env du back.");
        }
        try {
            return restTemplate.getForObject(url + chemin, JsonNode.class, variables);
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new RawgIndisponibleException("La clé d'API RAWG est refusée : vérifiez RAWG_API_KEY dans le fichier .env du back.");
        } catch (HttpClientErrorException.NotFound exception) {
            throw exception; // traité par l'appelant (jeu inconnu)
        } catch (RestClientException exception) {
            throw new RawgIndisponibleException("RAWG ne répond pas pour le moment, réessayez plus tard.");
        }
    }

    // Un jeu RAWG (en JSON) → RawgJeuDto. path() renvoie un nœud vide au lieu de null si un champ manque.
    private RawgJeuDto toDto(JsonNode jeu) {
        return new RawgJeuDto(
                jeu.path("id").asLong(),
                jeu.path("name").asText(),
                texteOuNull(jeu.path("background_image")),
                plateformes(jeu.path("platforms")),
                texteOuNull(jeu.path("released"))
        );
    }

    // [{"platform": {"name": "PC"}}, {"platform": {"name": "PlayStation 5"}}, …] → "PC, PlayStation 5, …"
    private String plateformes(JsonNode plateformes) {
        List<String> noms = StreamSupport.stream(plateformes.spliterator(), false)
                .map(plateforme -> plateforme.path("platform").path("name").asText())
                .filter(nom -> !nom.isBlank())
                .limit(NB_PLATEFORMES_MAX)
                .toList();
        return noms.isEmpty() ? null : String.join(", ", noms);
    }

    private String texteOuNull(JsonNode valeur) {
        return valeur.isNull() || valeur.isMissingNode() || valeur.asText().isBlank() ? null : valeur.asText();
    }
}
