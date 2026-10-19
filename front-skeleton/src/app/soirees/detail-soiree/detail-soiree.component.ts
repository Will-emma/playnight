import { Location } from "@angular/common"
import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { PartieDetail, ScoreDetail, SoireeDetail } from "models/soiree.model"
import { formaterDate, formaterDuree, pluriel } from "utils/format"
import { DetailSoireePageData } from "./detail-soiree.resolver"

// Une ligne du classement d'une partie en équipe : l'équipe, son rang et ses membres
export interface LigneEquipe {
  numeroEquipe: number | null // null : joueur sans équipe dans une partie en équipe
  rang: number
  membres: ScoreDetail[]
  points: number | null // points communs à toute l'équipe ; null si absents ou différents d'un membre à l'autre
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
  // Classement par équipe de chaque partie en équipe (clé : id de la partie), calculé une seule fois par soirée
  equipesParPartie = new Map<number, LigneEquipe[]>()

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
      this.equipesParPartie = new Map(
        (page.soiree?.parties ?? []).filter(estEnEquipe).map((partie) => [partie.id, regrouperParEquipe(partie)]),
      )
    })
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

  initiale(pseudo: string): string {
    return pseudo.trim().charAt(0).toLocaleUpperCase("fr-FR")
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
