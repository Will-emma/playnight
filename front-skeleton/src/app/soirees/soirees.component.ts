import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { Joueur } from "models/joueur.model"
import { SoireeResume } from "models/soiree.model"
import { SoireeService } from "services/soiree.service"
import { formaterDate, pluriel } from "utils/format"
import { SoireesPageData } from "./soirees.resolver"

// Au-delà, les joueurs suivants sont résumés par "+N" pour garder des cartes de même hauteur
const NB_AVATARS_MAX = 5

@Component({
  selector: "app-soirees",
  templateUrl: "./soirees.component.html",
  styleUrls: ["./soirees.component.scss"],
})
export class SoireesComponent implements OnInit {
  readonly formaterDate = formaterDate
  readonly pluriel = pluriel

  soirees: SoireeResume[] = []
  erreur: SoireesPageData["erreur"] = null
  chargement = false

  constructor(
    private route: ActivatedRoute,
    private soireeService: SoireeService,
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe((data) => {
      const page: SoireesPageData = data["soireesPage"]
      this.soirees = page.soirees
      this.erreur = page.erreur
    })
  }

  // Bouton "Réessayer" : on rappelle l'API sans recharger toute l'appli
  recharger(): void {
    this.chargement = true
    this.soireeService.findAll().subscribe({
      next: (soirees) => {
        this.soirees = soirees
        this.erreur = null
        this.chargement = false
      },
      error: () => {
        this.erreur = "indisponible"
        this.chargement = false
      },
    })
  }

  avatarsAffiches(soiree: SoireeResume): Joueur[] {
    return soiree.joueurs.slice(0, NB_AVATARS_MAX)
  }

  nbJoueursMasques(soiree: SoireeResume): number {
    return Math.max(0, soiree.joueurs.length - NB_AVATARS_MAX)
  }

  // "Alice, Bob et Chloé" : lu par les lecteurs d'écran à la place des avatars
  listePseudos(soiree: SoireeResume): string {
    const pseudos = soiree.joueurs.map((joueur) => joueur.pseudo)
    if (pseudos.length <= 1) return pseudos.join("")
    return `${pseudos.slice(0, -1).join(", ")} et ${pseudos[pseudos.length - 1]}`
  }

  initiale(pseudo: string): string {
    return pseudo.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }
}
