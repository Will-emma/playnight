import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { FicheJeu, Jeu, MeilleurJoueur, TypeJeu } from "models/jeu.model"
import { Joueur } from "models/joueur.model"
import { formaterDate, formaterDuree, pluriel } from "utils/format"
import { FicheJeuPageData } from "./fiche-jeu.resolver"

// Mêmes couleurs que les couvertures de la Ludothèque et les avatars de la page Joueurs
const COULEURS = ["#e9562e", "#397df2", "#2fc0b5", "#f5b73e", "#9c5be1", "#ed3f70"]

// Badge de date des parties : "12" / "SEPT" (format fourni par le navigateur)
const FORMAT_MOIS_COURT = new Intl.DateTimeFormat("fr-FR", { month: "short" })

// Nombre de joueurs sur le podium ; les suivants sont listés en dessous
const NB_PODIUM = 3

@Component({
  selector: "app-fiche-jeu",
  templateUrl: "./fiche-jeu.component.html",
  styleUrls: ["./fiche-jeu.component.scss"],
})
export class FicheJeuComponent implements OnInit {
  // Le back renvoie tous les vainqueurs : la page n'en montre que 5 (podium compris), sauf si on demande à tout voir
  readonly nbMeilleursJoueursAffiches = 5

  fiche: FicheJeu | null = null
  erreur: FicheJeuPageData["erreur"] = null
  toutLeClassement = false
  // Images qui n'ont pas pu être chargées (couverture du jeu, avatars) : on affiche la couleur et l'initiale à la place
  readonly imagesIndisponibles = new Set<string>()

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    // data (et non snapshot.data) : la page se met à jour si on passe d'une fiche à une autre sans la quitter
    this.route.data.subscribe((data) => {
      const page: FicheJeuPageData = data["ficheJeuPage"]
      this.fiche = page.fiche
      this.erreur = page.erreur
      this.toutLeClassement = false
    })
  }

  // Podium : le 1er au centre, comme sur une vraie marche (2e, 1er, 3e)
  get podium(): { meilleur: MeilleurJoueur }[] {
    const [premier, deuxieme, troisieme] = this.fiche?.meilleursJoueurs ?? []
    return [{ meilleur: deuxieme }, { meilleur: premier }, { meilleur: troisieme }].filter((ligne) => ligne.meilleur !== undefined)
  }

  // Les joueurs après le podium : jusqu'au 5e, ou tout le classement à la demande
  get suivants(): MeilleurJoueur[] {
    const classement = this.fiche?.meilleursJoueurs ?? []
    return classement.slice(NB_PODIUM, this.toutLeClassement ? undefined : this.nbMeilleursJoueursAffiches)
  }

  // Les joueurs à égalité de victoires partagent la même place (1, 2, 2, 4…)
  place(meilleurJoueur: MeilleurJoueur): number {
    const classement = this.fiche?.meilleursJoueurs ?? []
    return classement.findIndex((autre) => autre.nbVictoires === meilleurJoueur.nbVictoires) + 1
  }

  labelType(type: TypeJeu): string {
    return type === TypeJeu.SOCIETE ? "Jeu de société" : "Jeu vidéo"
  }

  nombreJoueurs(jeu: Jeu): string {
    if (jeu.nbJoueursMin == null || jeu.nbJoueursMax == null) return "Nombre de joueurs non renseigné"
    if (jeu.nbJoueursMin === jeu.nbJoueursMax) return pluriel(jeu.nbJoueursMin, "joueur") // "1 joueur", "2 joueurs"
    return `${jeu.nbJoueursMin} à ${jeu.nbJoueursMax} joueurs`
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

  // "2026-09-12T20:00:00" → { numero: "12", mois: "SEPT" }
  badgeDate(date: string): { numero: string; mois: string } {
    const jour = new Date(date)
    return {
      numero: String(jour.getDate()),
      mois: FORMAT_MOIS_COURT.format(jour).replace(".", "").toLocaleUpperCase("fr-FR"),
    }
  }

  avatarDe(joueur: Joueur): string | null {
    return this.aUneImage(joueur.avatar) ? joueur.avatar : null
  }

  readonly formaterDate = formaterDate
  readonly formaterDuree = formaterDuree
  readonly pluriel = pluriel

  // ["Alex", "Léa", "Max"] → "Alex, Léa et Max"
  formaterVainqueurs(vainqueurs: string[]): string {
    if (vainqueurs.length === 0) return "Aucun vainqueur enregistré"
    if (vainqueurs.length === 1) return vainqueurs[0]
    return `${vainqueurs.slice(0, -1).join(", ")} et ${vainqueurs[vainqueurs.length - 1]}`
  }
}
