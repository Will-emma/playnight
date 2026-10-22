import { Component, Inject, OnDestroy, OnInit } from "@angular/core"
import { MAT_DIALOG_DATA } from "@angular/material/dialog"
import { Subscription } from "rxjs"
import { Joueur, PointElo, ProgressionElo } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"
import { formaterDate, pluriel } from "utils/format"

@Component({
  selector: "app-elo-progression",
  templateUrl: "./elo-progression.component.html",
  styleUrls: ["./elo-progression.component.scss"],
})
export class EloProgressionComponent implements OnInit, OnDestroy {
  progression: ProgressionElo | null = null
  chargement = false
  erreur = false
  selection = 0
  minimum = 980
  maximum = 1020
  private requete?: Subscription
  readonly pluriel = pluriel
  readonly formaterDate = formaterDate

  constructor(@Inject(MAT_DIALOG_DATA) readonly joueur: Joueur, private service: JoueurService) {}

  ngOnInit(): void { this.charger() }

  charger(): void {
    this.requete?.unsubscribe()
    this.chargement = true
    this.erreur = false
    this.requete = this.service.getProgressionElo(this.joueur.id).subscribe({
      next: (progression) => {
        this.progression = progression
        this.selection = Math.max(0, progression.points.length - 1)
        this.minimum = Math.floor((progression.points.reduce((min, p) => Math.min(min, p.elo), 1000) - 10) / 20) * 20
        this.maximum = Math.ceil((progression.points.reduce((max, p) => Math.max(max, p.elo), 1000) + 10) / 20) * 20
        this.chargement = false
      },
      error: () => { this.chargement = false; this.erreur = true },
    })
  }

  ngOnDestroy(): void { this.requete?.unsubscribe() }

  get points(): PointElo[] { return this.progression?.points ?? [] }
  get pointSelectionne(): PointElo | undefined { return this.points[this.selection] }
  get graduations(): number[] { return [this.minimum, (this.minimum + this.maximum) / 2, this.maximum] }
  x(index: number): number { return 54 + index * 650 / Math.max(1, this.points.length - 1) }
  y(elo: number): number { return 214 - (elo - this.minimum) * 184 / (this.maximum - this.minimum) }
  get ligne(): string { return this.points.map((p, i) => `${this.x(i)},${this.y(p.elo)}`).join(" ") }
  get surface(): string { return `54,214 ${this.ligne} ${this.x(this.points.length - 1)},214` }
  variation(valeur: number): string { return `${valeur > 0 ? "+" : ""}${valeur}` }

  choisirPoint(event: Event): void { this.selection = Number((event.target as HTMLInputElement).value) }
}
