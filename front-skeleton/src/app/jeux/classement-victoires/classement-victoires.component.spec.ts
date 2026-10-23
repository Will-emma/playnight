import { ComponentFixture, TestBed } from "@angular/core/testing"
import { ActivatedRoute } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { Subject } from "rxjs"
import { TypeJeu } from "models/jeu.model"
import { ClassementVictoiresComponent } from "./classement-victoires.component"
import { ClassementVictoiresPageData } from "./classement-victoires.resolver"

describe("Page du classement complet par victoires", () => {
  let fixture: ComponentFixture<ClassementVictoiresComponent>
  let route: Subject<{ classementVictoiresPage: ClassementVictoiresPageData }>
  const page: ClassementVictoiresPageData = {
    jeuId: 88,
    jeu: { id: 88, nom: "Catan", type: TypeJeu.SOCIETE, plateforme: null, nbJoueursMin: 3, nbJoueursMax: 4, image: null },
    classement: [3, 2, 2, 1, 1, 1].map((nbVictoires, i) => ({
      joueur: { id: 101 + i, pseudo: ["Alice", "Benoît", "Chloé", "Dina", "Eli", "Farah"][i], avatar: i === 0 ? "/avatar.png" : null },
      nbVictoires,
    })),
    erreur: null,
  }

  beforeEach(async () => {
    route = new Subject()
    await TestBed.configureTestingModule({
      imports: [ClassementVictoiresComponent, RouterTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { data: route } }],
    }).compileComponents()
    fixture = TestBed.createComponent(ClassementVictoiresComponent)
    fixture.detectChanges()
  })

  function afficher(donnees = page): void {
    route.next({ classementVictoiresPage: donnees })
    fixture.detectChanges()
  }

  it("affiche tous les joueurs, leurs victoires et les rangs partagés", () => {
    afficher()
    expect(fixture.nativeElement.querySelector(".jeu-nom").textContent).toBe("Catan")
    expect(fixture.nativeElement.querySelectorAll(".liste li").length).toBe(6)
    expect(fixture.componentInstance.lignes.map(l => l.rang)).toEqual([1, 2, 2, 4, 4, 4])
    const rangs = Array.from(fixture.nativeElement.querySelectorAll(".rang")) as HTMLElement[]
    expect(rangs.map(r => r.textContent?.trim())).toEqual(["Rang 1", "Rang 2", "Rang 2", "Rang 4", "Rang 4", "Rang 4"])
    const victoires = Array.from(fixture.nativeElement.querySelectorAll(".victoires strong")) as HTMLElement[]
    expect(victoires.map(v => v.textContent)).toEqual(["3", "2", "2", "1", "1", "1"])
    expect(fixture.nativeElement.querySelector(".liste").textContent).toContain("Farah")
  })

  it("utilise des liens de profil sur toute la ligne et un lien Retour au jeu", () => {
    afficher()
    const liens = Array.from(fixture.nativeElement.querySelectorAll("a.joueur")) as HTMLAnchorElement[]
    expect(liens.map(l => l.getAttribute("href"))).toEqual(page.classement.map(l => `/joueurs/${l.joueur.id}`))
    expect(liens[0].querySelector("img")).not.toBeNull()
    expect(liens[0].querySelector(".pseudo")?.textContent).toBe("Alice")
    expect(fixture.nativeElement.querySelector(".retour-jeu").getAttribute("href")).toBe("/jeux/88")
  })

  it("remplace une image en erreur par l'initiale du joueur", () => {
    afficher()
    fixture.nativeElement.querySelector(".avatar img").dispatchEvent(new Event("error"))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".avatar img")).toBeNull()
    expect(fixture.nativeElement.querySelector(".avatar").textContent.trim()).toBe("A")
  })

  it("affiche un état vide lorsqu'aucun joueur n'a gagné", () => {
    afficher({ ...page, classement: [] })
    expect(fixture.nativeElement.textContent).toContain("Aucune victoire pour l'instant")
    expect(fixture.nativeElement.querySelector(".liste")).toBeNull()
    expect(fixture.nativeElement.querySelector(".retour-jeu")).not.toBeNull()
  })

  it("distingue une erreur API d'un classement vide", () => {
    afficher({ ...page, jeu: null, classement: [], erreur: "indisponible" })
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain("momentanément indisponible")
    expect(fixture.nativeElement.textContent).not.toContain("Aucune victoire")
  })

  it("signale un jeu introuvable et propose la ludothèque", () => {
    afficher({ jeuId: null, jeu: null, classement: [], erreur: "introuvable" })
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain("Ce jeu n'existe pas")
    expect(fixture.nativeElement.querySelector(".retour-jeu").getAttribute("href")).toBe("/jeux")
  })

  it("recalcule les places lors du passage à un autre classement", () => {
    afficher()
    afficher({ ...page, jeuId: 99, classement: [page.classement[5]] })
    expect(fixture.componentInstance.lignes.map(l => l.rang)).toEqual([1])
    expect(fixture.nativeElement.querySelectorAll(".liste li").length).toBe(1)
    expect(fixture.nativeElement.querySelector(".retour-jeu").getAttribute("href")).toBe("/jeux/99")
  })
})
