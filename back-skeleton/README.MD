# Your backend API

## Set up 
1. Copie-colle le `.env.sample` en `.env`
2. Remplis le fichier `.env` avec les credentials de ton choix
3. Fait un `docker compose up`
4. Rajoute le plugin : [env-file](https://plugins.jetbrains.com/plugin/7861-envfile)
5. Ajoute ton fichier `.env` comme ci-dessous (n'oublie pas d'activer les fichiers cachés)
<img src="img-readme/img.png">
<img src="img-readme/img_1.png">
<img src="img-readme/img_2.png">


### Photos de profil

La galerie Angular contient 12 avatars pixel art originaux générés pour PlayNight,
sans téléchargement de personnages ou de photos provenant de tiers.
`PUT /joueurs/{id}/avatar` reçoit `{ "avatarId": 1 }` (1 à 12).
`POST /joueurs/{id}/avatar` reçoit le champ multipart `fichier`.
Les deux réponses reprennent le DTO joueur existant avec sa nouvelle URL d'avatar.

Les photos JPEG, PNG et WebP sont limitées à 2 Mio, 4096 pixels par côté et
16 mégapixels. Le serveur vérifie le format réel avec ImageIO (plugin TwelveMonkeys
pour WebP), décode puis réencode la première image en PNG, sans métadonnées et
avec un côté maximal de 1024 pixels. Les noms sont des UUID générés par le serveur.
Les photos sont servies uniquement via `GET /joueurs/avatars/{uuid}.png`.
Un échec de transaction SQL annule le nouveau fichier ; les anciennes photos
restent dans le dossier pour éviter d'invalider une URL encore utilisée ailleurs.

Le stockage par défaut est `back-skeleton/uploads/avatars/` lorsque l'API est
lancée depuis `back-skeleton/`. Ce dossier est ignoré par Git et reste présent
après redémarrage. Configurer `AVATAR_STORAGE_DIR` avec un **chemin absolu durable**
pour un lancement indépendant du répertoire courant ou un déploiement.
En conteneur, monter ce dossier sur un volume persistant. Inclure le dossier dans
les sauvegardes avec PostgreSQL. `PUBLIC_API_URL` doit contenir l'URL publique de
l'API (par défaut `http://localhost:8080`). Aucune migration SQL n'est nécessaire.

Chaque lecture de profil, liste, soirée ou classement réutilise `joueur.avatar` ;
les resolvers Angular rechargent les données en naviguant, et le profil affiché
est actualisé dès que l'enregistrement réussit. La prévisualisation reste locale
jusqu'à l'appui sur « Enregistrer » ; « Annuler » n'envoie aucune modification.


### API à jour et démarrage local

Après l'ajout ou la modification de contrôleurs Spring, redémarrer le backend :
un processus Java déjà lancé conserve ses anciennes routes, même si les fichiers
ont été compilés depuis. Une route avatar absente peut produire un `404` sans
origine et un rejet de la requête CORS préalable dans le navigateur. Vérifier
`http://localhost:8080/v3/api-docs` : `/joueurs/{id}/avatar` et
`/joueurs/{id}/classements` doivent être présents. Les contrôleurs correspondants
portent déjà `@CrossOrigin` ; ajouter une autre règle CORS ne met pas à jour une
ancienne instance du serveur.

Sur une base existante, appliquer d'abord `initdb/3_CLASSEMENTS.sql` selon la
procédure du modèle de données. Cela ajoute la table Elo sans réinitialiser
PostgreSQL. Depuis `back-skeleton/`, démarrer ensuite la version compilée avec un
dossier d'avatars stable :

```sh
mvn package -DskipTests
AVATAR_STORAGE_DIR="$PWD/uploads/avatars" java -jar target/playnight-0.0.1-SNAPSHOT.jar
```

Arrêter au préalable l'ancienne instance qui occupe le port 8080. L'URL publique
par défaut est `http://localhost:8080` ; adapter `PUBLIC_API_URL` si nécessaire.
Les chemins `/assets/avatars/avatar-XX.png` de la galerie sont servis par Angular,
et les photos importées sont servies par l'API. Les tests vérifient séparément
la persistance SQL et la récupération des fichiers par une nouvelle instance du
stockage ; le stockage doit rester monté au même emplacement après redémarrage.


### Catalogue de jeux de société

`initdb/4_AJOUT_JEUX_SOCIETE.sql` ajoute les métadonnées facultatives
`description` et `duree_estimee`, puis les quatre jeux suivants :

| Jeu | Joueurs | Durée indicative |
|---|---|---|
| Undercover | 3–12 | 15 min |
| UNO | 2–10 | 20 min |
| Dames (internationales) | 2 | 20 min |
| Échecs (partie rapide) | 2 | 30 min |

Ces estimations dépendent de la variante et du rythme des joueurs. Il s'agit
séparément de jeux de type `SOCIETE`, sans plateforme ni moteur de règles.
Les parties, scores et classements utilisent les écrans et endpoints existants.
Pour Undercover, saisir le camp gagnant au rang 1 et les autres au rang 2 ;
si des équipes sont renseignées, tous leurs membres doivent partager le même
rang. Pour une nulle aux Dames ou aux Échecs, attribuer le même rang aux deux
joueurs. Le calcul Elo existant reste inchangé.

Une base neuve exécute les scripts dans leur ordre habituel. Sur une base
existante, arrêter l'API puis appliquer **uniquement** le nouveau script depuis
`back-skeleton/`, avant de relancer l'API compilée :

```sh
docker compose exec -T database sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < initdb/4_AJOUT_JEUX_SOCIETE.sql
```

Ne pas rejouer les anciens jeux de données ni réinitialiser le volume PostgreSQL.
Le script conserve les jeux présents, leurs identifiants et leurs personnalisations.
Il reconnaît les noms après suppression des espaces extérieurs et normalisation
de la casse et des accents de « Échecs ». Une réexécution ne crée pas de doublon.
Il n'est pas exécuté au démarrage de Spring : un jeu supprimé reste supprimé
après redémarrage. Une réexécution **manuelle** restaure les jeux du catalogue
qui manquent. La suppression existante reste disponible pour un jeu jamais joué ;
un jeu utilisé dans une partie reste protégé par les clés étrangères.

Les quatre couvertures dans `front-skeleton/src/assets/jeux/` sont des images
originales générées pour PlayNight : masque et indices pour Undercover, cartes
colorées aux formes géométriques pour UNO, damier et pions pour Dames, cavalier
et roi pour Échecs. Consigne artistique commune : illustration de couverture
chaleureuse, sans texte ni logo, adaptée au thème sombre et doré. Aucune image
de boîte commerciale n'a été téléchargée.


Les jeux de société historiques disposent également de couvertures originales
créées pour PlayNight : île et village pour Catan, livre et paysage onirique pour
Dixit, cartes et accessoires d'espionnage pour Codenames, port et monuments
antiques pour 7 Wonders. Même direction artistique : illustration chaleureuse,
ombres bleu nuit et lumière dorée, sans texte ni logo commercial.

Sur une base existante, compléter les images manquantes avec :

```sh
docker compose exec -T database sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < initdb/5_IMAGES_JEUX_SOCIETE.sql
```

Ce script renseigne les images nulles, vides ou temporaires (`placehold.co`)
des quatre jeux concernés.
Il conserve les images personnalisées, ne crée aucun jeu et peut être rejoué.
Il est aussi exécuté dans l'ordre habituel à l'initialisation d'une base neuve.
