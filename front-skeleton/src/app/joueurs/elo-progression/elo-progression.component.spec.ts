import { RouterTestingModule } from "@angular/router/testing"
import { ComponentFixture, TestBed } from "@angular/core/testing"
import { MAT_DIALOG_DATA, MatDialogModule } from "@angular/material/dialog"
import { NoopAnimationsModule } from "@angular/platform-browser/animations"
import { of, Subject, throwError } from "rxjs"
import { ProgressionElo } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"
import { EloProgressionComponent } from "./elo-progression.component"

describe("Courbe de progression Elo", () => {
  let fixture: ComponentFixture<EloProgressionComponent>
  let service: jasmine.SpyObj<JoueurService>
  const donnees: ProgressionElo = {
    classement: { elo: 1007, rang: 1 }, meilleurElo: 1016, variationTotale: 7, nbParties: 2,
    points: [
      { partieId: null, date: null, jeu: null, elo: 1000, variation: 0 },
      { partieId: 11, date: "2026-01-01T20:00:00", jeu: "Catan", elo: 1016, variation: 16 },
      { partieId: 12, date: "2026-01-02T20:00:00", jeu: "Dixit", elo: 1007, variation: -9 },
    ],
  }
  beforeEach(async () => {
    service = jasmine.createSpyObj("JoueurService", ["getProgressionElo"])
    service.getProgressionElo.and.returnValue(of(donnees))
    await TestBed.configureTestingModule({
      declarations: [EloProgressionComponent], imports: [MatDialogModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { id: 42, pseudo: "Léa", avatar: null } },
        { provide: JoueurService, useValue: service },
      ],
    }).compileComponents()
    fixture = TestBed.createComponent(EloProgressionComponent)
  })

  it("charge les statistiques du joueur et dessine chaque étape réelle", () => {
    fixture.detectChanges()
    expect(service.getProgressionElo).toHaveBeenCalledWith(42)
    expect(fixture.nativeElement.querySelectorAll("circle").length).toBe(3)
    expect(fixture.nativeElement.querySelector(".resume").textContent).toContain("1007")
    expect(fixture.nativeElement.querySelector(".resume").textContent).toContain("1016")
    expect(fixture.nativeElement.querySelector(".detail-point").textContent).toContain("Dixit")
    expect(fixture.componentInstance.ligne).not.toContain("NaN")
  })

  it("permet d’explorer les étapes avec le curseur accessible au clavier", () => {
    fixture.detectChanges()
    const curseur = fixture.nativeElement.querySelector("input[type=range]") as HTMLInputElement
    curseur.value = "1"
    curseur.dispatchEvent(new Event("input"))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".detail-point").textContent).toContain("Catan")
    expect(fixture.nativeElement.querySelector(".detail-point").textContent).toContain("+16")
  })

  it("montre un état vide sans tracer de données fictives", () => {
    service.getProgressionElo.and.returnValue(of({ ...donnees, points: [], nbParties: 0 }))
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain("Aucune partie classée")
    expect(fixture.nativeElement.querySelector("svg")).toBeNull()
  })

  it("permet de réessayer après une erreur", () => {
    service.getProgressionElo.and.returnValue(throwError(() => new Error("indisponible")))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull()
    service.getProgressionElo.and.returnValue(of(donnees))
    fixture.nativeElement.querySelector('[role="alert"] button').click()
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector("svg")).not.toBeNull()
  })

  it("annule la lecture HTTP lorsque la fenêtre est détruite", () => {
    const reponse = new Subject<ProgressionElo>()
    service.getProgressionElo.and.returnValue(reponse)
    fixture.detectChanges()
    expect(reponse.observed).toBeTrue()
    fixture.destroy()
    expect(reponse.observed).toBeFalse()
  })
})
