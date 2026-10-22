import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { HttpErrorResponse } from "@angular/common/http"
import { JoueurService } from "services/joueur.service"
import { AVATARS } from "utils/avatar"
import { JoueurClassement, comparerClassement } from "utils/classement"
import { pluriel } from "utils/format"
import { JoueursPageData } from "./joueurs.resolver"

// Mêmes couleurs que l'accueil, la Ludothèque et les profils (index : id - 1)
const COULEURS = ["#e9562e", "#397df2", "#2fc0b5", "#f5b73e", "#9c5be1", "#ed3f70"]

// Une carte de "Toute la bande" : le joueur, ses chiffres et sa place au classement
interface CarteJoueur {
  classement: JoueurClassement
  rang: number
  tauxVictoire: number // en %, arrondi
}

@Component({
  selector: "app-joueurs",
  templateUrl: "./joueurs.component.html",
  styleUrls: ["./joueurs.component.scss"],
})
export class JoueursComponent implements OnInit {
  readonly pluriel = pluriel
  readonly avatars = AVATARS

  classement: JoueurClassement[] = []
  podium: { classement: JoueurClassement; rang: number }[] = [] // dans l'ordre d'affichage : 2e, 1er, 3e
  cartes: CarteJoueur[] = [] // dans l'ordre d'arrivée dans le groupe (id)
  nbVictoires = 0
  erreur: JoueursPageData["erreur"] = null

  formulaireOuvert = false
  enregistrement = false
  pseudo = ""
  avatarChoisi = AVATARS[0] // portrait de la galerie choisi dans le formulaire
  messageErreurAjout = ""
  // Images qui n'ont pas pu être chargées : on affiche l'initiale colorée à la place
  readonly imagesIndisponibles = new Set<string>()

  constructor(
    private route: ActivatedRoute,
    private joueurService: JoueurService,
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      const page: JoueursPageData = data["joueursPage"]
      this.erreur = page.erreur
      this.mettreAJour(page.classement)
    })
  }

  // Recalcule tout ce qui dépend du classement (au chargement et après un ajout)
  private mettreAJour(classement: JoueurClassement[]): void {
    this.classement = [...classement].sort(comparerClassement)
    this.nbVictoires = this.classement.reduce((total, ligne) => total + ligne.nbVictoires, 0)

    // Podium : le 1er au centre, comme sur une vraie marche
    const [premier, deuxieme, troisieme] = this.classement
    this.podium = [
      { classement: deuxieme, rang: 2 },
      { classement: premier, rang: 1 },
      { classement: troisieme, rang: 3 },
    ].filter((marche) => marche.classement !== undefined)

    this.cartes = this.classement
      .map((ligne, index) => ({
        classement: ligne,
        rang: index + 1,
        tauxVictoire: ligne.nbParties === 0 ? 0 : Math.round((ligne.nbVictoires / ligne.nbParties) * 100),
      }))
      .sort((a, b) => a.classement.joueur.id - b.classement.joueur.id)
  }

  // Les joueurs classés après le podium
  get suite(): { classement: JoueurClassement; rang: number }[] {
    return this.classement.slice(3).map((ligne, index) => ({ classement: ligne, rang: index + 4 }))
  }

  basculerFormulaire(): void {
    this.formulaireOuvert = !this.formulaireOuvert
    this.messageErreurAjout = ""
    if (this.formulaireOuvert) this.avatarChoisi = this.avatarConseille
  }

  ajouterJoueur(): void {
    const pseudo = this.pseudo.trim()
    if (!pseudo || this.enregistrement) return

    this.enregistrement = true
    this.messageErreurAjout = ""
    this.joueurService.create({ pseudo, avatar: this.avatarChoisi.url }).subscribe({
      next: (joueur) => {
        // Un nouveau joueur n'a encore joué aucune partie : inutile de recharger les profils
        this.mettreAJour([...this.classement, { joueur, nbVictoires: 0, nbParties: 0 }])
        this.pseudo = ""
        this.formulaireOuvert = false
        this.enregistrement = false
      },
      error: (erreur: HttpErrorResponse) => {
        this.messageErreurAjout =
          erreur.status === 409
            ? "Ce pseudo est déjà utilisé. Choisissez-en un autre."
            : "Le joueur n’a pas pu être ajouté. Vérifiez les informations puis réessayez."
        this.enregistrement = false
      },
    })
  }

  // Couleur que recevra le joueur en cours de création (les id se suivent)
  get couleurApercu(): string {
    const dernierId = Math.max(0, ...this.classement.map((ligne) => ligne.joueur.id))
    return this.couleur(dernierId + 1)
  }

  // Portrait proposé d'office : celui de la galerie que le moins de joueurs portent (le premier en cas d'égalité),
  // pour que chacun ait le sien tant que c'est possible. Même règle que le back (JoueurService.avatarParDefaut)
  get avatarConseille(): (typeof AVATARS)[number] {
    const nbPorteurs = (url: string) => this.classement.filter((ligne) => ligne.joueur.avatar === url).length
    return AVATARS.reduce((moinsPorte, avatar) => (nbPorteurs(avatar.url) < nbPorteurs(moinsPorte.url) ? avatar : moinsPorte))
  }

  choisirAvatar(avatar: (typeof AVATARS)[number]): void {
    this.avatarChoisi = avatar
  }

  get initialeApercu(): string {
    return this.pseudo.trim() ? this.initiale(this.pseudo) : "?"
  }

  aUneImage(url: string | null): boolean {
    return !!url && !this.imagesIndisponibles.has(url)
  }

  marquerIndisponible(url: string | null): void {
    if (url) this.imagesIndisponibles.add(url)
  }

  couleur(id: number): string {
    return COULEURS[Math.max(0, id - 1) % COULEURS.length]
  }

  initiale(texte: string): string {
    return texte.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }

  // 0 → "Nouveau dans la bande", 1 → "1 partie jouée", 5 → "5 parties jouées"
  resumeParties(nbParties: number): string {
    if (nbParties === 0) return "Nouveau dans la bande"
    return `${pluriel(nbParties, "partie")} jouée${nbParties > 1 ? "s" : ""}`
  }

  // "1er", "2e", "3e"…
  libelleRang(rang: number): string {
    return rang === 1 ? "1er" : `${rang}e`
  }
}
