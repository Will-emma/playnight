import { ComponentFixture, DeferBlockBehavior, DeferBlockState, TestBed } from "@angular/core/testing"
import { TypeJeu } from "models/jeu.model"
import { PartieHistorique } from "models/joueur.model"
import { StatistiquesJoueurComponent } from "./statistiques-joueur.component"

describe("Statistiques accessibles du joueur", () => {
  let fixture: ComponentFixture<StatistiquesJoueurComponent>
  const historique: PartieHistorique[] = Array.from({ length: 14 }, (_, i) => ({
    partieId: i + 1, date: "2026-10-03T20:00:00", jeu: { id: 8, nom: "Catan", type: TypeJeu.SOCIETE, image: null },
    rang: i < 12 ? 1 : 2, points: null,
  }))
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [StatistiquesJoueurComponent], deferBlockBehavior: DeferBlockBehavior.Manual }).compileComponents()
    fixture = TestBed.createComponent(StatistiquesJoueurComponent)
    fixture.componentRef.setInput("historique", historique)
    fixture.detectChanges()
  })

  it("fournit les valeurs exactes dans des tableaux accessibles avant de charger Chart.js", () => {
    fixture.nativeElement.querySelector("summary").click()
    expect(fixture.nativeElement.querySelector("details").open).toBeTrue()
    const tableaux = fixture.nativeElement.querySelectorAll("table") as NodeListOf<HTMLTableElement>
    expect(tableaux.length).toBe(2)
    expect(tableaux[0].caption?.textContent).toContain("historique complet")
    expect(tableaux[0].querySelector('th[scope="row"]')?.textContent).toBe("Catan")
    expect(tableaux[0].querySelector("td")?.textContent).toBe("12")
    expect(tableaux[1].querySelectorAll("tbody tr").length).toBe(12)
    expect(fixture.nativeElement.querySelector("canvas")).toBeNull()
  })

  it("charge deux vrais canvas dans le bloc différé", async () => {
    const [bloc] = await fixture.getDeferBlocks()
    await bloc.render(DeferBlockState.Complete)
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelectorAll('canvas[role="img"]').length).toBe(2)
    expect(fixture.nativeElement.querySelector("canvas").getAttribute("aria-label")).toContain("tableaux")
  })

  it("actualise les valeurs quand l'historique du profil change", () => {
    fixture.componentRef.setInput("historique", [{ ...historique[0], jeu: { ...historique[0].jeu, id: 20, nom: "Azul" } }])
    fixture.detectChanges()
    expect(fixture.componentInstance.statistiques.victoires).toEqual([{ jeuId: 20, nom: "Azul", victoires: 1 }])
    expect(fixture.nativeElement.querySelector("table").textContent).not.toContain("Catan")
  })

  it("gère le profil sans partie sans créer de canvas", () => {
    fixture.componentRef.setInput("historique", [])
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain("Aucune partie enregistrée")
    expect(fixture.nativeElement.querySelector("canvas")).toBeNull()
    expect(fixture.componentInstance.statistiques.totalPeriode).toBe(0)
  })

  it("conserve les tableaux si le module des graphiques échoue à charger", async () => {
    const [bloc] = await fixture.getDeferBlocks()
    await bloc.render(DeferBlockState.Error)
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain("tableaux")
    expect(fixture.nativeElement.querySelectorAll("table").length).toBe(2)
  })
})
