import { Component, DestroyRef, Inject, OnInit, inject } from "@angular/core"
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from "@angular/material/dialog"
import { takeUntilDestroyed } from "@angular/core/rxjs-interop"
import { Subscription, interval, timer } from "rxjs"
import { FormsModule } from "@angular/forms"
import { RouterLink } from "@angular/router"
import { Jeu, TypeJeu } from "models/jeu.model"
import { CriteresTirage, filtrerJeux, tirerJeu } from "utils/tirage-jeu"
import { JeuService } from "services/jeu.service"

export interface DonneesTirageJeu {
  jeux?: readonly Jeu[]
  typeInitial?: CriteresTirage["type"]
}

@Component({
  selector: "app-tirage-jeu-dialog",
  standalone: true,
  imports: [MatDialogModule, FormsModule, RouterLink],
  templateUrl: "./tirage-jeu-dialog.component.html",
  styleUrls: ["./tirage-jeu-dialog.component.scss"],
})
export class TirageJeuDialogComponent implements OnInit {
  jeux: readonly Jeu[]
  chargement = false
  erreurChargement = false
  tirageEnCours = false
  jeuDefilant: Jeu | null = null
  numeroTirage = 0
  private readonly destroyRef = inject(DestroyRef)
  private readonly animation = new Subscription()
  private requete?: Subscription
  private fermee = false
  private readonly chargerAuDemarrage: boolean
  type: NonNullable<CriteresTirage["type"]>
  nombreJoueurs: number | null = null
  effectifInvalide = false
  resultat: Jeu | null = null
  imageResultatIndisponible = false
  readonly idsExclus = new Set<number>()
  readonly effectifMaximum = Number.MAX_SAFE_INTEGER
  readonly types = [
    { valeur: "TOUS", libelle: "Tous les jeux" },
    { valeur: TypeJeu.SOCIETE, libelle: "Jeux de société" },
    { valeur: TypeJeu.JEU_VIDEO, libelle: "Jeux vidéo" },
  ]

  constructor(
    @Inject(MAT_DIALOG_DATA) data: DonneesTirageJeu | null,
    private jeuService: JeuService,
    dialogRef: MatDialogRef<TirageJeuDialogComponent>,
  ) {
    this.jeux = data?.jeux ?? []
    this.chargerAuDemarrage = data?.jeux === undefined
    this.type = data?.typeInitial ?? "TOUS"
    // beforeClosed annule dès le début de la fermeture, pas après sa transition.
    dialogRef.beforeClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.nettoyer())
    this.destroyRef.onDestroy(() => this.nettoyer())
  }

  ngOnInit(): void {
    if (this.chargerAuDemarrage) this.chargerJeux()
  }

  chargerJeux(): void {
    if (this.chargement || this.fermee) return
    this.chargement = true
    this.erreurChargement = false
    this.requete = this.jeuService.findAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (jeux) => { this.jeux = jeux; this.chargement = false },
      error: () => { this.erreurChargement = true; this.chargement = false },
    })
  }

  private nettoyer(): void {
    this.fermee = true
    this.animation.unsubscribe()
    this.requete?.unsubscribe()
  }

  get configurationBloquee(): boolean {
    return this.chargement || this.erreurChargement || this.tirageEnCours
  }

  // Les exclusions ne masquent pas les lignes : on peut toujours les décocher.
  get jeuxCompatibles(): Jeu[] {
    return filtrerJeux(this.jeux, { type: this.type, nombreJoueurs: this.nombreJoueurs })
  }

  get jeuxEligibles(): Jeu[] {
    return filtrerJeux(this.jeux, {
      type: this.type, nombreJoueurs: this.nombreJoueurs, idsExclus: this.idsExclus,
    })
  }

  changerType(type: typeof this.type): void {
    if (this.configurationBloquee) return
    this.type = type
    this.effacerResultat()
  }

  changerNombreJoueurs(event: Event): void {
    if (this.configurationBloquee) return
    const champ = event.target as HTMLInputElement
    // Distinguer une saisie non numérique d'un champ volontairement vide.
    this.nombreJoueurs = champ.validity.badInput ? NaN : champ.value === "" ? null : champ.valueAsNumber
    this.effectifInvalide = !champ.validity.valid
    this.effacerResultat()
  }

  changerExclusion(id: number, event: Event): void {
    if (this.configurationBloquee) return
    if ((event.target as HTMLInputElement).checked) this.idsExclus.add(id)
    else this.idsExclus.delete(id)
    this.effacerResultat()
  }

  tirer(): void {
    if (this.configurationBloquee || this.animation.closed) return
    const candidats = this.jeuxEligibles
    const selection = tirerJeu(candidats)
    this.effacerResultat()
    if (!selection) return
    this.numeroTirage++
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) {
      this.resultat = selection
      return
    }
    this.tirageEnCours = true
    let index = 0
    this.jeuDefilant = candidats[0]
    // Défilement déterministe : aucun appel au hasard pendant le suspense.
    const defilement = interval(120).subscribe(() => {
      index = (index + 1) % candidats.length
      this.jeuDefilant = candidats[index]
    })
    this.animation.add(defilement)
    this.animation.add(timer(2000).subscribe(() => {
      defilement.unsubscribe()
      this.jeuDefilant = null
      this.tirageEnCours = false
      this.resultat = selection
    }))
  }

  private effacerResultat(): void {
    this.resultat = null
    this.imageResultatIndisponible = false
  }

  libelleType(jeu: Jeu): string {
    return jeu.type === TypeJeu.SOCIETE ? "Société" : "Jeu vidéo"
  }

  libelleEffectif(jeu: Jeu): string {
    const min = jeu.nbJoueursMin
    const max = jeu.nbJoueursMax
    if (min == null || max == null) return "Effectif non renseigné"
    return min === max ? `${min} joueur${min === 1 ? "" : "s"}` : `${min}–${max} joueurs`
  }
}
