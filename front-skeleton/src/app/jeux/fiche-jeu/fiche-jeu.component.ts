import { Component, OnInit } from "@angular/core"
import { ActivatedRoute } from "@angular/router"
import { FicheJeu, Jeu, MeilleurJoueur, TypeJeu } from "models/jeu.model"
import { FicheJeuPageData } from "./fiche-jeu.resolver"

// Format fourni par le navigateur : "samedi 12 septembre 2026 à 20:00" (la locale française d'Angular n'est pas activée)
const FORMAT_DATE = new Intl.DateTimeFormat("fr-FR", {
  weekday: "long",
  day: "numeric",
  month: "long",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
})

@Component({
  selector: "app-fiche-jeu",
  templateUrl: "./fiche-jeu.component.html",
  styleUrls: ["./fiche-jeu.component.scss"],
})
export class FicheJeuComponent implements OnInit {
  // Le back renvoie tous les vainqueurs : la page n'en montre que 5, sauf si on demande à tout voir
  readonly nbMeilleursJoueursAffiches = 5

  fiche: FicheJeu | null = null
  erreur: FicheJeuPageData["erreur"] = null
  toutLeClassement = false
  imageIndisponible = false

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    // data (et non snapshot.data) : la page se met à jour si on passe d'une fiche à une autre sans la quitter
    this.route.data.subscribe((data) => {
      const page: FicheJeuPageData = data["ficheJeuPage"]
      this.fiche = page.fiche
      this.erreur = page.erreur
      this.toutLeClassement = false
      this.imageIndisponible = false
    })
  }

  get meilleursJoueursAffiches(): MeilleurJoueur[] {
    const classement = this.fiche?.meilleursJoueurs ?? []
    return this.toutLeClassement ? classement : classement.slice(0, this.nbMeilleursJoueursAffiches)
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
    if (jeu.nbJoueursMin === jeu.nbJoueursMax) return `${jeu.nbJoueursMin} joueurs`
    return `${jeu.nbJoueursMin} à ${jeu.nbJoueursMax} joueurs`
  }

  initiale(jeu: Jeu): string {
    return jeu.nom.trim().charAt(0).toLocaleUpperCase("fr-FR")
  }

  // "2026-09-12T20:00:00" → "samedi 12 septembre 2026 à 20:00"
  formaterDate(date: string): string {
    return FORMAT_DATE.format(new Date(date))
  }

  // 75 → "1 h 15", 45 → "45 min"
  formaterDuree(minutes: number): string {
    if (minutes < 60) return `${minutes} min`
    const reste = minutes % 60
    return `${Math.floor(minutes / 60)} h${reste > 0 ? " " + String(reste).padStart(2, "0") : ""}`
  }

  // ["Alex", "Léa", "Max"] → "Alex, Léa et Max"
  formaterVainqueurs(vainqueurs: string[]): string {
    if (vainqueurs.length === 0) return "Aucun vainqueur enregistré"
    if (vainqueurs.length === 1) return vainqueurs[0]
    return `${vainqueurs.slice(0, -1).join(", ")} et ${vainqueurs[vainqueurs.length - 1]}`
  }

  pluriel(nombre: number, mot: string): string {
    return `${nombre} ${mot}${nombre > 1 ? "s" : ""}`
  }
}
