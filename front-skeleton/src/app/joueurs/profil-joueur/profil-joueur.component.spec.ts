import { JoueurService } from "services/joueur.service"
import { SuppressionComponent } from "../../suppression/suppression.component"
import { StatistiquesJoueurComponent } from "../statistiques-joueur/statistiques-joueur.component"
import { TypeJeu } from "models/jeu.model"
import { Location } from "@angular/common"
import { ComponentFixture, TestBed } from "@angular/core/testing"
import { By } from "@angular/platform-browser"
import { ActivatedRoute } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { MatDialog } from "@angular/material/dialog"
import { Subject, of } from "rxjs"
import { ProfilJoueurComponent } from "./profil-joueur.component"
import { ProfilJoueurPageData } from "./profil-joueur.resolver"

describe("Classement Elo sur le profil", () => {
  let fixture: ComponentFixture<ProfilJoueurComponent>
  let route: Subject<{ profilJoueurPage: ProfilJoueurPageData }>
  const profil = { joueur: { id: 42, pseudo: "Léa", avatar: null }, nbParties: 8, nbVictoires: 3, jeuxFavoris: [], historique: [] }

  beforeEach(async () => {
    route = new Subject()
    await TestBed.configureTestingModule({
      declarations: [ProfilJoueurComponent, SuppressionComponent],
      imports: [RouterTestingModule, StatistiquesJoueurComponent],
      providers: [
        { provide: JoueurService, useValue: jasmine.createSpyObj("JoueurService", ["delete"]) },
        { provide: ActivatedRoute, useValue: { data: route } },
        { provide: Location, useValue: { back: jasmine.createSpy() } },
        { provide: MatDialog, useValue: { open: jasmine.createSpy("open").and.returnValue({ afterClosed: () => of({ elo: 1007, rang: 1 }) }) } },
      ],
    }).compileComponents()
    fixture = TestBed.createComponent(ProfilJoueurComponent)
    fixture.detectChanges()
  })

  it("affiche les valeurs API et le lien du véritable jeu sans remplacer les victoires", () => {
    route.next({ profilJoueurPage: { profil, classementGlobal: { elo: 1088, rang: 3 }, erreur: null, erreurClassements: false, classements: [
      { jeu: { id: 87, nom: "Catan", type: TypeJeu.SOCIETE, image: null }, elo: 1145, rang: 2, nbParties: 8 },
    ] } })
    fixture.detectChanges()
    const section = fixture.nativeElement.querySelector(".elo-personnel") as HTMLElement
    expect(section.textContent).toContain("Catan")
    expect(section.textContent).toContain("1145")
    expect(section.textContent).toContain("2e")
    expect(section.querySelector("a")?.getAttribute("href")).toBe("/jeux/87")
    expect(fixture.componentInstance.profil?.nbVictoires).toBe(3)
  })

  it("affiche l’Elo et le rang globaux dans l’en-tête", () => {
    route.next({ profilJoueurPage: { profil, classementGlobal: { elo: 1088, rang: 3 }, erreur: null, erreurClassements: false, classements: [] } })
    fixture.detectChanges()
    const carte = fixture.nativeElement.querySelector(".entete .statistique--elo") as HTMLElement
    expect(carte.textContent).toContain("Elo global")
    expect(carte.textContent).toContain("1088")
    expect(carte.textContent).toContain("3e au classement global")
  })

  it("affiche le score initial sans inventer de rang", () => {
    route.next({ profilJoueurPage: { profil, classementGlobal: { elo: 1000, rang: null }, erreur: null, erreurClassements: false, classements: [] } })
    fixture.detectChanges()
    const carte = fixture.nativeElement.querySelector(".statistique--elo") as HTMLElement
    expect(carte.textContent).toContain("1000")
    expect(carte.textContent).toContain("Non classé")
  })

  it("signale une indisponibilité sans afficher de faux score global", () => {
    route.next({ profilJoueurPage: { profil, classementGlobal: null, erreur: null, erreurClassements: false, classements: [] } })
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".statistique--elo").textContent).toContain("Momentanément indisponible")
  })

  it("ouvre la courbe depuis la carte et actualise le score à la fermeture", () => {
    route.next({ profilJoueurPage: { profil, classementGlobal: null, erreur: null, erreurClassements: false, classements: [] } })
    fixture.detectChanges()
    fixture.nativeElement.querySelector(".elo-global-action").click()
    const dialog = TestBed.inject(MatDialog)
    expect(dialog.open).toHaveBeenCalled()
    expect((dialog.open as jasmine.Spy).calls.mostRecent().args[1].data.id).toBe(42)
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".statistique--elo").textContent).toContain("1007")
  })

  it("affiche un état vide pour un joueur sans classement", () => {
    route.next({ profilJoueurPage: { profil, classementGlobal: { elo: 1088, rang: 3 }, erreur: null, erreurClassements: false, classements: [] } })
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".elo-personnel").textContent).toContain("Aucun classement Elo")
  })

  it("garde le profil et distingue une erreur API d’un classement vide", () => {
    route.next({ profilJoueurPage: { profil, classementGlobal: { elo: 1088, rang: 3 }, erreur: null, erreurClassements: true, classements: [] } })
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector("#pseudo-joueur").textContent).toBe("Léa")
    expect(fixture.nativeElement.querySelector(".elo-personnel [role=alert]").textContent).toContain("indisponible")
  })

  it("transmet tout l'historique aux graphiques et les actualise au changement de profil", () => {
    const historique = Array.from({ length: 14 }, (_, i) => ({
      partieId: i + 1, date: "2026-10-03T20:00:00", rang: 1, points: null,
      jeu: { id: 8, nom: "Catan", type: TypeJeu.SOCIETE, image: null },
    }))
    route.next({ profilJoueurPage: { profil: { ...profil, nbParties: 14, nbVictoires: 14, historique }, classementGlobal: null, erreur: null, erreurClassements: false, classements: [] } })
    fixture.detectChanges()
    const stats = fixture.debugElement.query(By.directive(StatistiquesJoueurComponent)).componentInstance as StatistiquesJoueurComponent
    expect(fixture.componentInstance.historiqueAffiche.length).toBe(10)
    expect(stats.statistiques.victoires[0].victoires).toBe(14)
    route.next({ profilJoueurPage: { profil: { ...profil, joueur: { ...profil.joueur, id: 99, pseudo: "Tom" }, nbParties: 0, nbVictoires: 0 }, classementGlobal: null, erreur: null, erreurClassements: false, classements: [] } })
    fixture.detectChanges()
    expect(stats.statistiques.victoires).toEqual([])
    expect(fixture.nativeElement.querySelector("#pseudo-joueur").textContent).toBe("Tom")
  })

  it("conserve la modification d'avatar sur le profil avec les statistiques", () => {
    route.next({ profilJoueurPage: { profil, classementGlobal: null, erreur: null, erreurClassements: false, classements: [] } })
    fixture.detectChanges()
    const nouveau = { ...profil.joueur, avatar: "/assets/avatars/avatar-03.png" }
    const ouvrir = TestBed.inject(MatDialog).open as jasmine.Spy
    ouvrir.and.returnValue({ afterClosed: () => of(nouveau) })
    fixture.nativeElement.querySelector(".modifier-avatar").click()
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".entete__avatar img").getAttribute("src")).toBe(nouveau.avatar)
    expect(fixture.componentInstance.profil?.nbVictoires).toBe(3)
    expect(fixture.debugElement.query(By.directive(StatistiquesJoueurComponent))).not.toBeNull()
  })
})
