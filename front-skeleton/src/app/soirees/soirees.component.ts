import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { JeuResume, TypeJeu } from "models/jeu.model"
import { Joueur } from "models/joueur.model"
import { SoireeDetail } from "models/soiree.model"
import { SoireeService } from "services/soiree.service"
import { formaterDate, formaterDuree, pluriel } from "utils/format"
import { SoireesPageData, chargerSoirees } from "./soirees.resolver"

// Au-delà, les joueurs suivants sont résumés par "+N" pour garder des cartes de même hauteur
const NB_AVATARS_MAX = 5
// Nombre de couvertures dans la mosaïque en haut de chaque carte
const NB_JEUX_MOSAIQUE = 3

// Mêmes couleurs que les couvertures de la Ludothèque et les avatars de la page Joueurs
const COULEURS = ["#e9562e", "#397df2", "#2fc0b5", "#f5b73e", "#9c5be1", "#ed3f70"]

// Formats fournis par le navigateur (la locale française d'Angular n'est pas activée)
const FORMAT_MOIS = new Intl.DateTimeFormat("fr-FR", { month: "long", year: "numeric" }) // "octobre 2026"
const FORMAT_MOIS_COURT = new Intl.DateTimeFormat("fr-FR", { month: "short" }) // "oct."
const FORMAT_JOUR_HEURE = new Intl.DateTimeFormat("fr-FR", { weekday: "long", hour: "2-digit", minute: "2-digit" }) // "samedi 19:30"

// Résumé des vainqueurs d'une soirée, affiché dans l'encart doré
// "star" : un joueur a plus de victoires que tous les autres ; "egalite" : plusieurs joueurs en tête
type EncartVainqueurs = { type: "star"; pseudo: string; nbVictoires: number } | { type: "egalite"; pseudos: string[]; nbVictoires: number }

// Tout ce qu'une carte affiche, calculé une seule fois au chargement
interface CarteSoiree {
  soiree: SoireeDetail
  jeux: JeuResume[] // jeux joués, sans doublon, NB_JEUX_MOSAIQUE au maximum
  nbJeuxMasques: number
  jour: string // "03"
  mois: string // "OCT"
  jourHeure: string // "Samedi · 19:30"
  dureeTotale: number // en minutes
  encart: EncartVainqueurs | null // null : aucune partie jouée
}

interface MoisSoirees {
  libelle: string // "OCTOBRE 2026"
  cartes: CarteSoiree[]
}

@Component({
  selector: "app-soirees",
  templateUrl: "./soirees.component.html",
  styleUrls: ["./soirees.component.scss"],
})
export class SoireesComponent implements OnInit {
  readonly formaterDate = formaterDate
  readonly formaterDuree = formaterDuree
  readonly pluriel = pluriel

  soirees: SoireeDetail[] = []
  moisAvecSoirees: MoisSoirees[] = []
  nbParties = 0
  nbJoueursDifferents = 0
  erreur: SoireesPageData["erreur"] = null
  chargement = false
  // Images qui n'ont pas pu être chargées : on affiche la couverture colorée ou l'initiale à la place
  readonly imagesIndisponibles = new Set<string>()

  constructor(
    private route: ActivatedRoute,
    private soireeService: SoireeService,
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      const page: SoireesPageData = data["soireesPage"]
      this.afficher(page.soirees)
      this.erreur = page.erreur
    })
  }

  // Bouton "Réessayer" : on rappelle l'API sans recharger toute l'appli
  recharger(): void {
    this.chargement = true
    chargerSoirees(this.soireeService).subscribe({
      next: (soirees) => {
        this.afficher(soirees)
        this.erreur = null
        this.chargement = false
      },
      error: () => {
        this.erreur = "indisponible"
        this.chargement = false
      },
    })
  }

  private afficher(soirees: SoireeDetail[]): void {
    this.soirees = soirees
    this.nbParties = soirees.reduce((total, soiree) => total + soiree.parties.length, 0)
    this.nbJoueursDifferents = new Set(soirees.flatMap((soiree) => soiree.joueurs.map((joueur) => joueur.id))).size

    // Les soirées arrivent de la plus récente à la plus ancienne : il suffit de les regrouper dans l'ordre
    this.moisAvecSoirees = []
    for (const soiree of soirees) {
      const libelle = FORMAT_MOIS.format(new Date(soiree.date)).toLocaleUpperCase("fr-FR")
      const dernier = this.moisAvecSoirees.at(-1)
      if (dernier?.libelle === libelle) dernier.cartes.push(this.preparerCarte(soiree))
      else this.moisAvecSoirees.push({ libelle, cartes: [this.preparerCarte(soiree)] })
    }
  }

  private preparerCarte(soiree: SoireeDetail): CarteSoiree {
    const date = new Date(soiree.date)
    const jourHeure = FORMAT_JOUR_HEURE.format(date) // "samedi 19:30"
    const separation = jourHeure.indexOf(" ")

    // Un même jeu joué plusieurs fois n'a qu'une tuile
    const jeux = [...new Map(soiree.parties.map((partie) => [partie.jeu.id, partie.jeu])).values()]

    return {
      soiree,
      jeux: jeux.slice(0, NB_JEUX_MOSAIQUE),
      nbJeuxMasques: Math.max(0, jeux.length - NB_JEUX_MOSAIQUE),
      jour: String(date.getDate()).padStart(2, "0"),
      mois: FORMAT_MOIS_COURT.format(date).replace(".", "").toLocaleUpperCase("fr-FR"),
      jourHeure: `${jourHeure.charAt(0).toLocaleUpperCase("fr-FR")}${jourHeure.slice(1, separation)} · ${jourHeure.slice(separation + 1)}`,
      dureeTotale: soiree.parties.reduce((total, partie) => total + partie.duree, 0),
      encart: this.resumerVainqueurs(soiree),
    }
  }

  // Victoires de chaque joueur pendant la soirée : un score de rang 1 compte (ex aequo et équipes compris)
  private resumerVainqueurs(soiree: SoireeDetail): EncartVainqueurs | null {
    const victoires = new Map<string, number>()
    for (const partie of soiree.parties) {
      for (const score of partie.scores.filter((score) => score.rang === 1)) {
        victoires.set(score.joueur.pseudo, (victoires.get(score.joueur.pseudo) ?? 0) + 1)
      }
    }
    if (victoires.size === 0) return null

    const nbVictoires = Math.max(...victoires.values())
    const enTete = [...victoires.keys()].filter((pseudo) => victoires.get(pseudo) === nbVictoires)
    return enTete.length === 1
      ? { type: "star", pseudo: enTete[0], nbVictoires }
      : { type: "egalite", pseudos: enTete, nbVictoires }
  }

  avatarsAffiches(soiree: SoireeDetail): Joueur[] {
    return soiree.joueurs.slice(0, NB_AVATARS_MAX)
  }

  nbJoueursMasques(soiree: SoireeDetail): number {
    return Math.max(0, soiree.joueurs.length - NB_AVATARS_MAX)
  }

  // "Alice, Bob et Chloé" : lu par les lecteurs d'écran à la place des avatars
  listePseudos(pseudos: string[]): string {
    if (pseudos.length <= 1) return pseudos.join("")
    return `${pseudos.slice(0, -1).join(", ")} et ${pseudos[pseudos.length - 1]}`
  }

  pseudos(soiree: SoireeDetail): string[] {
    return soiree.joueurs.map((joueur) => joueur.pseudo)
  }

  aUneImage(url: string | null): boolean {
    return !!url && !this.imagesIndisponibles.has(url)
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
