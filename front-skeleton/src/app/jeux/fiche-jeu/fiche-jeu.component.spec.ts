import { ComponentFixture, TestBed } from "@angular/core/testing"
import { ActivatedRoute } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { Subject } from "rxjs"
import { FicheJeu, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"
import { SuppressionComponent } from "../../suppression/suppression.component"
import { FicheJeuComponent } from "./fiche-jeu.component"
import { FicheJeuPageData } from "./fiche-jeu.resolver"

describe("Fiches détaillées des quatre jeux de société", () => {
  let fixture: ComponentFixture<FicheJeuComponent>
  let route: Subject<{ ficheJeuPage: FicheJeuPageData }>
  const catalogue = [
    { nom: "Undercover", slug: "undercover", min: 3, max: 12, duree: 15 },
    { nom: "UNO", slug: "uno", min: 2, max: 10, duree: 20 },
    { nom: "Dames", slug: "dames", min: 2, max: 2, duree: 20 },
    { nom: "Échecs", slug: "echecs", min: 2, max: 2, duree: 30 },
  ]
  const fiches: FicheJeu[] = catalogue.map((jeu, i) => ({
    jeu: {
      id: 901 + i, nom: jeu.nom, type: TypeJeu.SOCIETE, plateforme: null,
      nbJoueursMin: jeu.min, nbJoueursMax: jeu.max,
      image: `/assets/jeux/${jeu.slug}.png`, description: `Présentation de ${jeu.nom}`, dureeEstimee: jeu.duree,
    },
    nbParties: 0, meilleursJoueurs: [], parties: [],
  }))

  beforeEach(async () => {
    route = new Subject()
    await TestBed.configureTestingModule({
      declarations: [FicheJeuComponent, SuppressionComponent],
      imports: [RouterTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { data: route } },
        { provide: JeuService, useValue: jasmine.createSpyObj("JeuService", ["delete"]) },
      ],
    }).compileComponents()
    fixture = TestBed.createComponent(FicheJeuComponent)
    fixture.detectChanges()
  })

  function afficher(fiche: FicheJeu): void {
    route.next({ ficheJeuPage: { fiche, classement: [], erreurClassement: false, erreur: null } })
    fixture.detectChanges()
  }

  catalogue.forEach((jeu, i) => {
    it(`affiche la fiche de ${jeu.nom}, ses informations et son état sans partie`, () => {
      afficher(fiches[i])
      const page = fixture.nativeElement as HTMLElement
      expect(page.querySelector("#nom-jeu")?.textContent).toBe(jeu.nom)
      expect(page.querySelector(".jeu-description")?.textContent).toContain(jeu.nom)
      expect(page.querySelector(".couverture__image")?.getAttribute("src")).toBe(fiches[i].jeu.image)
      const caracteristiques = page.querySelector(".caracteristiques")!.textContent!
      expect(caracteristiques).toContain(jeu.min === jeu.max ? "2 joueurs" : `${jeu.min} à ${jeu.max} joueurs`)
      expect(caracteristiques).toContain(`Env. ${jeu.duree} min`)
      expect(page.textContent).toContain("Aucune partie pour l'instant")
      expect(page.querySelector(".elo .vide")?.textContent).toContain("Aucune partie")
      expect(page.querySelector("app-suppression")).not.toBeNull()
    })
  })

  it("conserve un affichage de secours si l'image ne charge pas", () => {
    afficher(fiches[3])
    fixture.nativeElement.querySelector(".couverture__image").dispatchEvent(new Event("error"))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".couverture__initiale").textContent).toBe("É")
    expect(fixture.nativeElement.querySelector("#nom-jeu").textContent).toBe("Échecs")
  })

  it("affiche l'historique et l'Elo après une partie et retire la suppression", () => {
    const fiche: FicheJeu = {
      ...fiches[2], nbParties: 1,
      meilleursJoueurs: [{ joueur: { id: 51, pseudo: "Camille", avatar: null }, nbVictoires: 1 }],
      parties: [{ partieId: 72, date: "2026-10-24T20:00:00", duree: 25, vainqueurs: ["Camille"] }],
    }
    route.next({ ficheJeuPage: {
      fiche, erreur: null, erreurClassement: false,
      classement: [{ joueurId: 51, pseudo: "Camille", avatar: null, elo: 1016, nbParties: 1, rang: 1 }],
    } })
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".elo__points").textContent).toContain("1016")
    expect(fixture.nativeElement.querySelector(".elo__pseudo").getAttribute("href")).toBe("/joueurs/51")
    expect(fixture.nativeElement.querySelector(".parties").textContent).toContain("Camille")
    expect(fixture.nativeElement.querySelector(".parties").textContent).toContain("25 min")
    expect(fixture.nativeElement.querySelector("app-suppression")).toBeNull()
  })
})
