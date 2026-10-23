import { ComponentFixture, TestBed } from "@angular/core/testing"
import { ActivatedRoute, Router } from "@angular/router"
import { ReactiveFormsModule } from "@angular/forms"
import { MatStepperModule } from "@angular/material/stepper"
import { MatStepper } from "@angular/material/stepper"
import { By } from "@angular/platform-browser"
import { NoopAnimationsModule } from "@angular/platform-browser/animations"
import { Jeu, TypeJeu } from "models/jeu.model"
import { SoireeService } from "services/soiree.service"
import { NouvelleSoireeComponent } from "./nouvelle-soiree.component"

describe("Sélection des nouveaux jeux dans une soirée", () => {
  let fixture: ComponentFixture<NouvelleSoireeComponent>
  const jeux: Jeu[] = ["Undercover", "UNO", "Dames", "Échecs"].map((nom, index) => ({
    id: 801 + index, nom, type: TypeJeu.SOCIETE, plateforme: null, image: null,
    nbJoueursMin: nom === "Undercover" ? 3 : 2, nbJoueursMax: nom === "Undercover" ? 12 : nom === "UNO" ? 10 : 2,
  }))
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [NouvelleSoireeComponent], imports: [ReactiveFormsModule, MatStepperModule, NoopAnimationsModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { data: { nouvelleSoireePage: {
          jeux, error: false, joueurs: [1, 2, 3].map(id => ({ id, pseudo: `J${id}`, avatar: null })),
        } } } } },
        { provide: Router, useValue: { navigate: jasmine.createSpy() } },
        { provide: SoireeService, useValue: {} },
      ],
    }).compileComponents()
    fixture = TestBed.createComponent(NouvelleSoireeComponent)
    fixture.detectChanges()
  })

  it("permet de sélectionner chacun des quatre jeux depuis le formulaire réel", async () => {
    fixture.componentInstance.infos.patchValue({ date: "2026-10-22", lieu: "Test", joueurIds: [1, 2, 3] })
    const stepper = fixture.debugElement.query(By.directive(MatStepper)).componentInstance as MatStepper
    stepper.next()
    fixture.detectChanges()
    await fixture.whenStable()
    const select = fixture.nativeElement.querySelector('select[formControlName="jeuId"]') as HTMLSelectElement
    expect(select).not.toBeNull()
    for (const jeu of jeux) {
      const option = Array.from(select.options).find(o => o.textContent?.startsWith(jeu.nom))!
      expect(option).toBeDefined()
      select.value = option.value
      select.dispatchEvent(new Event("change"))
      fixture.detectChanges()
      expect(fixture.componentInstance.parties.controls[0].controls.jeuId.value).toBe(jeu.id)
    }
  })
})
