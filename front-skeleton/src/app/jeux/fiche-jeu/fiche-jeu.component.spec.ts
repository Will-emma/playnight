import { ComponentFixture, TestBed } from "@angular/core/testing"
import { ActivatedRoute } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { Subject } from "rxjs"
import { FicheJeu, MeilleurJoueur, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"
import { SuppressionComponent } from "../../suppression/suppression.component"
import { FicheJeuComponent } from "./fiche-jeu.component"
import { FicheJeuPageData } from "./fiche-jeu.resolver"

describe("FicheJeuComponent", () => {
  let fixture: ComponentFixture<FicheJeuComponent>
  let route: Subject<{ ficheJeuPage: FicheJeuPageData }>

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

  it("affiche la fiche d’un jeu physique nouvellement créé sans image ni description", () => {
    route.next({ ficheJeuPage: {
      fiche: {
        jeu: { id: 89, nom: "Mon jeu physique", type: TypeJeu.SOCIETE, plateforme: null, nbJoueursMin: 2, nbJoueursMax: 200, image: null, description: null },
        nbParties: 0, meilleursJoueurs: [], parties: [],
      }, erreur: null, erreurClassement: false, classement: [],
    } })
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector("#nom-jeu").textContent).toBe("Mon jeu physique")
    expect(fixture.nativeElement.querySelector(".caracteristiques").textContent).toContain("2 à 200 joueurs")
    expect(fixture.nativeElement.querySelector(".couverture__initiale").textContent).toBe("M")
    expect(fixture.nativeElement.textContent).toContain("Aucune partie pour l'instant")
  })

  describe("Classement par victoires sur la fiche d'un jeu", () => {
    const vainqueurs: MeilleurJoueur[] = [3, 2, 2, 1, 1, 1].map((nbVictoires, index) => ({
      joueur: { id: 101 + index, pseudo: ["Alice", "Benoît", "Chloé", "Dina", "Eli", "Farah"][index], avatar: null },
      nbVictoires,
    }))
    const fiche: FicheJeu = {
      jeu: { id: 88, nom: "Catan", type: TypeJeu.SOCIETE, plateforme: null, nbJoueursMin: 3, nbJoueursMax: 4, image: null },
      nbParties: 5, meilleursJoueurs: vainqueurs, parties: [],
    }

    function afficher(classement: MeilleurJoueur[] = vainqueurs, nbParties = 5): void {
      route.next({ ficheJeuPage: {
        fiche: { ...fiche, meilleursJoueurs: classement, nbParties }, erreur: null, erreurClassement: false,
        classement: nbParties > 0 ? [{ joueurId: 101, pseudo: "Alice", avatar: null, elo: 1088, nbParties, rang: 1 }] : [],
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

  describe("Fiches détaillées des quatre jeux de société", () => {
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
})
