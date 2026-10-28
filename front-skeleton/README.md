# FrontSkeleton

## Les installations nécessaire
- Node JS : https://nodejs.org/en/download
- Angular : `npm install -g @angular/cli`

## Avant de lancer le projet

Lancer `npm i`

## Pour lancer le projet

Lancer `npm start` et se rendre sur `http://localhost:4200/`


## Statistiques du profil joueur (ticket #23)

Versions fixées : `ng2-charts@6.0.1` et `chart.js@4.4.9`, avec Angular 17.1.0.
Après récupération du code, utiliser `npm ci`, puis `npm start`.
Ouvrir `/joueurs/{id}` et descendre jusqu'à « Statistiques de jeu ».

Les barres horizontales comptent les rangs 1 de l'historique complet, équipes et
premiers ex æquo compris. Les jeux sont regroupés par identifiant et les jeux
joués sans victoire sont conservés à zéro. Le tri porte sur les victoires, puis
le nom et l'identifiant. Les points ne servent pas à déterminer les victoires.

La courbe compte les participations aux parties pendant les douze derniers mois
calendaires, mois courant compris. Les mois manquants valent zéro. La clé du mois
est extraite de la date locale ISO fournie par l'API, sans conversion UTC. Les
parties hors période restent comptées dans les barres de l'historique complet.
Il s'agit des parties enregistrées : aucun statut de partie terminée n'existe
dans le modèle. Les données proviennent de `GET /joueurs/{id}/profil`, sans nouvel
endpoint, sans requête supplémentaire et sans changement des règles Elo.

Le composant reçoit toujours `profil.historique`, indépendamment des dix lignes
affichées dans l'historique du profil. Il recalcule les données à chaque nouvelle
référence d'historique reçue. Le rendu Chart.js est chargé avec `@defer (on viewport)`
quand la zone approche de l'écran. Seuls les contrôleurs, éléments, échelles et
infobulles nécessaires sont enregistrés. Les tableaux restent disponibles si le
module différé échoue. Ils sont accessibles via « Voir les données des graphiques ».
La courbe Elo SVG et les fonctions d'avatar restent indépendantes.

Validation : `npm test -- --watch=false --browsers=ChromeHeadless` et
`npm run build`. Le navigateur de test se configure avec `CHROME_BIN` si nécessaire.
Les tests utilisent des fixtures et n'écrivent dans aucune base PostgreSQL.
