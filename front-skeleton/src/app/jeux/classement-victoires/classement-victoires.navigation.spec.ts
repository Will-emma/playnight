import { TestBed } from "@angular/core/testing"
import { RouterTestingHarness, RouterTestingModule } from "@angular/router/testing"
import { of } from "rxjs"
import { AppRoutingModule } from "app-routing.module"
import { JeuService } from "services/jeu.service"
import { ClassementVictoiresComponent } from "./classement-victoires.component"

describe("Navigation vers le classement complet", () => {
  it("ouvre la vraie route Angular et recharge les données en changeant de jeu", async () => {
    const service = jasmine.createSpyObj("JeuService", ["getById", "getClassementVictoires"])
    service.getById.and.callFake((id: number) => of({ id, nom: `Jeu ${id}` }))
    service.getClassementVictoires.and.returnValue(of([
      { joueur: { id: 42, pseudo: "Léa", avatar: null }, nbVictoires: 3 },
    ]))
    await TestBed.configureTestingModule({
      imports: [AppRoutingModule, RouterTestingModule],
      providers: [{ provide: JeuService, useValue: service }],
    }).compileComponents()
    const harness = await RouterTestingHarness.create()
    await harness.navigateByUrl("/jeux/88/classement-victoires", ClassementVictoiresComponent)
    expect(harness.routeNativeElement?.textContent).toContain("Jeu 88")
    expect(harness.routeNativeElement?.querySelector(".joueur")?.getAttribute("href")).toBe("/joueurs/42")
    expect(service.getClassementVictoires).toHaveBeenCalledWith(88)
    await harness.navigateByUrl("/jeux/99/classement-victoires", ClassementVictoiresComponent)
    expect(harness.routeNativeElement?.textContent).toContain("Jeu 99")
    expect(service.getClassementVictoires).toHaveBeenCalledWith(99)
    expect(harness.routeNativeElement?.querySelector(".retour-jeu")?.getAttribute("href")).toBe("/jeux/99")
  })
})
