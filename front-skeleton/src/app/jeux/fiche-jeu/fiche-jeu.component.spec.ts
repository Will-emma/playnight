import { ComponentFixture, TestBed } from "@angular/core/testing"
import { ActivatedRoute } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { Subject } from "rxjs"
import { FicheJeu, MeilleurJoueur, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"
import { SuppressionComponent } from "../../suppression/suppression.component"
import { FicheJeuComponent } from "./fiche-jeu.component"
import { FicheJeuPageData } from "./fiche-jeu.resolver"

describe("Classement par victoires sur la fiche d'un jeu", () => {
  let fixture: ComponentFixture<FicheJeuComponent>
  let route: Subject<{ ficheJeuPage: FicheJeuPageData }>
  const vainqueurs: MeilleurJoueur[] = [3, 2, 2, 1, 1, 1].map((nbVictoires, index) => ({
    joueur: { id: 101 + index, pseudo: ["Alice", "Benoît", "Chloé", "Dina", "Eli", "Farah"][index], avatar: null },
    nbVictoires,
  }))
  const fiche: FicheJeu = {
    jeu: { id: 88, nom: "Catan", type: TypeJeu.SOCIETE, plateforme: null, nbJoueursMin: 3, nbJoueursMax: 4, image: null },
    nbParties: 5, meilleursJoueurs: vainqueurs, parties: [],
  }

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

  function afficher(classement: MeilleurJoueur[] = vainqueurs, nbParties = 5): void {
    route.next({ ficheJeuPage: {
      fiche: { ...fiche, meilleursJoueurs: classement, nbParties }, erreur: null, erreurClassement: false,
      classement: [{ joueurId: 101, pseudo: "Alice", avatar: null, elo: 1088, nbParties: 5, rang: 1 }],
    } })
    fixture.detectChanges()
  }

  it("affiche les places 1, 2, 2, 4 et conserve l'Elo distinct", () => {
    afficher()
    expect(fixture.nativeElement.querySelector("#titre-classement").textContent).toBe("Classement par victoires")
    expect(vainqueurs.slice(0, 4).map(v => fixture.componentInstance.place(v))).toEqual([1, 2, 2, 4])
    const socles = Array.from(fixture.nativeElement.querySelectorAll(".podium__socle")) as HTMLElement[]
    expect(socles.map(s => s.textContent?.replace(/\s+/g, " ").trim())).toEqual(["Place 2", "Place 1", "Place 2"])
    const suivants = Array.from(fixture.nativeElement.querySelectorAll(".classement__place")) as HTMLElement[]
    expect(suivants.map(s => s.textContent?.trim())).toEqual(["4", "4"])
    expect(fixture.nativeElement.querySelector(".elo__points").textContent).toContain("1088")
    expect(fixture.nativeElement.querySelector(".podium__victoires").textContent).toContain("2 victoires")
  })

  it("limite l'aperçu à cinq joueurs et propose la page complète au-delà", () => {
    afficher()
    const lien = fixture.nativeElement.querySelector(".bloc a.bouton--discret") as HTMLAnchorElement
    expect(lien.textContent).toBe("Voir tout le classement")
    expect(lien.getAttribute("href")).toBe("/jeux/88/classement-victoires")
    expect(fixture.nativeElement.querySelectorAll(".podium__marche").length).toBe(3)
    expect(fixture.nativeElement.querySelectorAll(".classement__ligne").length).toBe(2)
    expect(fixture.nativeElement.querySelector(".classement").textContent).not.toContain("Farah")
  })

  it("ne propose pas le classement complet pour cinq joueurs ou moins", () => {
    afficher(vainqueurs.slice(0, 5))
    expect(fixture.nativeElement.querySelector(".bloc a.bouton--discret")).toBeNull()
    afficher([])
    expect(fixture.nativeElement.querySelector(".bloc a.bouton--discret")).toBeNull()
  })

  it("conserve les liens normaux vers les vrais identifiants des joueurs", () => {
    afficher()
    const liens = Array.from(fixture.nativeElement.querySelectorAll(".podium__joueur, .classement__pseudo")) as HTMLAnchorElement[]
    expect(liens.map(l => l.getAttribute("href"))).toEqual([
      "/joueurs/102", "/joueurs/101", "/joueurs/103", "/joueurs/104", "/joueurs/105",
    ])
  })

  it("partage aussi la première place entre plusieurs vainqueurs ex aequo", () => {
    afficher(vainqueurs.slice(0, 3).map(v => ({ ...v, nbVictoires: 2 })))
    expect(fixture.nativeElement.querySelectorAll(".podium__marche--1").length).toBe(3)
    expect(fixture.nativeElement.querySelectorAll(".podium__couronne").length).toBe(3)
  })

  it("affiche un état vide sans inventer de joueur vainqueur", () => {
    afficher([])
    expect(fixture.nativeElement.textContent).toContain("Aucun vainqueur enregistré")
    expect(fixture.nativeElement.querySelector(".podium")).toBeNull()
  })

  it("garde la fiche d'un jeu jamais joué et son action de suppression", () => {
    afficher([], 0)
    expect(fixture.nativeElement.textContent).toContain("Aucune partie pour l'instant")
    expect(fixture.nativeElement.querySelector("app-suppression")).not.toBeNull()
  })

  it("actualise l'aperçu au changement de fiche", () => {
    afficher()
    afficher([vainqueurs[3]])
    expect(fixture.nativeElement.querySelectorAll(".podium__marche").length).toBe(1)
    expect(fixture.nativeElement.querySelector(".podium").textContent).toContain("Dina")
  })
})
