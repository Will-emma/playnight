# PlayNight

PlayNight est une application web de suivi de soirées jeux, pour les jeux de société comme pour les jeux vidéo : on y enregistre les soirées, les parties jouées et les scores de chaque joueur.
Le projet repose sur un backend Spring Boot (API REST), un frontend Angular et une base PostgreSQL lancée avec Docker.

## Prérequis

- Docker Desktop (lancé)
- Java 17 (JDK) et Maven 3.9+
- Node.js 20+ et npm

## Première installation (une seule fois)

Dans le dossier `back-skeleton/`, créer le fichier `.env` à partir du modèle :

```bash
cd back-skeleton
cp .env.sample .env
```

Puis l'ouvrir et remplir les 3 valeurs, par exemple :

```
DATABASE_USER=playnight
DATABASE_PASSWORD=playnight
DATABASE_NAME=playnight
```

⚠️ Ne refaites pas cette copie ensuite : elle écraserait votre `.env`. Ce fichier reste sur votre machine, il n'est jamais envoyé sur GitHub.

## Lancer le projet

Il faut **2 terminaux**, qui restent ouverts pendant que vous travaillez (dans VS Code, le bouton `+` du panneau Terminal en ouvre un nouveau).

### Terminal 1 : base de données + backend (port 8080)

```bash
cd back-skeleton
docker compose up -d
mvn spring-boot:run
```

Le backend lit tout seul le fichier `.env`. C'est prêt quand la ligne `Started BackSkeletonApplication` s'affiche.
Swagger UI : http://localhost:8080/swagger-ui/index.html

### Terminal 2 : frontend (port 4200)

```bash
cd front-skeleton
npm install
npm start
```

`npm install` n'est utile que la première fois, ou quand les dépendances changent. C'est prêt quand `Compiled successfully` s'affiche.
Application : http://localhost:4200

### Arrêter

`Ctrl + C` dans chaque terminal. La base peut rester allumée ; pour l'arrêter : `docker compose down` (dans `back-skeleton/`).

## En cas de problème

| Symptôme | Cause probable |
|---|---|
| Le front s'affiche mais les listes sont vides | Le backend n'est pas lancé (terminal 1) |
| `password authentication failed for user "${DATABASE_USER}"` | Le fichier `.env` est absent de `back-skeleton/` |
| `Port 8080 was already in use` | Un autre backend tourne déjà : arrêtez-le avec `Ctrl + C` |
| Les tables ou les données ne correspondent pas aux scripts `initdb/` | Les scripts ne s'exécutent qu'à la création de la base : `docker compose down -v` puis `docker compose up -d` (⚠️ efface les données) |

Le guide d'installation détaillé fourni par l'école (IntelliJ, Docker, captures d'écran) se trouve dans [INSTALLATION-ECOLE.md](INSTALLATION-ECOLE.md).
