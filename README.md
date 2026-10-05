# PlayNight

[![CI](https://github.com/Will-emma/playnight/actions/workflows/ci.yml/badge.svg)](https://github.com/Will-emma/playnight/actions/workflows/ci.yml)

Application web de suivi de soirées jeux entre amis, pour les jeux de société comme pour les jeux vidéo.
PlayNight enregistre les soirées, les parties jouées et les scores, puis en tire un historique et des statistiques par joueur et par jeu.

> Projet scolaire en cours de développement : MVP prévu le 25 octobre 2026, rendu final le 31 octobre 2026.

## Fonctionnalités prévues

- Gestion des joueurs et de la ludothèque, avec un filtre jeu de société / jeu vidéo
- Création d'une soirée : joueurs présents, parties jouées, scores (y compris en équipe)
- Historique des soirées et détail de chaque partie
- Profil joueur et fiche jeu : parties jouées, victoires, meilleurs joueurs

## Stack technique

| Partie | Technologies |
|---|---|
| Backend | Java 17, Spring Boot 3.1, Spring Data JPA, Swagger (springdoc-openapi) |
| Frontend | Angular 17, Angular Material, Bootstrap 5 |
| Base de données | PostgreSQL, lancée avec Docker Compose |

## Prérequis

- [Docker Desktop](https://www.docker.com/products/docker-desktop/), lancé
- Java 17 (JDK) et [Maven](https://maven.apache.org/download.cgi) 3.9 ou plus
- [Node.js](https://nodejs.org/) 20 LTS (recommandé) et npm

## Installation

À faire une seule fois, depuis la racine du projet :

```bash
cd back-skeleton
cp .env.sample .env
```

Ouvrir ensuite `back-skeleton/.env` et renseigner les identifiants de la base locale :

```properties
DATABASE_USER=playnight
DATABASE_PASSWORD=playnight
DATABASE_NAME=playnight
```

Le fichier `.env` reste sur votre machine : il est exclu de Git.

## Lancement

Le projet se lance dans **deux terminaux**, ouverts depuis la racine du projet et laissés ouverts pendant le développement.

**Terminal 1 : base de données et backend**

```bash
cd back-skeleton
docker compose up -d
mvn spring-boot:run
```

Le backend est prêt quand la ligne `Started BackSkeletonApplication` s'affiche. Il lit automatiquement le fichier `.env`.
Dans IntelliJ, il suffit de lancer la classe `BackSkeletonApplication` : le plugin EnvFile n'est plus nécessaire.

**Terminal 2 : frontend**

```bash
cd front-skeleton
npm install
npm start
```

Le frontend est prêt quand `Compiled successfully` s'affiche. `npm install` n'est nécessaire qu'au premier lancement ou après un changement de dépendances.

**Arrêt** : `Ctrl + C` dans chaque terminal, puis `docker compose down` dans `back-skeleton/` pour arrêter la base.

| Service | Adresse |
|---|---|
| Application | http://localhost:4200 |
| API REST | http://localhost:8080 |
| Documentation de l'API (Swagger UI) | http://localhost:8080/swagger-ui/index.html |

## Structure du projet

```
├── back-skeleton/       API Spring Boot
│   ├── initdb/          scripts SQL exécutés à la création de la base
│   └── src/main/java/   controllers → services → DAO → models, DTO
├── front-skeleton/      application Angular
├── docs/                documentation de conception
└── .github/workflows/   intégration continue (GitHub Actions)
```

## Contribuer

La branche `main` est protégée : toute modification passe par une Pull Request.

1. Partir d'une version à jour : `git checkout main` puis `git pull`
2. Créer une branche pour le ticket : `git checkout -b feature/nom-du-ticket`
3. Committer, pousser la branche, puis ouvrir une Pull Request contenant `Closes #<numéro du ticket>`
4. Faire relire la PR par un autre membre de l'équipe ; elle peut être fusionnée une fois approuvée et la CI au vert

Les tâches sont suivies dans les [Issues](https://github.com/Will-emma/playnight/issues) et sur le [tableau du projet](https://github.com/users/Will-emma/projects/3).

## Dépannage

| Problème | Solution |
|---|---|
| Le frontend s'affiche, mais les listes sont vides | Le backend n'est pas lancé : voir le terminal 1. |
| `password authentication failed for user "${DATABASE_USER}"` | Le fichier `back-skeleton/.env` est absent : voir [Installation](#installation). |
| `password authentication failed for user "playnight"` | Le `.env` a été modifié après la création de la base. Recréer la base : `docker compose down -v` puis `docker compose up -d`. |
| `Port 8080 was already in use` | Un autre backend tourne déjà : l'arrêter avec `Ctrl + C`. |
| La base ne reflète pas les scripts de `initdb/` | Les scripts ne s'exécutent qu'à la création de la base. Recréer la base : `docker compose down -v` puis `docker compose up -d`. |

> `docker compose down -v` supprime toutes les données de la base locale, qui est ensuite recréée à partir des scripts de `initdb/`.

## Documentation

- [Modèle de données](docs/modele-donnees.md)
- [Format des DTO (JSON échangé entre le back et le front)](docs/format-dto.md)
- [Guide d'installation fourni par l'école](INSTALLATION-ECOLE.md) (IntelliJ, Docker, captures d'écran)
