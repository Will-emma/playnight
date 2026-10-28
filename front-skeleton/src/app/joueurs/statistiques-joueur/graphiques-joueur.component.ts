import { Component, Input, OnChanges } from "@angular/core"
import { BaseChartDirective, provideCharts } from "ng2-charts"
import { BarController, BarElement, CategoryScale, ChartData, ChartOptions, LinearScale, LineController, LineElement, PointElement, Tooltip } from "chart.js"
import { calculerStatistiques, StatistiquesJoueur } from "./statistiques-joueur.utils"

@Component({
  selector: "app-graphiques-joueur",
  standalone: true,
  imports: [BaseChartDirective],
  providers: [provideCharts({ registerables: [BarController, BarElement, LineController, LineElement, PointElement, CategoryScale, LinearScale, Tooltip] })],
  templateUrl: "./graphiques-joueur.component.html",
  styleUrls: ["./graphiques-joueur.component.scss"],
})
export class GraphiquesJoueurComponent implements OnChanges {
  @Input({ required: true }) statistiques: StatistiquesJoueur = calculerStatistiques([])
  barres: ChartData<"bar", number[], string> = { labels: [], datasets: [] }
  courbe: ChartData<"line", number[], string> = { labels: [], datasets: [] }
  hauteurBarres = 260
  aucuneVictoire = true

  private readonly axe = { ticks: { color: "#c5c9d9", precision: 0 }, grid: { color: "#343a53" } }
  readonly optionsBarres: ChartOptions<"bar"> = {
    responsive: true, maintainAspectRatio: false, animation: false, indexAxis: "y",
    scales: {
      x: { ...this.axe, beginAtZero: true, suggestedMax: 1, title: { display: true, text: "Victoires", color: "#c5c9d9" } },
      y: { ...this.axe, ticks: { color: "#c5c9d9", autoSkip: false, callback: function(value) {
        const nom = this.getLabelForValue(Number(value))
        return nom.length > 22 ? nom.slice(0, 21) + "…" : nom
      } } },
    },
  }
  readonly optionsCourbe: ChartOptions<"line"> = {
    responsive: true, maintainAspectRatio: false, animation: false,
    interaction: { mode: "index", intersect: false },
    scales: {
      y: { ...this.axe, beginAtZero: true, suggestedMax: 1, title: { display: true, text: "Parties jouées", color: "#c5c9d9" } },
      x: { ...this.axe, ticks: { color: "#c5c9d9", maxTicksLimit: 6, maxRotation: 45, minRotation: 0 } },
    },
  }

  ngOnChanges(): void {
    const jeux = this.statistiques.victoires
    this.hauteurBarres = Math.max(260, jeux.length * 38 + 60)
    this.aucuneVictoire = jeux.every(j => j.victoires === 0)
    this.barres = {
      labels: jeux.map(j => jeux.filter(autre => autre.nom === j.nom).length > 1 ? `${j.nom} (jeu ${j.jeuId})` : j.nom),
      datasets: [{ label: "Victoires", data: jeux.map(j => j.victoires), backgroundColor: "#f8b83f", borderRadius: 5 }],
    }
    this.courbe = {
      labels: this.statistiques.mois.map(m => m.libelle),
      datasets: [{ label: "Parties jouées", data: this.statistiques.mois.map(m => m.parties), borderColor: "#2fc0b5", backgroundColor: "#2fc0b5", pointRadius: 4, pointHitRadius: 12, tension: 0 }],
    }
  }
}
