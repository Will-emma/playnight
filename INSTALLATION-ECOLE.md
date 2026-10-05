# Set up cours Java Spring Boot

## I. Installation
**2 méthodes d'installation sont possibles : par téléchargements Internet ou par Clé USB**
### Windows
#### Par téléchargements Internet :
- Docker : https://docs.docker.com/desktop/install/windows-install/
- IntelliJ : https://www.jetbrains.com/fr-fr/idea/download/#section=windows

### Mac
Pour les puces M1 ou M2, choisis *Apple Chip* & *(Apple Silicon)* <br>
Pour les puces Intel, choisis *Intel Chip* & *(Intel)*

#### Par téléchargements Internet :
- Docker  : https://docs.docker.com/desktop/install/mac-install/
- IntelliJ : https://www.jetbrains.com/fr-fr/idea/download/#section=mac

### Linux
#### Commandes pour les installations :
- Docker : <br>
  - `curl -fsSL https://get.docker.com -o get-docker.sh` <br>
  - `sudo sh get-docker.sh`
  - `sudo usermod -aG docker $USER`

- IntelliJ : <br>
  - `sudo snap install intellij-idea-ultimate --classic`

## II. Set up IntelliJ
### 1. Création compte
IntelliJ est un IDE super complet et est devenu un indispensable pour le développement de projet Java. <br>

Avec ses recherches de fichiers optimisées, ses auto-complétions et ses raccourcis à gogo, IntelliJ améliore grandement l'expérience de développement.
Le Graal étant que Maven y est de base intégré et que tu puisses
télécharger n'importe quelle version de Java directement depuis ton IDE (non ce n'est pas un rêve !)<br>

Cerise sur le gateau grâce à ton compte EPF, tu peux gratuitement obtenir une licence de la version payante (Ultimate).
Elle est essentielle au bon déroulement du TP car, avec cette dernière, tu pourras notamment visualiser ta base de données et interagir avec elle directement depuis IntelliJ. <br>
Pour te créer un compte, suis ce lien et effectue les actions détaillées ci-dessous : https://www.jetbrains.com/fr-fr/idea/

<p align="center">
<img src="img-readme/main-readme/img.png" width="500"/>
<img src="img-readme/main-readme/img2.png" width="500"/>
</p>

### 2. Licence IntelliJ
Pour récupérer une licence gratuite utilise ce lien : https://jetbrains.com/shop/eform/students

### 3. Lancer IntelliJ
Une fois ta licence récupérée, lance l'IDE et relie ton compte
<p align="center">
<img src="img-readme/main-readme/img4.png" width="600"/>
<img src="img-readme/main-readme/img3.png" width="600"/>
</p>

### 4. Clonage du projet
Pour récupérer le repository GitLab du TP, lance `git clone https://gitlab.takima.io/formation-dev-web/skeleton-web-app-school.git` dans le dossier de ton choix

Tu y trouveras toutes les ressources nécessaires à la suite du TP.

Dans IntelliJ, ouvre dans un premier temps uniquement le dossier `back-skeleton`.
<p align="center">
<img src="img-readme/main-readme/img5.png" width="400"/>
</p>

### 5. Téléchargement de Java 17
Depuis IntelliJ, tu peux directement télécharger Java 17 : <br>
<p align="center">
<img src="img-readme/main-readme/img7.png" width="300"/>
<img src="img-readme/main-readme/img8.png" width="300"/>
<img src="img-readme/main-readme/img9.png" width="600"/>
<img src="img-readme/main-readme/img10.png" width="600"/>
</p>

### 6. Ouvrir un terminal dans IntelliJ
Il est très utile d'avoir un terminal ouvert pour pouvoir interagir avec le programme en ligne de commande.
<p align="center">
<img src="img-readme/main-readme/img11.png" width="600"/>
</p>
<br>
Tu peux également changer de Shell (Surtout utile pour les Windows)<br>
<br>
<p align="center">
<img src="img-readme/main-readme/img12.png" width="500"/>
</p>
<br>

Sélectionne *Git Bash*
<p align="center">
<img src="img-readme/main-readme/img13.png" width="500"/>
</p>

## III. Set up de la BDD via Docker
### 1. Docker
Après avoir télechargé docker, lance le Docker Desktop en cliquant sur l'icône. Tu dois obtenir l'écran suivant :
<p align="center">
<img src="img-readme/main-readme/img14.png" width="800"/>
</p>
Ferme et ré-ouvre ton application IntelliJ pour que les changements effectués par l'installation de Docker soit pris en compte.
<br>

Pour s'assurer que ton install Docker a bien fonctionné, lance la commande suivante dans ton terminal :
`docker run -d -p 80:80 docker/getting-started`

Tu dois obtenir ce résultat :
<br>
<p align="center">
<img src="img-readme/main-readme/img15.png" width="600"/>
</p>

### 2. Définition des variables d'environnement
1. Copie-colle le `.env.sample` en `.env`
2. Remplis le fichier `.env` avec les credentials de ton choix. Ce sont les accès de ta bdd.

<img src="img-readme/main-readme/img36.png" width="300"/>

Il est important que ces variables restent privées. Il ne faut pas les push avec le reste de ton code :

3. Vérifie que le `.env` est bien dans le fichier .gitignore

### 3. Lancement de la BDD
Dans un terminal, place-toi si tu n'y es pas déjà à la racine du dossier back-skeleton.
<p align="center">
<img src="img-readme/main-readme/img33.png" width="700"/>
</p>

Tu vas maintenant lancer le container de ta bdd. Pour ce faire, lance la commande `docker compose up -d`

Tu dois obtenir ce résultat :
<p align="center">
<img src="img-readme/main-readme/img34.png" width="700"/>
</p>

Voilà ! Ta base de données est créée, mais c'est plus sympa si on peut la voir...

### 4. Afficher la BDD dans IntelliJ
<em>Attention, cette étape ne fonctionne qu'avec la version "Ultimate" de IntelliJ. Pour rappel, tu peux la demander gratuitement en tant qu'étudiant.</em>

Effectue les étapes suivantes :
<p align="center">
<img src="img-readme/main-readme/img16.png" width="700"/>
<img src="img-readme/main-readme/img17.png" width="700"/>
</p>

Installe les drivers si besoin :
<p align="center">
<img src="img-readme/main-readme/img18.png" width="500"/>
</p>

Saisie les infos (1-3), puis test la connexion à la bdd (4)
<p align="center">
<img src="img-readme/main-readme/img19.png" width="500"/>
<img src="img-readme/main-readme/img20.png"/>
</p><br> 

Si c'est valide, clique sur *Apply* (5) puis *OK*

### 5. Initialisation de la BDD
C'est cool d'avoir une BDD qui fonctionne mais c'est encore plus cool quand on peut lui insérer des données en 2 clics.
Il se trouve qu'il y a des script de peuplement SQL déjà tout prêts qui n'attendent qu'à être lancés :
<p align="center">
<img src="img-readme/main-readme/img21.png" width="500"/>
</p>
Clic droit + run : 
<p align="center">
<img src="img-readme/main-readme/img22.png" width="600"/>
<img src="img-readme/main-readme/img23.png" width="400"/>
</p>

Tadaaaam (j'avoue un peu plus que 2 clics)
<p align="center">
<img src="img-readme/main-readme/img24.png" width="500"/>
</p>

## IV. Run du projet (c'est bientôt fini promis !)
Ce projet utilise Maven, qui permet de déclarer et gérer toutes ses dépendances grâce à un fichier *pom.xml* <br>

Il faut donc que l'IDE le détecte en tant que projet Maven. Pour ce faire, clique-droit sur le pom.xml et sélectionne l'option suivante :
<p align="center">
<img src="img-readme/main-readme/img35.png" width="200"/>
</p>

Rajoute le plugin : [env-file](https://plugins.jetbrains.com/plugin/7861-envfile)

Tu peux maintenant run ton projet ! <br>

**Deux** façons de faire :
<p align="center">
<img src="img-readme/main-readme/img25.png" width="600"/>
</p>

**OU**
<p align="center">
<img src="img-readme/main-readme/img26.png" width="600"/>
</p>

Ça ouvre :
<p align="center">
<img src="img-readme/main-readme/img27.png" width="600"/>
</p>
Sélectionne le Java 17 préalablement téléchargé et ajoute l'option `-Xms256m -Xmx256m` (c'est pour éviter que IntelliJ mange toute ta RAM)<br>
<br>
<p align="center">
<img src="img-readme/main-readme/img28.png" width="600"/>
<img src="img-readme/main-readme/img29.png" />
<img src="img-readme/main-readme/img30.png" width="500"/>
</p>

Enfin, ajoute ton fichier `.env` comme ci-dessous (n'oublie pas d'activer les fichiers cachés)

<img src="img-readme/main-readme/img37.png" width="600"/>
<img src="img-readme/main-readme/img38.png" width="600"/>
<img src="img-readme/main-readme/img39.png" width="600"/>

Tu peux maintenant lancer ton application :
<br>
<p align="center"><img src="img-readme/main-readme/img31.png" width="800"/></p>

Alors ça marche ? ✨​
<p align="center"><img src="img-readme/main-readme/img32.png" width="900"/></p>
