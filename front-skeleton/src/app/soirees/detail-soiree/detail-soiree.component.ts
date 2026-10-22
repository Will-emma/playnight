import { Location } from "@angular/common"
import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { TypeJeu } from "models/jeu.model"
import { Joueur } from "models/joueur.model"
import { PartieDetail, ScoreDetail, SoireeDetail } from "models/soiree.model"
import { formaterDate, formaterDuree, pluriel } from "utils/format"
import { DetailSoireePageData } from "./detail-soiree.resolver"

// Mêmes couleurs que les couvertures de la Ludothèque et les avatars de la page Joueurs
const COULEURS = ["#e9562e", "#397df2", "#2fc0b5", "#f5b73e", "#9c5be1", "#ed3f70"]

// Formats fournis par le navigateur (la locale française d'Angular n'est pas activée)
const FORMAT_JOUR_COURT = new Intl.DateTimeFormat("fr-FR", { weekday: "short" }) // "sam."
const FORMAT_MOIS_COURT = new Intl.DateTimeFormat("fr-FR", { month: "short" }) // "sept."
const FORMAT_HEURE = new Intl.DateTimeFormat("fr-FR", { hour: "2-digit", minute: "2-digit" }) // "20:00"

// Une ligne du classement d'une partie en équipe : l'équipe, son rang et ses membres
export interface LigneEquipe {
  numeroEquipe: number | null // null : joueur sans équipe dans une partie en équipe
  rang: number
  membres: ScoreDetail[]
  points: number | null // points communs à toute l'équipe ; null si absents ou différents d'un membre à l'autre
}

// Tout ce qu'une partie affiche, calculé une seule fois par soirée
interface PartieAffichee {
  partie: PartieDetail
  vainqueurs: ScoreDetail[] // scores de rang 1 (plusieurs en cas d'ex aequo)
  // Les autres joueurs, avec la largeur de leur barre de points (en %, par rapport au meilleur score de la partie)
  // largeur null : la partie n'a pas de points (seul le rang compte)
  autres: { score: ScoreDetail; largeur: number | null }[]
  equipes: LigneEquipe[] | null // null : partie en chacun pour soi
}

@Component({
  selector: "app-detail-soiree",
  templateUrl: "./detail-soiree.component.html",
  styleUrls: ["./detail-soiree.component.scss"],
})
export class DetailSoireeComponent implements OnInit {
  readonly formaterDate = formaterDate
  readonly formaterDuree = formaterDuree
  readonly pluriel = pluriel

  soiree: SoireeDetail | null = null
  erreur: DetailSoireePageData["erreur"] = null
  partiesAffichees: PartieAffichee[] = []
  // Bloc de date de l'en-tête : "SAM." / "12" / "SEPT 2026" / "20:00"
  date = { jour: "", numero: "", moisAnnee: "", heure: "" }
  dureeTotale = 0
  victoiresParJoueur = new Map<number, number>() // clé : id du joueur
  star: { joueur: Joueur; nbVictoires: number } | null = null // null : aucune partie, ou égalité en tête
  // Images qui n'ont pas pu être chargées : on affiche la couverture colorée ou l'initiale à la place
  readonly imagesIndisponibles = new Set<string>()

  constructor(
    private route: ActivatedRoute,
    private location: Location,
  ) {}

  ngOnInit(): void {
    // data (et non snapshot.data) : la page se met à jour si on passe d'une soirée à une autre sans la quitter
    this.route.data.subscribe((data) => {
      const page: DetailSoireePageData = data["detailSoireePage"]
      this.soiree = page.soiree
      this.erreur = page.erreur
      if (page.soiree) this.preparer(page.soiree)
    })
  }

  private preparer(soiree: SoireeDetail): void {
    const date = new Date(soiree.date)
    this.date = {
      jour: FORMAT_JOUR_COURT.format(date).toLocaleUpperCase("fr-FR"),
      numero: String(date.getDate()),
      moisAnnee: `${FORMAT_MOIS_COURT.format(date).replace(".", "")} ${date.getFullYear()}`.toLocaleUpperCase("fr-FR"),
      heure: FORMAT_HEURE.format(date),
    }
    this.dureeTotale = soiree.parties.reduce((total, partie) => total + partie.duree, 0)
    this.partiesAffichees = soiree.parties.map(preparerPartie)

    // Victoires de la soirée : un score de rang 1 compte (ex aequo et membres d'une équipe gagnante compris)
    this.victoiresParJoueur = new Map()
    for (const { vainqueurs } of this.partiesAffichees) {
      for (const score of vainqueurs) {
        this.victoiresParJoueur.set(score.joueur.id, (this.victoiresParJoueur.get(score.joueur.id) ?? 0) + 1)
      }
    }

    // Star de la soirée : seulement si un joueur a strictement plus de victoires que tous les autres
    const nbMax = Math.max(0, ...this.victoiresParJoueur.values())
    const enTete = soiree.joueurs.filter((joueur) => this.victoiresParJoueur.get(joueur.id) === nbMax)
    this.star = nbMax > 0 && enTete.length === 1 ? { joueur: enTete[0], nbVictoires: nbMax } : null
  }

  victoires(joueur: Joueur): number {
    return this.victoiresParJoueur.get(joueur.id) ?? 0
  }

  // On arrive ici depuis plusieurs pages (liste des soirées, profil joueur…) : on revient à la précédente
  retour(): void {
    this.location.back()
  }

  // 1 → "1er", 2 → "2e"
  formaterRang(rang: number): string {
    return rang === 1 ? "1er" : `${rang}e`
  }

  // Les membres d'une équipe ont-ils des points différents (par exemple les buts de chacun) ?
  pointsIndividuels(ligne: LigneEquipe): boolean {
    return ligne.points === null && ligne.membres.some((membre) => membre.points !== null)
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

  initiale(pseudo: string): string {
    return pseudo.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }
}

function preparerPartie(partie: PartieDetail): PartieAffichee {
  // Les scores arrivent triés par rang (back) : les vainqueurs sont en tête
  const vainqueurs = partie.scores.filter((score) => score.rang === 1)
  const meilleurScore = Math.max(0, ...partie.scores.map((score) => score.points ?? 0))
  return {
    partie,
    vainqueurs,
    autres: partie.scores
      .filter((score) => score.rang !== 1)
      .map((score) => ({
        score,
        largeur: score.points !== null && meilleurScore > 0 ? Math.round((score.points / meilleurScore) * 100) : null,
      })),
    equipes: estEnEquipe(partie) ? regrouperParEquipe(partie) : null,
  }
}

// Une partie est en équipe dès qu'un de ses scores a un numéro d'équipe
function estEnEquipe(partie: PartieDetail): boolean {
  return partie.scores.some((score) => score.numeroEquipe !== null)
}

// Regroupe les scores par numéro d'équipe. Les scores arrivent triés par rang (back) :
// les équipes sont donc créées dans l'ordre du classement. Un joueur sans équipe forme sa propre ligne.
function regrouperParEquipe(partie: PartieDetail): LigneEquipe[] {
  const lignes: LigneEquipe[] = []
  for (const score of partie.scores) {
    const ligne = score.numeroEquipe === null ? undefined : lignes.find((l) => l.numeroEquipe === score.numeroEquipe)
    if (ligne) {
      ligne.membres.push(score)
    } else {
      lignes.push({ numeroEquipe: score.numeroEquipe, rang: score.rang, membres: [score], points: null })
    }
  }
  // Points de l'équipe : seulement s'ils sont renseignés et identiques pour tous les membres
  for (const ligne of lignes) {
    const points = ligne.membres[0].points
    ligne.points = points !== null && ligne.membres.every((membre) => membre.points === points) ? points : null
  }
  return lignes
}
