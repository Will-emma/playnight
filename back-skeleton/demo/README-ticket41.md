# Démonstration facultative — ticket #41

Ces fichiers ne sont pas des migrations de production. Aucun n'est exécuté au
démarrage. Ils refusent toute base autre que `playnight_demo_ticket41` ou
`playnight_demo_ticket41_test_<suffixe>` (minuscules, chiffres et `_`). Ils ne
touchent ni `playnight`, ni `playnight_demo_ticket16`, ni les fichiers `.env`,
les sauvegardes ou les avatars. Aucun volume ni aucune base n'est supprimé.

## Catalogue et modes représentés

La base initiale apporte quatre jeux de société et quatre jeux vidéo.
Undercover (3–12), UNO (2–10), Dames (2) et Échecs (2), ainsi que leurs images,
proviennent exclusivement de la PR `feature/ajout-jeux-societe`.
Le ticket #41 ne recopie ni ses migrations, ni ses assets, ni ses fonctionnalités.

| Nouveau jeu | Limites stockées | Mode représenté |
| --- | --- | --- |
| Overcooked! 2 | 1–4 | Local/en ligne ; la démo utilise le versus 2 contre 2 |
| It Takes Two | 2–2 | Coopération à deux ; conservé sans partie |
| Fortnite | 2–2 | Écran partagé sur PlayStation/Xbox ; conservé sans partie |
| EA Sports FC 25 | 1–4 | Coup d'envoi local PS4/PS5 ; la démo utilise deux équipes de deux |

Les limites désignent des joueurs humains du mode indiqué. Fortnite n'est pas
limité globalement à deux joueurs : son lobby Battle Royale et ses autres modes
ne sont pas modélisés par cette fiche de démo. FC 25 peut se jouer seul contre
l'ordinateur ; les modes Clubs jusqu'à 22 joueurs ne sont pas représentés.
Le mode retenu est précisé dans la plateforme et la description des nouveaux jeux.
Il n'y a pas de faux vainqueur compétitif dans It Takes Two, exclusivement coopératif.

Sources officielles consultées le 24 octobre 2026 :

- [Overcooked! 2 : 1–4 joueurs](https://team17.helpshift.com/hc/en/4-overcooked-franchise/faq/86-overcooked-2-crossplay/)
- [Overcooked! 2 : mode versus](https://www.team17.com/news/the-a-z-of-overcooked-2)
- [It Takes Two : coopération à deux](https://help.ea.com/en/articles/it-takes-two/it-takes-two/how-to-play/)
- [Fortnite : deuxième joueur en écran partagé](https://www.epicgames.com/help/c-34254770/a10401868)
- [FC 25 : 1–4 joueurs locaux, jusqu'à 22 en ligne](https://store.playstation.com/en-us/product/UP0006-PPSA20049_00-25STANDARDBUNDLE/)

Seuls les jeux vidéo absents sont insérés, avec une URL `placehold.co`. Aucune
image existante, même vide, n'est remplacée. Aucun appel RAWG ni clé API n'est
nécessaire. Les images externes nécessitent Internet et leur disponibilité
ne constitue pas un stockage local garanti.

## Prérequis et lancement reproductible

Depuis la racine `playnight/`, Docker Desktop et le conteneur PostgreSQL
`playnight_database` doivent fonctionner. Les commandes utilisent le compte
déjà configuré dans le conteneur sans afficher ses identifiants.

```bash
bash back-skeleton/demo/test-ticket41.sh playnight_demo_ticket41
```

Le lanceur exige une base **inexistante** et la crée. Si elle existe, il s'arrête
sans la réinitialiser. Par défaut, sans argument, il crée une nouvelle base
`playnight_demo_ticket41_test_<date_heure>`, conservée après le test.

Il applique dans cette base uniquement :

1. `initdb/1_TABLES.sql`, `2_DEFAULT_ENTRIES.sql`, `3_CLASSEMENTS.sql`.
2. Les migrations société `4_AJOUT_JEUX_SOCIETE.sql` et
   `5_IMAGES_JEUX_SOCIETE.sql`, lues avec `git show` au commit `7bbdb6d`.
3. Le script facultatif `ticket41.sql`, puis sa réexécution et ses vérifications.

Les migrations externes sont envoyées à PostgreSQL sans être copiées dans cette
branche. Après fusion de la PR société, on peut utiliser sa version dans `main` :

```bash
TICKET41_SOCIETE_REF=main bash back-skeleton/demo/test-ticket41.sh
```

Le conteneur est configurable via `TICKET41_CONTAINER`. Aucun script du ticket
#16 n'est exécuté. Une soirée non identifiée ayant déjà exactement la même date
et le même lieu provoque un refus, sans modification de cette soirée.

Si Docker est indisponible, le même lanceur accepte un serveur PostgreSQL local
**isolé**, déjà démarré. `TICKET41_PG_BIN` désigne son répertoire de binaires ;
les variables PostgreSQL habituelles choisissent explicitement son port/compte :

```bash
TICKET41_PG_BIN=/chemin/vers/postgresql/bin \
PGHOST=127.0.0.1 PGPORT=55441 PGUSER=postgres \
bash back-skeleton/demo/test-ticket41.sh playnight_demo_ticket41
```

Le garde-fou sur le nom de base et le refus d'écraser une base existante restent
identiques. Ne pas installer ni démarrer ce serveur sur le port de la base principale.

Pour rejouer le script seul dans une base déjà préparée, depuis `back-skeleton/demo/`
avec un client PostgreSQL local et un compte autorisé :

```bash
psql -X -v ON_ERROR_STOP=1 -d playnight_demo_ticket41 -f ticket41.sql
psql -X -v ON_ERROR_STOP=1 -d playnight_demo_ticket41 -f ticket41-verification.sql
```

Avec Docker uniquement, développer l'include avant envoi (aucun fichier copié) :

```bash
python3 -c 'from pathlib import Path; p=Path("back-skeleton/demo"); print((p/"ticket41.sql").read_text().replace("\\ir ticket41-verification.sql", (p/"ticket41-verification.sql").read_text()))' |
  docker exec -i playnight_database sh -c 'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d playnight_demo_ticket41'
```

## Scénarios et résultat attendu

Les six joueurs initiaux sont réutilisés sans modifier leur pseudo ou leur avatar.
Ils participent aux quatre nouvelles soirées ; seules les personnes ayant joué
une partie y ont un score.

| Date locale | Lieu | Parties |
| --- | --- | --- |
| 9 octobre 2026, 20 h | Vendredi chez Léa | Catan, UNO |
| 11 octobre 2026, 15 h | Dimanche chez Max | Catan, Undercover (4 civils contre 2 infiltrés) |
| 17 octobre 2026, 16 h | Tournoi du samedi | Catan, Échecs, Overcooked! 2 (2 contre 2) |
| 23 octobre 2026, 20 h 30 | Vendredi manettes | Catan (rangs 1, 2, 2, 4), FC 25 (match nul 3–3 en 2 contre 2) |

Sur les seules données initiales et les prérequis société :

- **16 jeux : 8 société et 8 vidéo**, 6 joueurs, 7 soirées, 17 parties, 71 scores.
- **3 jeux sans partie** : Dames, Fortnite et It Takes Two.
- Catan est le plus joué : 5 parties et 5 victoires de Léa, victoire initiale comprise.
- Undercover et Overcooked ont des équipes gagnantes/perdantes ; FC 25 représente
  deux équipes ex æquo au premier rang. Selon la convention actuelle `rang = 1`,
  chaque membre de ces deux équipes compte comme vainqueur. Aucun changement métier.
- Septembre et octobre fournissent des points pour les futurs graphiques ; les
  autres mois sans partie doivent rester à zéro. Dates SQL locales, sans UTC.

Une victoire compte pour chaque score `rang = 1`, jamais selon le seul total de
points : à UNO, moins de points est meilleur. L'Elo reste calculé par le service
existant au démarrage de l'API. Aucune valeur Elo n'est écrite par ce script.

## Réexécution et conservation

Le schéma dédié `demo_ticket41` identifie les quatre jeux vidéo, quatre soirées
et neuf parties par des clés stables et les vrais identifiants générés.
Renommer une soirée ne la duplique pas. La réexécution ne réécrit ni ses
résultats ni ses participants. La suppression volontaire d'une fixture enlève
son entrée du manifeste par cascade : une exécution **manuelle** ultérieure
peut alors la recréer ; aucun réensemencement automatique n'existe.

Une incohérence détectée annule la transaction complète. Si des limites d'un
jeu existant diffèrent du mode prévu, le script refuse de les écraser. Il refuse
aussi les noms de jeux ambigus. Les résultats modifiés manuellement ne sont pas
restaurés ; certaines assertions de scénario pourront alors échouer.

Les totaux exacts et les trois jeux sans partie sont garantis sur la base neuve
du lanceur. Sur une base déjà enrichie, les données supplémentaires sont
préservées : on ne supprime jamais des parties pour imposer ces totaux.

Le lanceur contrôle les totaux, les équipes, les ex æquo, la conservation de
chaque ligne initiale, la réexécution (séquences comprises), une image changée,
un lieu renommé, un score modifié et le rejet d'une équipe incohérente. Il ne démarre pas l'API
et ne touche pas au processus éventuellement utilisé par une autre démonstration.

## Vérification dans le navigateur

Le frontend actuel appelle explicitement `http://localhost:8080`. Pour examiner
cette démo, arrêter soi-même l'API existante puis démarrer une API sur cette
base uniquement, depuis `back-skeleton/`, sans modifier `.env` :

```bash
DATABASE_NAME=playnight_demo_ticket41 \
AVATAR_STORAGE_DIR=/tmp/playnight-ticket41-api-avatars \
mvn spring-boot:run
```

Si le PostgreSQL isolé utilise le port `55441` (repli local sans Docker), utiliser
plutôt les paramètres explicites suivants. Ils ne modifient aucun fichier :

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:55441/playnight_demo_ticket41 \
SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD= \
DATABASE_NAME=playnight_demo_ticket41 \
AVATAR_STORAGE_DIR=/tmp/playnight-ticket41-api-avatars \
mvn spring-boot:run
```

Une API de contrôle peut être lancée en parallèle sur un autre port en ajoutant
`-Dspring-boot.run.arguments=--server.port=18441`. Elle permet les vérifications
REST sans arrêter l'API habituelle, mais le frontend actuel continue d'appeler
le port `8080` : elle ne suffit donc pas seule pour la navigation Angular.

Le dossier temporaire isole le stockage de ce processus ; aucun avatar n'y est
importé par les scripts. Ne pas utiliser les fonctions de modification d'avatar
pendant ce test. Les avatars prédéfinis des joueurs restent ceux du frontend.

Puis, depuis `front-skeleton/` : `npm start`. Ouvrir :

- `http://localhost:4200/jeux` : catalogue et filtres 8 société / 8 vidéo.
- `http://localhost:4200/soirees` : nouvelles soirées et leurs résultats.
- Fiche Catan : cinq parties ; profil Léa : victoires et progression Elo.
- Fiches Dames, Fortnite, It Takes Two : aucune partie.
- Parties Overcooked / Undercover : équipes ; Catan du 23 octobre : `1, 2, 2, 4`.
- API : `http://localhost:8080/jeux` puis `/jeux/<id>/fiche` et `/jeux/<id>/classement`.

Les identifiants sont générés : les liens des cartes donnent les bonnes URL.
Les images de société locales nécessitent les assets de leur PR indépendante ;
elles ne peuvent pas s'afficher sur cette branche tant que ces assets ne sont
pas disponibles. Les nouvelles images placeholder et les URL RAWG sont externes.
La page complète des victoires (#16) et les graphiques (#23) ne sont disponibles
qu'après fusion de leurs PR ; les données seront déjà compatibles.

Pour revenir à l'environnement habituel : arrêter cette API, puis la relancer
sans la variable temporaire `DATABASE_NAME`, avec la commande habituelle.
Une affectation devant la commande ne change ni le shell parent ni `.env`.
Ne supprimer aucune base ni aucun volume pour revenir à cet environnement.
