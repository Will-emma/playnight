import { ComponentFixture, TestBed } from "@angular/core/testing"
import { By } from "@angular/platform-browser"
import { BaseChartDirective } from "ng2-charts"
import { GraphiquesJoueurComponent } from "./graphiques-joueur.component"
import { calculerStatistiques } from "./statistiques-joueur.utils"

describe("Rendu Chart.js des statistiques", () => {
  let fixture: ComponentFixture<GraphiquesJoueurComponent>
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [GraphiquesJoueurComponent] }).compileComponents()
    fixture = TestBed.createComponent(GraphiquesJoueurComponent)
  })

  it("crée les deux graphiques et met à jour leurs données", () => {
    const donnees = calculerStatistiques([])
    donnees.victoires = [{ jeuId: 8, nom: "Catan", victoires: 3 }]
    donnees.mois[11].parties = 2
    donnees.totalPeriode = 2
    fixture.componentRef.setInput("statistiques", donnees)
    fixture.detectChanges()
    const graphiques = fixture.debugElement.queryAll(By.directive(BaseChartDirective)).map(d => d.injector.get(BaseChartDirective))
    expect(graphiques.length).toBe(2)
    expect(graphiques[0].chart?.data.datasets[0].data).toEqual([3])
    expect(graphiques[1].chart?.data.datasets[0].data[11]).toBe(2)
    fixture.componentRef.setInput("statistiques", { ...donnees, victoires: [{ jeuId: 20, nom: "Azul", victoires: 1 }] })
    fixture.detectChanges()
    expect(graphiques[0].chart?.data.labels).toEqual(["Azul"])
    expect(graphiques[0].chart?.data.datasets[0].data).toEqual([1])
  })

  it("signale les zéros et distingue les jeux homonymes", () => {
    const donnees = calculerStatistiques([])
    donnees.victoires = [{ jeuId: 8, nom: "Catan", victoires: 0 }, { jeuId: 20, nom: "Catan", victoires: 0 }]
    fixture.componentRef.setInput("statistiques", donnees)
    fixture.detectChanges()
    expect(fixture.componentInstance.barres.labels).toEqual(["Catan (jeu 8)", "Catan (jeu 20)"])
    expect(fixture.nativeElement.textContent).toContain("Aucune victoire")
    expect(fixture.nativeElement.textContent).toContain("Aucune partie sur cette période")
  })

  it("libère les canvas à la destruction du profil", () => {
    fixture.componentRef.setInput("statistiques", calculerStatistiques([]))
    fixture.detectChanges()
    const directives = fixture.debugElement.queryAll(By.directive(BaseChartDirective)).map(d => d.injector.get(BaseChartDirective))
    fixture.destroy()
    expect(directives.every(d => d.chart === undefined)).toBeTrue()
  })
})
