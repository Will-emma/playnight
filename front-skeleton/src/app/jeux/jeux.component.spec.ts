import { ComponentFixture, TestBed } from "@angular/core/testing"
import { ActivatedRoute } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { Jeu, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"
import { JeuxComponent } from "./jeux.component"

describe("Nouveaux jeux dans la ludothèque", () => {
  let fixture: ComponentFixture<JeuxComponent>
  const jeux: Jeu[] = ["Undercover", "UNO", "Dames", "Échecs"].map((nom, index) => ({
    id: 701 + index, nom, type: TypeJeu.SOCIETE, plateforme: null,
    nbJoueursMin: nom === "Undercover" ? 3 : 2, nbJoueursMax: nom === "Undercover" ? 12 : nom === "UNO" ? 10 : 2,
    image: `/assets/jeux/${["undercover", "uno", "dames", "echecs"][index]}.png`,
    description: `Présentation de ${nom}`, dureeEstimee: nom === "Undercover" ? 15 : nom === "Échecs" ? 30 : 20,
  }))
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [JeuxComponent], imports: [RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { data: { jeuxPage: { jeux, error: false } } } } },
        { provide: JeuService, useValue: {} },
      ],
    }).compileComponents()
    fixture = TestBed.createComponent(JeuxComponent)
    fixture.detectChanges()
  })

  it("affiche les quatre cartes, leurs informations et les vrais liens de fiches", () => {
    const cartes = fixture.nativeElement.querySelectorAll(".game-card") as NodeListOf<HTMLElement>
    expect(cartes.length).toBe(4)
    jeux.forEach((jeu, i) => {
      expect(cartes[i].textContent).toContain(jeu.nom)
      expect(cartes[i].textContent).toContain(jeu.description!)
      expect(cartes[i].textContent).toContain(`env. ${jeu.dureeEstimee} min`)
      expect(cartes[i].querySelector("a")?.getAttribute("href")).toBe(`/jeux/${jeu.id}`)
    })
  })

  it("conserve le filtre société et le secours si une illustration échoue", () => {
    fixture.componentInstance.choisirFiltre(TypeJeu.SOCIETE)
    const image = fixture.nativeElement.querySelector(".game-card__cover-image") as HTMLImageElement
    image.dispatchEvent(new Event("error"))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelectorAll(".game-card").length).toBe(4)
    expect(fixture.nativeElement.querySelector(".cover-initial").textContent).toBe("U")
  })

  it("affiche un jeu physique créé et sépare les adaptations numériques dans les filtres", () => {
    const physique: Jeu = { id: 8901, nom: "Mon jeu physique", type: TypeJeu.SOCIETE, plateforme: null, nbJoueursMin: 2, nbJoueursMax: 200, image: null }
    const adaptation: Jeu = { ...physique, id: 8902, nom: "Monopoly numérique", type: TypeJeu.JEU_VIDEO, plateforme: "PC" }
    fixture.componentInstance.jeux = [...jeux, physique, adaptation]
    fixture.componentInstance.choisirFiltre(TypeJeu.SOCIETE)
    fixture.detectChanges()
    expect(fixture.nativeElement.textContent).toContain("Mon jeu physique")
    expect(fixture.nativeElement.textContent).not.toContain("Monopoly numérique")
    expect(fixture.nativeElement.querySelector('a[href="/jeux/8901"]')).not.toBeNull()
    fixture.componentInstance.choisirFiltre(TypeJeu.JEU_VIDEO)
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelectorAll(".game-card").length).toBe(1)
    expect(fixture.nativeElement.textContent).toContain("Monopoly numérique")
    expect(fixture.nativeElement.textContent).not.toContain("Mon jeu physique")
  })
})
