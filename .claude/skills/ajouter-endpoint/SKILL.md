---
name: ajouter-endpoint
description: Ajouter ou modifier un endpoint REST de l'API PlayNight (back-skeleton) en respectant l'architecture du projet - controller, service, DAO, DTO avec mapper écrit à la main, gestion des erreurs, tests et documentation. À utiliser dès qu'on demande une nouvelle route, une nouvelle ressource ou une nouvelle donnée exposée par l'API.
---

# Ajouter un endpoint à l'API PlayNight

L'API est dans `back-skeleton/` (Spring Boot 3, Java 17, package `com.takima.backskeleton`).
Chaque endpoint traverse les mêmes couches, dans cet ordre :

```
controllers/  →  services/  →  DAO/  →  models/
     ↑              ↓
    DTO/  ←  mapper (DTO/XxxMapper)
```

Avant d'écrire du code, lire la ressource la plus proche (`JeuController`, `JeuService`,
`JeuDao`, `JeuDto`, `JeuMapper` et leurs tests) et reproduire son style.

## Règles du projet

- **Pas de Lombok, pas de nouvelle dépendance** sans accord de l'équipe.
- Package `com.takima.backskeleton` inchangé ; dossiers `controllers`, `services`, `DAO`, `DTO`, `models`.
- Injection **par constructeur** (champs `private final`), jamais `@Autowired` sur un champ.
- Le controller n'appelle **que des services**, jamais un DAO.
- Les entités ne sortent jamais de l'API : le controller reçoit et renvoie des **DTO**.
- Noms en français, comme le reste du code (`getClassementVictoires`, `findJeuById`…).
- Commentaires rares et utiles, en français, sur une ligne au-dessus du code concerné.

## 1. Choisir la route (conventions REST)

| Action | Verbe et route | Code de succès |
|---|---|---|
| Lister | `GET /ressources` (filtres en `@RequestParam`) | 200 |
| Détail | `GET /ressources/{id}` | 200 |
| Créer | `POST /ressources` | 201 (`@ResponseStatus(HttpStatus.CREATED)`) |
| Modifier | `PUT /ressources/{id}` | 200, renvoie l'objet modifié |
| Supprimer | `DELETE /ressources/{id}` | 204 (`@ResponseStatus(HttpStatus.NO_CONTENT)`) |
| Donnée calculée | `GET /ressources/{id}/sous-ressource` (ex. `/jeux/{id}/classement`) | 200 |

- Ressources au **pluriel**, en minuscules, mots séparés par des tirets (`classement-victoires`).
- Pas de verbe dans l'URL (`/jeux/{id}/fiche`, pas `/jeux/{id}/getFiche`).
- Les parties et les scores passent par leur soirée : pas d'endpoint séparé.

## 2. DAO

Interface `XxxDao extends JpaRepository<Xxx, Long>` annotée `@Repository`.
Préférer les **méthodes dérivées** (`findByTypeOrderByNomAsc`) ; sinon `@Query` en JPQL avec
`JOIN FETCH` pour éviter les requêtes en cascade, ou une projection (`XxxProjection`) pour
une agrégation (`COUNT`, `GROUP BY`). Requête native seulement si JPQL ne suffit pas.

## 3. DTO et mapper

- DTO d'écriture ou de ressource complète : classe avec getters/setters et **builder interne**
  écrit à la main (modèle : `JeuDto` et `JeuDto.JeuDtoBuilder`).
- DTO de lecture seule (résumé, statistique) : classe immuable avec champs `final`, constructeur et
  getters (modèle : `MeilleurJoueurDto`, `JeuResumeDto`), ou `record` (modèle : `ClassementDto`).
- Écriture : un objet lié est désigné par son id (`jeuId`, `joueurIds`).
  Lecture : un objet lié est renvoyé en résumé (`JoueurResumeDto`, `JeuResumeDto`).
- Pas d'aller-retour : une soirée contient ses parties, une partie ne contient pas sa soirée.
- Conversion dans un mapper statique `XxxMapper` (constructeur privé) : `toDto`, `fromDto(dto, id)`,
  `toResumeDto`. Les entités se construisent avec leur builder (`new Jeu.Builder()...build()`).

## 4. Service

Classe `@Service` qui porte la logique métier :

- lecture introuvable : `orElseThrow(() -> new NoSuchElementException("Le jeu " + id + " n'existe pas"))`,
  dans une méthode privée `findXxxById` réutilisée ;
- règle métier non respectée : `IllegalArgumentException` avec un message en français ;
- `@Transactional` sur les écritures, `@Transactional(readOnly = true)` sur les lectures
  qui parcourent des relations ;
- après une modification de soirée, de partie ou de score : `classementService.recalculer()`
  pour garder l'Elo à jour.

## 5. Controller

```java
@GetMapping("/{id}/classement-victoires")
public List<MeilleurJoueurDto> getClassementVictoires(@PathVariable Long id) {
    return jeuService.getClassementVictoires(id);
}
```

Le controller ne fait qu'appeler le service et fixer le code HTTP. Les erreurs sont traduites
par `ApiExceptionHandler` (`@RestControllerAdvice`), qui renvoie `{ "message": "..." }` :

| Exception levée par le service | Code HTTP |
|---|---|
| `NoSuchElementException` | 404 |
| `IllegalArgumentException`, JSON ou paramètre invalide | 400 |
| `DataIntegrityViolationException` (clé étrangère, unicité) | 409 |

Ne pas ajouter de `try/catch` dans le controller. Pour un nouveau cas d'erreur, créer une
exception dans `services/` et un `@ExceptionHandler` dans `ApiExceptionHandler`.

## 6. Tests

Toujours les deux niveaux, dans `back-skeleton/src/test/java/com/takima/backskeleton/` :

- **Service** (`services/XxxServiceTest`) : test unitaire `@ExtendWith(MockitoExtension.class)`,
  DAO en `@Mock`, service en `@InjectMocks` ; couvrir le cas normal, l'élément introuvable
  et chaque règle métier.
- **Controller** (`controllers/XxxControllerTest`) : `@WebMvcTest(XxxController.class)`, services en
  `@MockBean` ; vérifier la route, le code HTTP et les noms des champs JSON avec `jsonPath`.
- **Requête SQL non triviale** (`@Query`, projection, agrégation) : test d'intégration
  `@SpringBootTest` sur la vraie base, `@Transactional` pour tout annuler (modèle :
  `ClassementVictoiresIntegrationTest`). Ne jamais supposer qu'une table est vide : les données
  de démo de `initdb/` sont présentes, comparer avant et après.

Lancer les tests depuis `back-skeleton/` avec `./mvnw test` (base PostgreSQL démarrée).
La CI exécute tous les scripts `initdb/` puis `mvn verify`.

## 7. Base de données (si le modèle change)

- Nouvelle table ou colonne : nouveau script numéroté dans `back-skeleton/initdb/`
  (`7_...sql`), **rejouable** (`IF NOT EXISTS`, `WHERE NOT EXISTS`) et sans toucher aux scripts existants.
- Documenter la commande pour une base existante dans `back-skeleton/README.MD`.
- Mettre à jour `docs/modele-donnees.md`.

## 8. Documentation et front

- `docs/format-dto.md` : ajouter le JSON de la route et la ligne du tableau « Endpoints ».
- Si le front l'utilise : une méthode dans `front-skeleton/src/app/services/xxx.service.ts`
  (même style que `JeuService`, `HttpParams` pour les paramètres) et le type dans `models/`.

## Vérification finale

1. `./mvnw test` passe (sous Windows, `AvatarStockageTest` peut échouer pour des droits de fichiers ; il passe en CI).
2. Appel réel de la route sur le back lancé (`curl http://localhost:8080/...`) : code HTTP et JSON attendus,
   y compris un id inexistant (404).
3. Swagger à jour : http://localhost:8080/swagger-ui/index.html
4. Branche `feature/...` partie de `main`, PR avec `Closes #N`, CI verte avant le merge.
