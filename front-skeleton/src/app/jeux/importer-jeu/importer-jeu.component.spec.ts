import { HttpErrorResponse } from "@angular/common/http"
import { ComponentFixture, TestBed } from "@angular/core/testing"
import { FormsModule } from "@angular/forms"
import { By } from "@angular/platform-browser"
import { Router } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { Subject, of, throwError } from "rxjs"
import { Jeu, RawgJeu, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"
import { AjouterJeuSocieteComponent } from "../ajouter-jeu-societe/ajouter-jeu-societe.component"
import { ImporterJeuComponent } from "./importer-jeu.component"

describe("Choix des parcours d’ajout d’un jeu", () => {
  let fixture: ComponentFixture<ImporterJeuComponent>
  let component: ImporterJeuComponent
  let service: jasmine.SpyObj<JeuService>
  let navigation: jasmine.Spy
  const rawg: RawgJeu = {
    idRawg: 2074, nom: "UNO", image: "/assets/jeux/uno.png",
    plateforme: "PC, Nintendo Switch", dateSortie: "2016-08-16",
  }
  const importe: Jeu = {
    id: 89, nom: "UNO", type: TypeJeu.JEU_VIDEO, plateforme: "PC, Nintendo Switch",
    nbJoueursMin: 2, nbJoueursMax: 4, image: null,
  }

  beforeEach(async () => {
    service = jasmine.createSpyObj<JeuService>("JeuService", ["rechercherSurRawg", "importerDepuisRawg", "create"])
    service.rechercherSurRawg.and.returnValue(of([rawg]))
    service.importerDepuisRawg.and.returnValue(of(importe))
    service.create.and.returnValue(of({ ...importe, type: TypeJeu.SOCIETE }))
    await TestBed.configureTestingModule({
      declarations: [ImporterJeuComponent],
      imports: [FormsModule, RouterTestingModule, AjouterJeuSocieteComponent],
      providers: [{ provide: JeuService, useValue: service }],
    }).compileComponents()
    navigation = spyOn(TestBed.inject(Router), "navigate").and.resolveTo(true)
    fixture = TestBed.createComponent(ImporterJeuComponent)
    component = fixture.componentInstance
    fixture.detectChanges()
  })

  function choisir(parcours: "video" | "adaptation" | "physique"): void {
    const principaux = fixture.nativeElement.querySelectorAll('.choix-types:not(.choix-societe) button') as NodeListOf<HTMLButtonElement>
    principaux[parcours === "video" ? 0 : 1].click()
    fixture.detectChanges()
    if (parcours !== "video") {
      const modes = fixture.nativeElement.querySelectorAll('.choix-societe button') as NodeListOf<HTMLButtonElement>
      modes[parcours === "adaptation" ? 0 : 1].click()
      fixture.detectChanges()
    }
  }

  function rechercher(): void {
    component.recherche = "  UNO  "
    fixture.detectChanges()
    fixture.nativeElement.querySelector("form.recherche").dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }))
    fixture.detectChanges()
  }

  it("ne sélectionne aucun parcours et ne lance aucun appel à l’ouverture", () => {
    expect(component.typeChoisi).toBeNull()
    expect(fixture.nativeElement.querySelector("form")).toBeNull()
    expect(service.rechercherSurRawg).not.toHaveBeenCalled()
    expect(service.create).not.toHaveBeenCalled()
  })

  it("propose les deux sous-choix société sans présélection ni recherche", () => {
    component.choisirType("SOCIETE")
    fixture.detectChanges()
    expect(component.modeSociete).toBeNull()
    expect(fixture.nativeElement.querySelectorAll('.choix-societe button').length).toBe(2)
    expect(fixture.nativeElement.querySelector('.choix-societe [aria-pressed="true"]')).toBeNull()
    expect(fixture.nativeElement.querySelector("form")).toBeNull()
    expect(service.rechercherSurRawg).not.toHaveBeenCalled()
  })

  for (const parcours of ["video", "adaptation"] as const) {
    it(`réutilise la recherche RAWG avec images et plateformes dans le parcours ${parcours}`, () => {
      choisir(parcours)
      expect(service.rechercherSurRawg).not.toHaveBeenCalled()
      expect(fixture.nativeElement.querySelectorAll("form.recherche").length).toBe(1)
      expect(fixture.nativeElement.querySelector("app-ajouter-jeu-societe")).toBeNull()
      rechercher()
      expect(service.rechercherSurRawg).toHaveBeenCalledOnceWith("UNO")
      const carte: HTMLElement = fixture.nativeElement.querySelector(".resultat")
      expect(carte.textContent).toContain("UNO")
      expect(carte.textContent).toContain("PC, Nintendo Switch")
      expect(carte.querySelector("img")?.getAttribute("src")).toBe(rawg.image!)
      expect(carte.querySelector(".mention-adaptation") !== null).toBe(parcours === "adaptation")
      if (parcours === "adaptation") {
        expect(carte.querySelector(".mention-adaptation")?.textContent).toBe("Adaptation numérique — Jeu vidéo")
        expect(service.importerDepuisRawg).not.toHaveBeenCalled()
      }
      expect(fixture.nativeElement.querySelector("#titre-import").textContent)
        .toBe(parcours === "adaptation" ? "Adaptation numérique du jeu de société" : "Importer un jeu vidéo")
    })

    it(`importe via RAWG avec les effectifs saisis et ouvre la fiche dans le parcours ${parcours}`, () => {
      choisir(parcours)
      rechercher()
      component.resultats[0].nbJoueursMin = 2
      component.resultats[0].nbJoueursMax = 4
      fixture.nativeElement.querySelector(".resultat__ajout button").click()
      expect(service.importerDepuisRawg).toHaveBeenCalledOnceWith(2074, { nbJoueursMin: 2, nbJoueursMax: 4 })
      expect(service.create).not.toHaveBeenCalled()
      expect(navigation).toHaveBeenCalledWith(["/jeux", 89])
    })

    it(`bloque les changements de parcours pendant la recherche ${parcours}`, () => {
      const reponse = new Subject<RawgJeu[]>()
      service.rechercherSurRawg.and.returnValue(reponse)
      choisir(parcours)
      rechercher()
      expect(Array.from(fixture.nativeElement.querySelectorAll(".choix-type") as NodeListOf<HTMLButtonElement>)
        .every(bouton => bouton.disabled)).toBeTrue()
      component.choisirType(parcours === "video" ? "SOCIETE" : "JEU_VIDEO")
      component.choisirModeSociete("MANUEL")
      expect(component.typeChoisi).toBe(parcours === "video" ? "JEU_VIDEO" : "SOCIETE")
      expect(component.modeSociete).toBe(parcours === "video" ? null : "RAWG")
      component.rechercher()
      expect(service.rechercherSurRawg).toHaveBeenCalledTimes(1)
      reponse.next([rawg])
      reponse.complete()
      fixture.detectChanges()
      expect(component.changementTypeBloque).toBeFalse()
    })

    it(`bloque les changements et les doubles imports pendant l’enregistrement ${parcours}`, () => {
      const reponse = new Subject<Jeu>()
      service.importerDepuisRawg.and.returnValue(reponse)
      choisir(parcours)
      rechercher()
      const resultat = component.resultats[0]
      fixture.nativeElement.querySelector(".resultat__ajout button").click()
      fixture.detectChanges()
      expect(fixture.nativeElement.querySelector(".resultat__ajout button").disabled).toBeTrue()
      expect(Array.from(fixture.nativeElement.querySelectorAll(".choix-type") as NodeListOf<HTMLButtonElement>)
        .every(bouton => bouton.disabled)).toBeTrue()
      component.choisirType(parcours === "video" ? "SOCIETE" : "JEU_VIDEO")
      component.choisirModeSociete("MANUEL")
      component.ajouter(resultat)
      component.rechercher()
      expect(service.rechercherSurRawg).toHaveBeenCalledTimes(1)
      expect(fixture.nativeElement.querySelector('form.recherche button').disabled).toBeTrue()
      expect(service.importerDepuisRawg).toHaveBeenCalledTimes(1)
      expect(component.afficherRawg).toBeTrue()
      reponse.error(new HttpErrorResponse({ status: 400, error: { message: "Ce jeu est déjà dans la ludothèque." } }))
      fixture.detectChanges()
      expect(component.changementTypeBloque).toBeFalse()
      expect(fixture.nativeElement.querySelector(".resultat__erreur").textContent).toContain("déjà dans la ludothèque")
    })
  }

  it("affiche les sept exemples de jeux dans la recherche d’adaptations", () => {
    choisir("adaptation")
    const champ: HTMLInputElement = fixture.nativeElement.querySelector("#recherche-rawg")
    for (const nom of ["Monopoly", "UNO", "Catan", "Carcassonne", "Risk", "Scrabble", "Ticket to Ride"]) {
      expect(champ.placeholder).toContain(nom)
    }
  })

  it("ne permet pas d’importer un ancien résultat pendant une nouvelle recherche", () => {
    choisir("video")
    rechercher()
    const ancienResultat = component.resultats[0]
    const reponse = new Subject<RawgJeu[]>()
    service.rechercherSurRawg.and.returnValue(reponse)
    rechercher()
    component.ajouter(ancienResultat)
    expect(service.importerDepuisRawg).not.toHaveBeenCalled()
    expect(fixture.nativeElement.querySelector(".resultat__ajout button").disabled).toBeTrue()
    reponse.next([rawg])
    reponse.complete()
    fixture.detectChanges()
    expect(component.changementTypeBloque).toBeFalse()
  })

  for (const parcours of ["video", "adaptation"] as const) {
    it(`n’impose pas la limite société de 200 au parcours ${parcours}`, () => {
      choisir(parcours)
      rechercher()
      const resultat = component.resultats[0]
      resultat.nbJoueursMin = 201
      resultat.nbJoueursMax = 250
      component.ajouter(resultat)
      expect(service.importerDepuisRawg).toHaveBeenCalledOnceWith(2074, { nbJoueursMin: 201, nbJoueursMax: 250 })
      expect(service.create).not.toHaveBeenCalled()
    })
  }

  it("efface résultats, effectifs saisis, erreurs et images indisponibles au changement de parcours", () => {
    choisir("video")
    rechercher()
    component.resultats[0].erreur = "Ancienne erreur d’import"
    component.resultats[0].nbJoueursMin = 8
    component.erreurRecherche = "Ancienne erreur de recherche"
    component.marquerImageIndisponible(rawg)
    choisir("adaptation")
    expect(component.recherche).toBe("")
    expect(component.rechercheEnvoyee).toBeNull()
    expect(component.resultats).toEqual([])
    expect(component.erreurRecherche).toBeNull()
    expect(component.imagesIndisponibles.size).toBe(0)
    expect(service.rechercherSurRawg).toHaveBeenCalledTimes(1)
    rechercher()
    component.choisirModeSociete("MANUEL")
    fixture.detectChanges()
    expect(component.resultats).toEqual([])
    expect(fixture.nativeElement.querySelector("form.recherche")).toBeNull()
    expect(fixture.nativeElement.querySelector("app-ajouter-jeu-societe")).not.toBeNull()
  })

  it("réinitialise le sous-choix en quittant puis revenant sur Jeu de société", () => {
    choisir("adaptation")
    choisir("video")
    component.choisirType("SOCIETE")
    fixture.detectChanges()
    expect(component.modeSociete).toBeNull()
    expect(fixture.nativeElement.querySelector("form")).toBeNull()
  })

  it("ne réinitialise pas une recherche en recliquant le choix déjà actif", () => {
    choisir("adaptation")
    rechercher()
    component.choisirType("SOCIETE")
    component.choisirModeSociete("RAWG")
    expect(component.resultats.length).toBe(1)
    expect(component.rechercheEnvoyee).toBe("UNO")
  })

  it("conserve les validations RAWG et n’importe pas des effectifs invalides", () => {
    choisir("adaptation")
    rechercher()
    for (const [min, max, message] of [
      [null, 4, "minimum et maximum"], [1.5, 4, "minimum et maximum"],
      [0, 4, "au moins 1"], [4, 2, "inférieur au minimum"],
    ] as const) {
      const resultat = component.resultats[0]
      resultat.nbJoueursMin = min
      resultat.nbJoueursMax = max
      component.ajouter(resultat)
      expect(resultat.erreur).toContain(message)
    }
    expect(service.importerDepuisRawg).not.toHaveBeenCalled()
  })

  it("conserve les erreurs de recherche et les efface en passant au physique", () => {
    service.rechercherSurRawg.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 503, error: { message: "RAWG est indisponible." },
    })))
    choisir("adaptation")
    rechercher()
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain("RAWG est indisponible")
    expect(component.changementTypeBloque).toBeFalse()
    component.choisirModeSociete("MANUEL")
    fixture.detectChanges()
    expect(component.erreurRecherche).toBeNull()
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull()
  })

  it("conserve les états sans résultat et sans image", () => {
    service.rechercherSurRawg.and.returnValue(of([]))
    choisir("adaptation")
    rechercher()
    expect(fixture.nativeElement.textContent).toContain("Aucun jeu trouvé")
    service.rechercherSurRawg.and.returnValue(of([rawg]))
    rechercher()
    fixture.nativeElement.querySelector(".resultat__image img").dispatchEvent(new Event("error"))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".resultat__image img")).toBeNull()
    expect(fixture.nativeElement.querySelector(".resultat__image").textContent).toContain("U")
  })

  it("réutilise le vrai formulaire physique et envoie SOCIETE sans appeler RAWG", () => {
    choisir("physique")
    expect(fixture.nativeElement.querySelector("form.recherche")).toBeNull()
    const manuel = fixture.debugElement.query(By.directive(AjouterJeuSocieteComponent))
      .componentInstance as AjouterJeuSocieteComponent
    manuel.formulaire.patchValue({ nom: "  Mon jeu  ", nbJoueursMin: 2, nbJoueursMax: 6 })
    fixture.nativeElement.querySelector("app-ajouter-jeu-societe form").dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }))
    expect(service.create).toHaveBeenCalledOnceWith({
      nom: "Mon jeu", type: TypeJeu.SOCIETE, nbJoueursMin: 2, nbJoueursMax: 6,
      description: null, image: null, plateforme: null, dureeEstimee: null,
    })
    expect(service.rechercherSurRawg).not.toHaveBeenCalled()
    expect(service.importerDepuisRawg).not.toHaveBeenCalled()
    expect(navigation).toHaveBeenCalledWith(["/jeux", 89])
  })

  it("bloque les deux niveaux de sélection pendant la création physique et les débloque après erreur", () => {
    const reponse = new Subject<Jeu>()
    service.create.and.returnValue(reponse)
    choisir("physique")
    const manuel = fixture.debugElement.query(By.directive(AjouterJeuSocieteComponent))
      .componentInstance as AjouterJeuSocieteComponent
    manuel.formulaire.patchValue({ nom: "Mon jeu", nbJoueursMin: 1, nbJoueursMax: 4 })
    manuel.enregistrer()
    fixture.detectChanges()
    expect(Array.from(fixture.nativeElement.querySelectorAll(".choix-type") as NodeListOf<HTMLButtonElement>)
      .every(bouton => bouton.disabled)).toBeTrue()
    component.choisirType("JEU_VIDEO")
    component.choisirModeSociete("RAWG")
    expect(component.modeSociete).toBe("MANUEL")
    expect(component.typeChoisi).toBe("SOCIETE")
    manuel.enregistrer()
    expect(service.create).toHaveBeenCalledTimes(1)
    reponse.error(new HttpErrorResponse({ status: 0 }))
    fixture.detectChanges()
    expect(component.changementTypeBloque).toBeFalse()
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain("ne répond pas")
    choisir("adaptation")
    expect(component.afficherRawg).toBeTrue()
  })
})
