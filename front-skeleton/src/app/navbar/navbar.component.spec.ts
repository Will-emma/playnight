import { Component, NgZone } from "@angular/core"
import { ComponentFixture, TestBed } from "@angular/core/testing"
import { By } from "@angular/platform-browser"
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing"
import { MatDialog, MatDialogModule } from "@angular/material/dialog"
import { MatListModule } from "@angular/material/list"
import { MatTooltip, MatTooltipModule } from "@angular/material/tooltip"
import { NoopAnimationsModule } from "@angular/platform-browser/animations"
import { Router } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { OverlayContainer } from "@angular/cdk/overlay"
import { NavbarComponent } from "./navbar.component"

@Component({ selector: "app-navigation-test", template: "<navbar></navbar><router-outlet></router-outlet>" })
class NavigationTest {}
@Component({ selector: "app-page-test", template: "<p>Page de test</p>" })
class PageTest {}

describe("Tirage depuis la navigation", () => {
  let fixture: ComponentFixture<NavigationTest>
  let http: HttpTestingController
  let overlay: HTMLElement
  const routes = ["jeux", "joueurs", "soirees", "jeux/:id", "joueurs/:id", "soirees/:id"]
  const url = "http://localhost:8080/jeux"

  function bouton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector(".tirage-nav")
  }

  async function ouvrir(): Promise<void> {
    bouton().click()
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [NavbarComponent, NavigationTest, PageTest],
      imports: [HttpClientTestingModule, MatDialogModule, MatListModule, MatTooltipModule,
        NoopAnimationsModule, RouterTestingModule.withRoutes(routes.map(path => ({ path, component: PageTest })))],
    }).compileComponents()
    fixture = TestBed.createComponent(NavigationTest)
    http = TestBed.inject(HttpTestingController)
    overlay = TestBed.inject(OverlayContainer).getContainerElement()
    fixture.detectChanges()
    await fixture.whenStable()
  })

  afterEach(() => {
    TestBed.inject(MatDialog).closeAll()
    fixture.detectChanges()
    http.verify()
    TestBed.inject(OverlayContainer).ngOnDestroy()
  })

  it("place un bouton de dé accessible juste après les liens existants", () => {
    expect(Array.from(fixture.nativeElement.querySelectorAll("mat-nav-list a"),
      (a: unknown) => (a as HTMLElement).textContent!.trim())).toEqual(["Ludothèque", "Joueurs", "Soirées"])
    expect(bouton().previousElementSibling!.tagName).toBe("MAT-NAV-LIST")
    expect(bouton().getAttribute("aria-label")).toBe("Tirer un jeu au sort")
    expect(bouton().getAttribute("aria-haspopup")).toBe("dialog")
    expect(bouton().querySelector(".material-icons")!.textContent).toBe("casino")
    const tooltip = fixture.debugElement.query(By.directive(MatTooltip)).injector.get(MatTooltip)
    expect(tooltip.message).toBe("Tirer un jeu au sort")
    expect(bouton().tabIndex).toBe(0)
    http.expectNone(url)
  })

  for (const route of ["/jeux", "/joueurs", "/soirees", "/jeux/42", "/joueurs/42", "/soirees/42"]) {
    it(`ouvre la modale depuis ${route} sans navigation supplémentaire`, async () => {
      await TestBed.inject(NgZone).run(() => TestBed.inject(Router).navigateByUrl(route))
      fixture.detectChanges()
      await ouvrir()
      const requete = http.expectOne(url)
      expect(requete.request.method).toBe("GET")
      expect(requete.request.params.keys()).toEqual([])
      requete.flush([])
      fixture.detectChanges()
      expect(overlay.querySelectorAll('[role="dialog"]').length).toBe(1)
      expect(overlay.textContent).toContain("Quel jeu ce soir ?")
      expect(overlay.textContent).toContain("La ludothèque est vide")
      expect(TestBed.inject(Router).url).toBe(route)
    })
  }

  it("évite les ouvertures multiples et les requêtes dupliquées", async () => {
    bouton().click()
    await ouvrir()
    expect(overlay.querySelectorAll('[role="dialog"]').length).toBe(1)
    expect(TestBed.inject(MatDialog).openDialogs.length).toBe(1)
    const requete = http.expectOne(url)
    requete.flush([])
    fixture.detectChanges()
    await ouvrir()
    http.expectNone(url)
    expect(TestBed.inject(MatDialog).openDialogs.length).toBe(1)
  })

  it("charge un catalogue frais à la réouverture, sans cache périmé", async () => {
    await ouvrir()
    http.expectOne(url).flush([])
    overlay.querySelector<HTMLButtonElement>(".fermer")!.click()
    fixture.detectChanges()
    await fixture.whenStable()
    await ouvrir()
    http.expectOne(url).flush([])
    expect(TestBed.inject(MatDialog).openDialogs.length).toBe(1)
  })

  it("permet de réessayer après une erreur réseau", async () => {
    await ouvrir()
    http.expectOne(url).error(new ProgressEvent("error"))
    fixture.detectChanges()
    expect(overlay.querySelector('[role="alert"]')!.textContent).toContain("indisponible")
    overlay.querySelector<HTMLButtonElement>(".reessayer")!.click()
    fixture.detectChanges()
    http.expectOne(url).flush([])
    fixture.detectChanges()
    expect(overlay.querySelector('[role="alert"]')).toBeNull()
  })
})
