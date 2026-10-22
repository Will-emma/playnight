# Format des DTO – PlayNight

Ce document fixe le JSON échangé entre le backend et le frontend.
Il sert de contrat : chacun peut développer sa partie (back ou front) en parallèle, en s'appuyant sur ces formats.
Il complète le [modèle de données](modele-donnees.md).

## Conventions

- Noms de champs en `camelCase`, identiques aux attributs des entités (`nbJoueursMin`, `numeroEquipe`).
- Dates au format ISO 8601 sans fuseau : `"2026-09-12T20:00:00"` (format natif de `LocalDateTime`).
- `id` toujours présent en lecture ; ignoré à la création (il est généré par la base).
- **Écriture** : un objet lié est désigné par son id (`jeuId`, `joueurIds`…).
- **Lecture** : un objet lié est renvoyé sous forme de **résumé** (voir ci-dessous), suffisant pour l'affichage.
- **Pas d'aller-retour** : une soirée contient ses parties, mais une partie ne contient pas sa soirée. Cela évite les boucles infinies lors de la conversion en JSON.
- Champs facultatifs : renvoyés à `null` quand ils sont vides (`avatar`, `plateforme`, `points`, `numeroEquipe`…).

## Résumés (objets liés en lecture)

```json
// Résumé d'un joueur
{ "id": 1, "pseudo": "Léa", "avatar": "https://..." }

// Résumé d'un jeu
{ "id": 3, "nom": "Catan", "type": "SOCIETE", "image": "https://..." }
```

## Joueurs – `/joueurs`

Lecture et écriture utilisent le même format.

```json
{
  "id": 1,
  "pseudo": "Léa",
  "avatar": "/assets/avatars/avatar-02.png"
}
```

À la création (`POST`), un `avatar` absent ou vide est remplacé par le portrait de la galerie (`/assets/avatars/avatar-01.png` à `avatar-12.png`) que le moins de joueurs portent.

## Jeux – `/jeux`

Lecture et écriture utilisent le même format. `type` vaut `"SOCIETE"` ou `"JEU_VIDEO"`.

```json
{
  "id": 5,
  "nom": "Mario Kart 8 Deluxe",
  "type": "JEU_VIDEO",
  "plateforme": "Nintendo Switch",
  "nbJoueursMin": 1,
  "nbJoueursMax": 4,
  "image": "https://..."
}
```

Filtre par type : `GET /jeux?type=SOCIETE` ou `GET /jeux?type=JEU_VIDEO`.

## Soirées – `/soirees`

### Liste – `GET /soirees`

Sans les parties, pour rester léger.

```json
[
  {
    "id": 1,
    "date": "2026-09-12T20:00:00",
    "lieu": "Chez Léa",
    "joueurs": [
      { "id": 1, "pseudo": "Léa", "avatar": "https://..." },
      { "id": 2, "pseudo": "MaxPower", "avatar": "https://..." }
    ],
    "nbParties": 3
  }
]
```

### Détail – `GET /soirees/{id}`

Avec les parties et leurs scores (triés par rang).

```json
{
  "id": 1,
  "date": "2026-09-12T20:00:00",
  "lieu": "Chez Léa",
  "joueurs": [
    { "id": 1, "pseudo": "Léa", "avatar": "https://..." }
  ],
  "parties": [
    {
      "id": 3,
      "duree": 30,
      "jeu": { "id": 3, "nom": "Codenames", "type": "SOCIETE", "image": "https://..." },
      "scores": [
        { "id": 9, "points": 9, "rang": 1, "numeroEquipe": 1, "joueur": { "id": 1, "pseudo": "Léa", "avatar": "https://..." } },
        { "id": 12, "points": 6, "rang": 2, "numeroEquipe": 2, "joueur": { "id": 3, "pseudo": "Samouraï", "avatar": "https://..." } }
      ]
    }
  ]
}
```

### Création – `POST /soirees`

Toute la soirée est envoyée **en une seule requête**, à la fin du formulaire en étapes : présents, parties et scores.
Le backend enregistre tout dans une même transaction (`@Transactional`) : soit tout est créé, soit rien.

```json
{
  "date": "2026-10-10T20:00:00",
  "lieu": "Chez Léa",
  "joueurIds": [1, 2, 5],
  "parties": [
    {
      "jeuId": 3,
      "duree": 45,
      "scores": [
        { "joueurId": 1, "rang": 1, "points": 10, "numeroEquipe": null },
        { "joueurId": 2, "rang": 2, "points": 8, "numeroEquipe": null },
        { "joueurId": 5, "rang": 3, "points": 4, "numeroEquipe": null }
      ]
    }
  ]
}
```

Réponse : `201 Created` avec la soirée créée, au format du détail.

Contrôles faits par le backend (sinon `400`) :
- au moins un joueur présent ;
- chaque joueur d'un score fait partie des joueurs présents ;
- un joueur n'a qu'un seul score par partie ;
- le nombre de joueurs d'une partie respecte `nbJoueursMin` et `nbJoueursMax` du jeu.

### Modification et suppression

- `PUT /soirees/{id}` : même format que la création.
- `DELETE /soirees/{id}` : supprime aussi les parties et les scores.

## Statistiques

### Profil d'un joueur – `GET /joueurs/{id}/profil`

```json
{
  "joueur": { "id": 1, "pseudo": "Léa", "avatar": "https://..." },
  "nbParties": 5,
  "nbVictoires": 2,
  "jeuxFavoris": [
    { "jeu": { "id": 1, "nom": "Catan", "type": "SOCIETE", "image": "https://..." }, "nbParties": 1 }
  ],
  "historique": [
    {
      "partieId": 3,
      "date": "2026-09-12T20:00:00",
      "jeu": { "id": 3, "nom": "Codenames", "type": "SOCIETE", "image": "https://..." },
      "rang": 1,
      "points": 9
    }
  ]
}
```

### Fiche d'un jeu – `GET /jeux/{id}/fiche`

```json
{
  "jeu": {
    "id": 1, "nom": "Catan", "type": "SOCIETE", "plateforme": null,
    "nbJoueursMin": 3, "nbJoueursMax": 4, "image": "https://..."
  },
  "nbParties": 1,
  "meilleursJoueurs": [
    { "joueur": { "id": 1, "pseudo": "Léa", "avatar": "https://..." }, "nbVictoires": 1 }
  ],
  "parties": [
    { "partieId": 1, "date": "2026-09-12T20:00:00", "duree": 75, "vainqueurs": ["Léa"] }
  ]
}
```

### Classement Elo d'un jeu – `GET /jeux/{id}/classement`

```json
[
  { "joueurId": 1, "pseudo": "Léa", "avatar": "https://...", "elo": 1016, "nbParties": 1, "rang": 1 },
  { "joueurId": 2, "pseudo": "Max", "avatar": null, "elo": 984, "nbParties": 1, "rang": 2 }
]
```

Tri : Elo décroissant, puis pseudo et identifiant. Les joueurs ayant le même
Elo partagent un rang (1, 2, 2, 4…). Un jeu existant sans résultat exploitable
renvoie `[]`, un jeu inexistant renvoie 404. Ce classement est indépendant de
`meilleursJoueurs`, qui reste le classement par victoires de la fiche jeu.
Le calcul et la procédure de migration sont décrits dans [le modèle de données](modele-donnees.md#classement-elo-ticket-24).

### Classement personnel – `GET /joueurs/{id}/classements`

```json
[
  {
    "jeu": { "id": 1, "nom": "Catan", "type": "SOCIETE", "image": "https://..." },
    "elo": 1145, "rang": 2, "nbParties": 8
  }
]
```

Les valeurs proviennent de `classements`, sans recalcul à la lecture. Le rang
compare le joueur à tous les joueurs du même jeu ; les ex aequo partagent leur
rang (1, 2, 2, 4…). Une requête SQL avec `RANK()` récupère tous ses jeux, triés par
nom puis identifiant, sans requête par jeu. Joueur sans classement : `200 []` ;
joueur inexistant : `404`. Les statistiques de victoires restent indépendantes.

### Elo global – `GET /joueurs/{id}/classement-global`

Réponse : `{ "elo": 1088, "rang": 3 }`. Le score global est la moyenne
arithmétique des Elo par jeu ayant au moins une partie classée, arrondie à l'entier
le plus proche (demi-entiers à l'opposé de zéro). Chaque jeu a le même poids,
indépendamment du nombre de parties ; il s'agit d'une synthèse des Elo par jeu.
Le classement global compare ces moyennes arrondies entre tous les joueurs
classés, avec les mêmes rangs partagés (1, 2, 2, 4…). Un joueur sans résultat a
le score initial 1000 et `rang: null` (« Non classé »). Joueur absent : 404.
Une requête d'agrégation et de rang suffit, sans recalculer l'historique ni
modifier les Elo par jeu ou les classements par victoires.

### Progression globale – `GET /joueurs/{id}/progression-elo`

Renvoie `classement` (Elo et rang actuels), `meilleurElo`, `variationTotale`,
`nbParties` et `points`. Chaque point contient `partieId`, `date`, `jeu`, `elo`
et `variation` (différence depuis l'étape précédente). Une série non vide
commence par un point initial à 1000, sans partie, date ni jeu. Sans résultat
exploitable, `points` est vide et le meilleur score est le score initial 1000.

La courbe rejoue en mémoire les parties des jeux du joueur, dans le même ordre
que le calcul Elo, avec le même `EloService`. Elle inclut l'historique des
adversaires, même lorsque le joueur n'a pas participé à leurs autres parties.
Seules les parties exploitables auxquelles il participe ajoutent un point.
À chaque étape, le score est la moyenne arrondie de ses Elo sur les jeux déjà
joués à cet instant. L'axe horizontal représente les étapes, pas une durée
proportionnelle ; les dates réelles sont consultables pour chaque point.

La lecture utilise un instantané PostgreSQL cohérent (`REPEATABLE_READ`) et
n'écrit pas dans la base. Les modifications et suppressions de résultats sont
prises en compte à la prochaine ouverture de la courbe. Le chargement n'est
lancé que lorsque l'utilisateur ouvre la fenêtre. Joueur inexistant : 404.

## Endpoints

| Ressource | Lister | Détail | Créer | Modifier | Supprimer |
|---|---|---|---|---|---|
| Joueurs | `GET /joueurs` | `GET /joueurs/{id}` | `POST /joueurs` | `PUT /joueurs/{id}` | `DELETE /joueurs/{id}` |
| Jeux | `GET /jeux` | `GET /jeux/{id}` | `POST /jeux` | `PUT /jeux/{id}` | `DELETE /jeux/{id}` |
| Soirées | `GET /soirees` | `GET /soirees/{id}` | `POST /soirees` | `PUT /soirees/{id}` | `DELETE /soirees/{id}` |

Les parties et les scores sont gérés à travers leur soirée : pas d'endpoints séparés pour le MVP.

## Erreurs

En cas d'erreur, le backend renvoie un code HTTP adapté et un message en français, que le frontend peut afficher tel quel :

```json
{ "message": "Le joueur 12 n'existe pas" }
```

| Code | Cas |
|---|---|
| `400 Bad Request` | Donnée invalide (champ obligatoire manquant, joueur absent de la soirée…) |
| `404 Not Found` | Élément introuvable |
| `409 Conflict` | Opération refusée par la base (pseudo déjà pris, jeu déjà joué, joueur qui a des scores…) |

Ces réponses sont produites par une classe `@RestControllerAdvice`, commune à toute l'API.

## Classes Java

| DTO | Utilisé pour |
|---|---|
| `JoueurDto` | Joueurs (lecture et écriture) |
| `JoueurResumeDto` | Joueur lié (dans une soirée, un score…) |
| `JeuDto` | Jeux (lecture et écriture) |
| `JeuResumeDto` | Jeu lié (dans une partie, un profil…) |
| `SoireeDto` | Liste des soirées |
| `SoireeDetailDto` | Détail d'une soirée |
| `SoireeCreationDto` | Création et modification d'une soirée |
| `PartieDto`, `ScoreDto` | Parties et scores dans le détail |
| `PartieCreationDto`, `ScoreCreationDto` | Parties et scores dans la création |
| `ProfilJoueurDto` | Profil d'un joueur |
| `FicheJeuDto` | Fiche d'un jeu |
| `ClassementJoueurDto` | Classements Elo d’un joueur pour tous ses jeux |
| `ClassementDto` | Classement Elo d'un jeu (`ClassementElo` côté Angular) |

Côté Angular, chaque DTO a son interface TypeScript équivalente dans `models/`.
