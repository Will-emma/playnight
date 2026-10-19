import { HttpErrorResponse } from "@angular/common/http"
import { Component } from "@angular/core"
import { Router } from "@angular/router"
import { RawgJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"

// Un résultat de recherche, avec ce que l'utilisateur saisit avant de l'ajouter
interface Resultat {
  jeu: RawgJeu
  nbJoueursMin: number | null
  nbJoueursMax: number | null
  envoiEnCours: boolean
  erreur: string | null
}

@Component({
  selector: "app-importer-jeu",
  templateUrl: "./importer-jeu.component.html",
  styleUrls: ["./importer-jeu.component.scss"],
})
export class ImporterJeuComponent {
  recherche = ""
  // Recherche réellement envoyée : le texte du champ peut changer pendant que les résultats s'affichent
  rechercheEnvoyee: string | null = null
  resultats: Resultat[] = []
  rechercheEnCours = false
  erreurRecherche: string | null = null
  readonly imagesIndisponibles = new Set<number>()

  constructor(
    private jeuService: JeuService,
    private router: Router,
  ) {}

  rechercher(): void {
    const recherche = this.recherche.trim()
    if (!recherche || this.rechercheEnCours) return

    this.rechercheEnCours = true
    this.erreurRecherche = null
    this.jeuService.rechercherSurRawg(recherche).subscribe({
      next: (jeux) => {
        // Valeurs proposées par défaut, à ajuster avant l'ajout (RAWG ne donne pas le nombre de joueurs)
        this.resultats = jeux.map((jeu) => ({ jeu, nbJoueursMin: 1, nbJoueursMax: 4, envoiEnCours: false, erreur: null }))
        this.rechercheEnvoyee = recherche
        this.rechercheEnCours = false
      },
      error: (erreur: HttpErrorResponse) => {
        this.resultats = []
        this.rechercheEnvoyee = null
        this.erreurRecherche = this.messageErreur(erreur, "La recherche n'a pas abouti. Réessayez dans un instant.")
        this.rechercheEnCours = false
      },
    })
  }

  ajouter(resultat: Resultat): void {
    if (resultat.envoiEnCours) return
    const erreurSaisie = this.verifierNbJoueurs(resultat)
    if (erreurSaisie) {
      resultat.erreur = erreurSaisie
      return
    }

    resultat.envoiEnCours = true
    resultat.erreur = null
    this.jeuService
      .importerDepuisRawg(resultat.jeu.idRawg, {
        nbJoueursMin: resultat.nbJoueursMin as number,
        nbJoueursMax: resultat.nbJoueursMax as number,
      })
      .subscribe({
        // Le jeu est dans la ludothèque : on ouvre sa fiche
        next: (jeu) => this.router.navigate(["/jeux", jeu.id]),
        error: (erreur: HttpErrorResponse) => {
          resultat.envoiEnCours = false
          resultat.erreur = this.messageErreur(erreur, "Le jeu n'a pas pu être ajouté. Réessayez dans un instant.")
        },
      })
  }

  // "2017-03-03" → "2017"
  annee(jeu: RawgJeu): string | null {
    return jeu.dateSortie ? jeu.dateSortie.slice(0, 4) : null
  }

  initiale(jeu: RawgJeu): string {
    return jeu.nom.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }

  marquerImageIndisponible(jeu: RawgJeu): void {
    this.imagesIndisponibles.add(jeu.idRawg)
  }

  // Mêmes règles que le back, vérifiées ici pour répondre tout de suite
  private verifierNbJoueurs(resultat: Resultat): string | null {
    const min = resultat.nbJoueursMin
    const max = resultat.nbJoueursMax
    if (min == null || max == null || !Number.isInteger(min) || !Number.isInteger(max)) {
      return "Indiquez un nombre de joueurs minimum et maximum."
    }
    if (min < 1) return "Il faut au moins 1 joueur."
    if (max < min) return "Le maximum ne peut pas être inférieur au minimum."
    return null
  }

  private messageErreur(erreur: HttpErrorResponse, parDefaut: string): string {
    // 400 (doublon, saisie) et 503 (RAWG non configuré ou indisponible) : le message du back s'affiche tel quel
    if ((erreur.status === 400 || erreur.status === 503) && erreur.error?.message) return erreur.error.message
    if (erreur.status === 0) return "L'API PlayNight ne répond pas. Vérifiez qu'elle est lancée, puis réessayez."
    return parDefaut
  }
}
