import { Component, Input, OnChanges } from "@angular/core"
import { PartieHistorique } from "models/joueur.model"
import { calculerStatistiques } from "./statistiques-joueur.utils"
import { GraphiquesJoueurComponent } from "./graphiques-joueur.component"

@Component({
  selector: "app-statistiques-joueur",
  standalone: true,
  imports: [GraphiquesJoueurComponent],
  templateUrl: "./statistiques-joueur.component.html",
  styleUrls: ["./statistiques-joueur.component.scss"],
})
export class StatistiquesJoueurComponent implements OnChanges {
  @Input({ required: true }) historique: readonly PartieHistorique[] = []
  statistiques = calculerStatistiques([])

  ngOnChanges(): void {
    this.statistiques = calculerStatistiques(this.historique)
  }
}
