import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { Jeu, JeuResume, TypeJeu } from "models/jeu.model"
import { Joueur } from "models/joueur.model"
import { PartieDetail, SoireeDetail } from "models/soiree.model"
import { JoueurClassement } from "utils/classement"
import { formaterDate, formaterDuree, pluriel } from "utils/format"
import { HomePageData } from "./home.resolver"

// Nombre de couvertures dans le collage du bandeau et dans la rangée "Dans la ludothèque"
const NB_COUVERTURES_COLLAGE = 3
const NB_COUVERTURES_RANGEE = 6

// Mêmes couleurs que les couvertures de la Ludothèque et les avatars de la page Joueurs
const COULEURS = ["#e9562e", "#397df2", "#2fc0b5", "#f5b73e", "#9c5be1", "#ed3f70"]

// Une partie de la dernière soirée, avec ses gagnants déjà calculés
interface PartieResumee {
  partie: PartieDetail
  gagnants: string // "Léa" ou "Léa, MaxPower, Inès" (ex aequo ou équipe)
  points: number | null
}

@Component({
  selector: "app-home",
  templateUrl: "./home.component.html",
  styleUrls: ["./home.component.scss"],
})
export class HomeComponent implements OnInit {
  readonly formaterDate = formaterDate
  readonly formaterDuree = formaterDuree
  readonly pluriel = pluriel

  nbJeux = 0
  nbSoirees = 0
  nbParties = 0
  nbJoueurs = 0
  couverturesCollage: Jeu[] = []
  couverturesRangee: Jeu[] = []
  podium: { classement: JoueurClassement; rang: number }[] = [] // dans l'ordre d'affichage : 2e, 1er, 3e
  derniereSoiree: SoireeDetail | null = null
  partiesDerniereSoiree: PartieResumee[] = []
  dernierGagnant: PartieResumee | null = null // pour la bulle sous le collage
  erreur: HomePageData["erreur"] = null
  // Images qui n'ont pas pu être chargées : on affiche la couverture colorée à la place
  readonly imagesIndisponibles = new Set<string>()

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      const page: HomePageData = data["homePage"]
      this.nbJeux = page.jeux.length
      this.nbSoirees = page.soirees.length
      // Total des parties de toutes les soirées
      this.nbParties = page.soirees.reduce((total, soiree) => total + soiree.nbParties, 0)
      this.nbJoueurs = page.classement.length

      // Collage : d'abord les jeux avec une vraie image (RAWG), puis les autres
      const avecImage = page.jeux.filter((jeu) => jeu.image)
      const sansImage = page.jeux.filter((jeu) => !jeu.image)
      this.couverturesCollage = [...avecImage, ...sansImage].slice(0, NB_COUVERTURES_COLLAGE)
      this.couverturesRangee = page.jeux.slice(0, NB_COUVERTURES_RANGEE)

      // Podium : le 1er au centre, comme sur une vraie marche
      const [premier, deuxieme, troisieme] = page.classement
      this.podium = [
        { classement: deuxieme, rang: 2 },
        { classement: premier, rang: 1 },
        { classement: troisieme, rang: 3 },
      ].filter((marche) => marche.classement !== undefined)

      this.derniereSoiree = page.derniereSoiree
      this.partiesDerniereSoiree = (page.derniereSoiree?.parties ?? []).map((partie) => this.resumerPartie(partie))
      this.dernierGagnant = this.partiesDerniereSoiree.at(-1) ?? null
      this.erreur = page.erreur
    })
  }

  // Les scores arrivent triés par rang : les gagnants sont tous ceux classés premiers
  private resumerPartie(partie: PartieDetail): PartieResumee {
    const premiers = partie.scores.filter((score) => score.rang === 1)
    return {
      partie,
      gagnants: premiers.map((score) => score.joueur.pseudo).join(", "),
      points: premiers[0]?.points ?? null,
    }
  }

  aUneImage(jeu: JeuResume): boolean {
    return !!jeu.image && !this.imagesIndisponibles.has(jeu.image)
  }

  aUnAvatar(joueur: Joueur): boolean {
    return !!joueur.avatar && !this.imagesIndisponibles.has(joueur.avatar)
  }

  marquerIndisponible(url: string | null): void {
    if (url) this.imagesIndisponibles.add(url)
  }

  labelType(type: TypeJeu): string {
    return type === TypeJeu.SOCIETE ? "Société" : "Jeu vidéo"
  }

  couleur(id: number): string {
    return COULEURS[Math.max(0, id - 1) % COULEURS.length]
  }

  initiale(texte: string): string {
    return texte.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }
}
