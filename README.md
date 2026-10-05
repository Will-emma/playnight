# PlayNight

PlayNight est une application web de suivi de soirées jeux, pour les jeux de société comme pour les jeux vidéo : on y enregistre les soirées, les parties jouées et les scores de chaque joueur.
Le projet repose sur un backend Spring Boot (API REST), un frontend Angular et une base PostgreSQL lancée avec Docker.

## Prérequis

- Docker Desktop
- Java 17 (JDK) et Maven 3.9+
- Node.js 20+ et npm

## Lancer le projet

### 1. Base de données

```bash
cd back-skeleton
cp .env.sample .env   # puis remplir DATABASE_USER, DATABASE_PASSWORD, DATABASE_NAME
docker compose up -d
```

Au premier lancement, les scripts de `initdb/` créent les tables et insèrent les données.
Pour repartir d'une base propre : `docker compose down -v` puis `docker compose up -d`.

### 2. Backend (port 8080)

```bash
cd back-skeleton
set -a; source .env; set +a   # charge les variables du .env (Git Bash)
mvn spring-boot:run
```

Dans IntelliJ, on peut aussi lancer `BackSkeletonApplication` avec le plugin EnvFile pointant sur `.env`.
Swagger UI : http://localhost:8080/swagger-ui/index.html

### 3. Frontend (port 4200)

```bash
cd front-skeleton
npm install
npm start
```

Application : http://localhost:4200

Le guide d'installation détaillé fourni par l'école (IntelliJ, Docker, captures d'écran) se trouve dans [INSTALLATION-ECOLE.md](INSTALLATION-ECOLE.md).
