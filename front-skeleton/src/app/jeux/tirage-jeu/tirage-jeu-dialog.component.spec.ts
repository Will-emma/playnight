import { Component } from "@angular/core"
import { ComponentFixture, TestBed, fakeAsync, flushMicrotasks, tick } from "@angular/core/testing"
import { OverlayContainer } from "@angular/cdk/overlay"
import { MatDialog, MatDialogRef } from "@angular/material/dialog"
import { NoopAnimationsModule } from "@angular/platform-browser/animations"
import { Router } from "@angular/router"
import { RouterTestingModule } from "@angular/router/testing"
import { Jeu, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"
import { Subject, of } from "rxjs"
import { DonneesTirageJeu, TirageJeuDialogComponent } from "./tirage-jeu-dialog.component"

@Component({ standalone: true, template: "" })
class HoteTirageTest {}

@Component({ standalone: true, template: "" })
class FicheJeuTest {}

describe("Configuration du tirage de jeu", () => {
  let fixture: ComponentFixture<HoteTirageTest>
  let ref: MatDialogRef<TirageJeuDialogComponent>
  let overlay: HTMLElement
  let service: jasmine.SpyObj<JeuService>
  let mouvements: jasmine.Spy
  let mouvementsReduits: boolean
  const jeux: readonly Jeu[] = Object.freeze([
    Object.freeze({ id: 10, nom: "Catan", type: TypeJeu.SOCIETE, plateforme: null, nbJoueursMin: 3, nbJoueursMax: 4, image: null }),
    Object.freeze({ id: 20, nom: "Mario Kart", type: TypeJeu.JEU_VIDEO, plateforme: "Switch", nbJoueursMin: 1, nbJoueursMax: 4, image: null }),
    Object.freeze({ id: 30, nom: "Échecs", type: TypeJeu.SOCIETE, plateforme: null, nbJoueursMin: 2, nbJoueursMax: 2, image: null }),
  ])

  async function ouvrir(data: DonneesTirageJeu = { jeux }): Promise<void> {
    TestBed.inject(MatDialog).closeAll()
    fixture.detectChanges()
    await fixture.whenStable()
    ref = TestBed.inject(MatDialog).open(TirageJeuDialogComponent, {
      data, width: "640px", maxWidth: "94vw", maxHeight: "90vh",
    })
    fixture.detectChanges()
    await fixture.whenStable()
    fixture.detectChanges()
  }

  function choisirType(type: string): void {
    const select = overlay.querySelector<HTMLSelectElement>("#tirage-type")!
    select.value = type
    select.dispatchEvent(new Event("change", { bubbles: true }))
    fixture.detectChanges()
  }

  function saisirEffectif(valeur: string): void {
    const input = overlay.querySelector<HTMLInputElement>("#tirage-effectif")!
    input.value = valeur
    input.dispatchEvent(new Event("input", { bubbles: true }))
    fixture.detectChanges()
  }

  function caseJeu(id: number): HTMLInputElement {
    return overlay.querySelector<HTMLInputElement>(`input[type="checkbox"][data-jeu-id="${id}"]`)!
  }

  function nomsAffiches(): string[] {
    return Array.from(overlay.querySelectorAll(".jeu-description strong"), (element) => element.textContent!.trim())
  }

  function compteur(): string {
    return overlay.querySelector(".compteur")!.textContent!.replace(/\s+/g, " ").trim()
  }

  function tirer(): void {
    overlay.querySelector<HTMLButtonElement>(".tirer")!.click()
    fixture.detectChanges()
  }

  beforeEach(async () => {
    service = jasmine.createSpyObj("JeuService", ["findAll"])
    service.findAll.and.returnValue(of([]))
    mouvementsReduits = true
    const matchMedia = window.matchMedia.bind(window)
    mouvements = spyOn(window, "matchMedia").and.callFake((query) => {
      const media = matchMedia(query)
      if (query === "(prefers-reduced-motion: reduce)") {
        Object.defineProperty(media, "matches", { value: mouvementsReduits })
      }
      return media
    })
    await TestBed.configureTestingModule({
      imports: [HoteTirageTest, TirageJeuDialogComponent, NoopAnimationsModule,
        RouterTestingModule.withRoutes([{ path: "jeux/:id", component: FicheJeuTest }])],
      providers: [{ provide: JeuService, useValue: service }],
    }).compileComponents()
    fixture = TestBed.createComponent(HoteTirageTest)
    overlay = TestBed.inject(OverlayContainer).getContainerElement()
    await ouvrir()
  })

  afterEach(() => {
    TestBed.inject(MatDialog).closeAll()
    fixture.detectChanges()
    TestBed.inject(OverlayContainer).ngOnDestroy()
  })

  it("s'ouvre via MatDialog avec le catalogue injecté et tous les jeux par défaut", () => {
    expect(overlay.querySelector('[role="dialog"]')).not.toBeNull()
    expect(ref.componentInstance.jeux).toBe(jeux)
    expect(nomsAffiches()).toEqual(["Catan", "Mario Kart", "Échecs"])
    expect(compteur()).toBe("3 jeux éligibles")
  })

  for (const type of [TypeJeu.SOCIETE, TypeJeu.JEU_VIDEO]) {
    it(`respecte le type initial ${type} reçu dans MAT_DIALOG_DATA`, async () => {
      await ouvrir({ jeux, typeInitial: type })
      expect(overlay.querySelector<HTMLSelectElement>("#tirage-type")!.value).toBe(type)
      expect(nomsAffiches()).toEqual(type === TypeJeu.SOCIETE ? ["Catan", "Échecs"] : ["Mario Kart"])
    })
  }

  it("actualise la liste et le compteur pour chaque filtre de type", () => {
    choisirType(TypeJeu.SOCIETE)
    expect(nomsAffiches()).toEqual(["Catan", "Échecs"])
    expect(compteur()).toBe("2 jeux éligibles")
    choisirType(TypeJeu.JEU_VIDEO)
    expect(nomsAffiches()).toEqual(["Mario Kart"])
    expect(compteur()).toBe("1 jeu éligible")
    choisirType("TOUS")
    expect(compteur()).toBe("3 jeux éligibles")
  })

  it("combine type et nombre exact, avec des bornes inclusives", () => {
    choisirType(TypeJeu.SOCIETE)
    saisirEffectif("3")
    expect(nomsAffiches()).toEqual(["Catan"])
    saisirEffectif("4")
    expect(nomsAffiches()).toEqual(["Catan"])
    saisirEffectif("2")
    expect(nomsAffiches()).toEqual(["Échecs"])
    expect(compteur()).toBe("1 jeu éligible")
  })

  it("désactive le filtre d'effectif quand le champ est vidé", () => {
    saisirEffectif("1")
    expect(nomsAffiches()).toEqual(["Mario Kart"])
    saisirEffectif("")
    expect(nomsAffiches()).toEqual(["Catan", "Mario Kart", "Échecs"])
    expect(ref.componentInstance.nombreJoueurs).toBeNull()
    expect(overlay.querySelector("#tirage-effectif-erreur")).toBeNull()
  })

  for (const valeur of ["0", "-2", "2.5", "9007199254740992"]) {
    it(`explique l'effectif invalide ${valeur} et ne propose aucun candidat`, () => {
      saisirEffectif(valeur)
      expect(compteur()).toBe("0 jeux éligibles")
      expect(nomsAffiches()).toEqual([])
      expect(overlay.querySelector("#tirage-effectif")!.getAttribute("aria-invalid")).toBe("true")
      expect(overlay.querySelector('[role="alert"]')!.textContent).toContain("entier positif")
      expect(overlay.querySelector(".etat-vide")!.textContent).toContain("Corrigez")
      saisirEffectif("2")
      expect(overlay.querySelector('[role="alert"]')).toBeNull()
      expect(compteur()).toBe("2 jeux éligibles")
    })
  }

  it("affiche un état explicite quand aucun jeu ne correspond aux critères", () => {
    saisirEffectif("12")
    expect(compteur()).toBe("0 jeux éligibles")
    expect(overlay.querySelector(".etat-vide")!.textContent).toContain("Aucun jeu ne correspond")
    expect(overlay.querySelector('[role="alert"]')).toBeNull()
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeTrue()
  })

  it("distingue une ludothèque vide d'un filtre sans résultat", async () => {
    await ouvrir({ jeux: [] })
    expect(compteur()).toBe("0 jeux éligibles")
    expect(overlay.querySelector(".etat-vide")!.textContent).toContain("La ludothèque est vide")
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeTrue()
  })

  it("exclut et réintègre un jeu par sa case, sans masquer sa ligne", () => {
    caseJeu(10).click()
    fixture.detectChanges()
    expect(compteur()).toBe("2 jeux éligibles")
    expect(caseJeu(10).checked).toBeTrue()
    expect(nomsAffiches()).toContain("Catan")
    expect(ref.componentInstance.jeuxEligibles.map((j) => j.id)).toEqual([20, 30])
    caseJeu(10).click()
    fixture.detectChanges()
    expect(compteur()).toBe("3 jeux éligibles")
    expect(caseJeu(10).checked).toBeFalse()
  })

  it("explique l'exclusion de tous les candidats et permet de les réintégrer", () => {
    for (const jeu of jeux) caseJeu(jeu.id).click()
    fixture.detectChanges()
    expect(compteur()).toBe("0 jeux éligibles")
    expect(overlay.querySelector(".etat-vide")!.textContent).toContain("Tous les jeux compatibles sont exclus")
    expect(overlay.querySelectorAll('input[type="checkbox"]').length).toBe(3)
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeTrue()
    caseJeu(20).click()
    fixture.detectChanges()
    expect(compteur()).toBe("1 jeu éligible")
    expect(overlay.querySelector(".etat-vide")).toBeNull()
  })

  it("conserve les exclusions lors d'un changement de type et d'effectif", () => {
    caseJeu(10).click()
    choisirType(TypeJeu.JEU_VIDEO)
    expect(compteur()).toBe("1 jeu éligible")
    choisirType(TypeJeu.SOCIETE)
    saisirEffectif("4")
    expect(caseJeu(10).checked).toBeTrue()
    expect(compteur()).toBe("0 jeux éligibles")
    saisirEffectif("")
    choisirType("TOUS")
    expect(compteur()).toBe("2 jeux éligibles")
  })

  it("distingue les exclusions de deux jeux portant le même nom", async () => {
    await ouvrir({ jeux: [{ ...jeux[0], id: 40, nom: "Même nom" }, { ...jeux[0], id: 41, nom: "Même nom" }] })
    caseJeu(40).click()
    fixture.detectChanges()
    expect(caseJeu(41).checked).toBeFalse()
    expect(ref.componentInstance.jeuxEligibles.map((j) => j.id)).toEqual([41])
    expect(compteur()).toBe("1 jeu éligible")
  })

  it("écarte les limites inconnues seulement quand un effectif est indiqué", async () => {
    await ouvrir({ jeux: [{ ...jeux[0], nbJoueursMin: null, nbJoueursMax: null }] })
    expect(compteur()).toBe("1 jeu éligible")
    expect(overlay.textContent).toContain("Effectif non renseigné")
    saisirEffectif("3")
    expect(compteur()).toBe("0 jeux éligibles")
  })

  it("utilise des contrôles natifs étiquetés et un compteur annoncé", () => {
    expect(overlay.querySelector<HTMLLabelElement>('label[for="tirage-type"]')!.control)
      .toBe(overlay.querySelector<HTMLSelectElement>("#tirage-type"))
    expect(overlay.querySelector<HTMLLabelElement>('label[for="tirage-effectif"]')!.control)
      .toBe(overlay.querySelector<HTMLInputElement>("#tirage-effectif"))
    expect(caseJeu(10).getAttribute("aria-label")).toBe("Exclure Catan")
    expect(caseJeu(10).tabIndex).toBe(0)
    caseJeu(10).focus()
    expect(document.activeElement).toBe(caseJeu(10))
    expect(caseJeu(10).labels!.length).toBe(1)
    expect(overlay.querySelectorAll('[aria-live="polite"]').length).toBe(1)
    expect(overlay.querySelector(".annonce-resultat")!.getAttribute("aria-live")).toBe("polite")
  })

  it("active le tirage avec des candidats et affiche un seul résultat sans fermer", () => {
    const hasard = spyOn(Math, "random").and.returnValue(0.5)
    const bouton = overlay.querySelector<HTMLButtonElement>(".tirer")!
    expect(bouton.disabled).toBeFalse()
    expect(overlay.querySelector(".resultat")).toBeNull()
    const fermer = spyOn(ref, "close").and.callThrough()
    tirer()
    expect(hasard).toHaveBeenCalledTimes(1)
    expect(ref.componentInstance.resultat).toBe(jeux[1])
    expect(overlay.querySelectorAll(".resultat").length).toBe(1)
    expect(overlay.querySelector(".resultat")!.textContent).toContain("Mario Kart")
    expect(overlay.querySelector(".resultat")!.textContent).toContain("Jeu vidéo")
    expect(Array.from(overlay.querySelectorAll(".resultat dd"), (e) => e.textContent!.trim())).toEqual(["1", "4"])
    expect(overlay.querySelector(".annonce-resultat")!.textContent).toContain("Jeu tiré au sort : Mario Kart")
    expect(fermer).not.toHaveBeenCalled()
    expect(compteur()).toBe("3 jeux éligibles")
  })

  it("ne tire que parmi les candidats autorisés par tous les critères", () => {
    choisirType(TypeJeu.SOCIETE)
    saisirEffectif("3")
    tirer()
    expect(ref.componentInstance.resultat).toBe(jeux[0])
    choisirType("TOUS")
    caseJeu(10).click()
    fixture.detectChanges()
    tirer()
    expect(ref.componentInstance.resultat).toBe(jeux[1])
  })

  it("relance avec les mêmes critères et remplace le résultat précédent", () => {
    const hasard = spyOn(Math, "random").and.returnValues(0, 0.999999)
    choisirType(TypeJeu.SOCIETE)
    tirer()
    expect(ref.componentInstance.resultat).toBe(jeux[0])
    expect(overlay.querySelector(".tirer")!.textContent).toContain("Relancer le tirage")
    tirer()
    expect(ref.componentInstance.resultat).toBe(jeux[2])
    expect(overlay.querySelectorAll(".resultat").length).toBe(1)
    expect(ref.componentInstance.type).toBe(TypeJeu.SOCIETE)
    expect(compteur()).toBe("2 jeux éligibles")
    expect(hasard).toHaveBeenCalledTimes(2)
  })

  it("autorise le même résultat lors d'une relance et conserve les exclusions", () => {
    spyOn(Math, "random").and.returnValue(0)
    caseJeu(10).click()
    fixture.detectChanges()
    tirer()
    expect(ref.componentInstance.resultat).toBe(jeux[1])
    tirer()
    expect(ref.componentInstance.resultat).toBe(jeux[1])
    expect(caseJeu(10).checked).toBeTrue()
    expect(compteur()).toBe("2 jeux éligibles")
  })

  for (const critere of ["type", "effectif", "exclusion", "réintégration"]) {
    it(`efface le résultat après changement de ${critere}`, () => {
      spyOn(Math, "random").and.returnValue(0)
      if (critere === "réintégration") {
        caseJeu(30).click()
        fixture.detectChanges()
      }
      tirer()
      expect(overlay.querySelector(".resultat")).not.toBeNull()
      if (critere === "type") choisirType(TypeJeu.JEU_VIDEO)
      else if (critere === "effectif") saisirEffectif("4")
      else {
        caseJeu(30).click()
        fixture.detectChanges()
      }
      expect(ref.componentInstance.resultat).toBeNull()
      expect(overlay.querySelector(".resultat")).toBeNull()
      expect(overlay.querySelector(".tirer")!.textContent).toContain("Tirer un jeu au sort")
    })
  }

  it("efface le résultat et désactive le tirage avec un effectif invalide ou sans correspondance", () => {
    tirer()
    saisirEffectif("0")
    expect(ref.componentInstance.resultat).toBeNull()
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeTrue()
    saisirEffectif("12")
    expect(overlay.querySelector(".etat-vide")!.textContent).toContain("Aucun jeu ne correspond")
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeTrue()
    saisirEffectif("")
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeFalse()
  })

  it("ne lance aucune sélection sur un catalogue vide, même par appel direct", async () => {
    await ouvrir({ jeux: [] })
    const hasard = spyOn(Math, "random")
    tirer()
    ref.componentInstance.tirer()
    expect(hasard).not.toHaveBeenCalled()
    expect(ref.componentInstance.resultat).toBeNull()
    expect(overlay.querySelector(".resultat")).toBeNull()
  })

  it("affiche l'image fournie et une alternative lorsqu'elle échoue", async () => {
    const image = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"
    await ouvrir({ jeux: [{ ...jeux[0], image }] })
    tirer()
    const img = overlay.querySelector<HTMLImageElement>(".resultat-image")!
    expect(img.getAttribute("src")).toBe(image)
    expect(img.alt).toBe("Illustration de Catan")
    img.dispatchEvent(new Event("error"))
    fixture.detectChanges()
    expect(overlay.querySelector(".resultat-image")).toBeNull()
    expect(overlay.querySelector(".resultat-remplacement")!.getAttribute("aria-label")).toContain("Catan")
    expect(overlay.querySelector(".resultat-remplacement")!.textContent).toContain("Illustration indisponible")
    tirer()
    expect(overlay.querySelector(".resultat-image")).not.toBeNull()
  })

  it("gère une image absente, un effectif fixe et des limites non renseignées", async () => {
    await ouvrir({ jeux: [jeux[2]] })
    tirer()
    expect(overlay.querySelector(".resultat-remplacement")).not.toBeNull()
    expect(Array.from(overlay.querySelectorAll(".resultat dd"), (e) => e.textContent!.trim())).toEqual(["2", "2"])
    await ouvrir({ jeux: [{ ...jeux[0], nbJoueursMin: null, nbJoueursMax: null }] })
    tirer()
    expect(Array.from(overlay.querySelectorAll(".resultat dd"), (e) => e.textContent!.trim()))
      .toEqual(["Non renseigné", "Non renseigné"])
  })

  it("ouvre la fiche avec le véritable id du résultat et ferme la modale", async () => {
    spyOn(Math, "random").and.returnValue(0.999999)
    tirer()
    const lien = overlay.querySelector<HTMLAnchorElement>(".voir-fiche")!
    expect(lien.getAttribute("href")).toBe("/jeux/30")
    const fermer = spyOn(ref, "close").and.callThrough()
    lien.click()
    fixture.detectChanges()
    await fixture.whenStable()
    expect(fermer).toHaveBeenCalled()
    expect(TestBed.inject(Router).url).toBe("/jeux/30")
  })

  it("ferme la fenêtre sans résultat et réinitialise les exclusions à la prochaine ouverture", async () => {
    caseJeu(10).click()
    const fermer = spyOn(ref, "close").and.callThrough()
    overlay.querySelector<HTMLButtonElement>(".fermer")!.click()
    expect(fermer).toHaveBeenCalled()
    await ouvrir()
    expect(compteur()).toBe("3 jeux éligibles")
    expect(caseJeu(10).checked).toBeFalse()
    expect(ref.componentInstance.jeux).toBe(jeux)
  })

  it("ne recharge pas un catalogue déjà fourni", () => {
    expect(service.findAll).not.toHaveBeenCalled()
  })

  it("charge le catalogue complet une seule fois et bloque le tirage en attendant", async () => {
    const reponse = new Subject<Jeu[]>()
    service.findAll.and.returnValue(reponse)
    await ouvrir({})
    expect(service.findAll).toHaveBeenCalledOnceWith()
    expect(overlay.textContent).toContain("Chargement de la ludothèque")
    expect(overlay.textContent).not.toContain("La ludothèque est vide")
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeTrue()
    ref.componentInstance.chargerJeux()
    expect(service.findAll).toHaveBeenCalledTimes(1)
    reponse.next([...jeux])
    reponse.complete()
    fixture.detectChanges()
    expect(compteur()).toBe("3 jeux éligibles")
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeFalse()
  })

  it("explique l'erreur API sans exposer sa réponse et permet de réessayer", async () => {
    const reponse = new Subject<Jeu[]>()
    service.findAll.and.returnValue(reponse)
    await ouvrir({})
    reponse.error(new Error("secret technique"))
    fixture.detectChanges()
    expect(overlay.querySelector('[role="alert"]')!.textContent).toContain("momentanément indisponible")
    expect(overlay.textContent).not.toContain("secret technique")
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeTrue()
    service.findAll.and.returnValue(of([...jeux]))
    overlay.querySelector<HTMLButtonElement>(".reessayer")!.click()
    fixture.detectChanges()
    expect(service.findAll).toHaveBeenCalledTimes(2)
    expect(compteur()).toBe("3 jeux éligibles")
    expect(overlay.querySelector('[role="alert"]')).toBeNull()
  })

  it("affiche l'état vide pour une réponse API vide", async () => {
    await ouvrir({})
    expect(overlay.textContent).toContain("La ludothèque est vide")
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeTrue()
  })

  it("annule la requête en cours à la fermeture", async () => {
    const reponse = new Subject<Jeu[]>()
    service.findAll.and.returnValue(reponse)
    await ouvrir({})
    expect(reponse.observed).toBeTrue()
    ref.close()
    expect(reponse.observed).toBeFalse()
  })

  it("sélectionne une fois au début et révèle ce résultat après deux secondes", fakeAsync(() => {
    mouvementsReduits = false
    const hasard = spyOn(Math, "random").and.returnValue(0.999999)
    tirer()
    expect(ref.componentInstance.resultat).toBeNull()
    expect(overlay.querySelector(".suspense")).not.toBeNull()
    expect(overlay.querySelector(".annonce-resultat")!.textContent!.trim()).toBe("")
    expect(hasard).toHaveBeenCalledTimes(1)
    const premier = ref.componentInstance.jeuDefilant
    tick(120)
    fixture.detectChanges()
    expect(ref.componentInstance.jeuDefilant).not.toBe(premier)
    tick(1879)
    expect(ref.componentInstance.resultat).toBeNull()
    tick(1)
    fixture.detectChanges()
    expect(ref.componentInstance.resultat).toBe(jeux[2])
    expect(overlay.querySelector(".suspense")).toBeNull()
    expect(hasard).toHaveBeenCalledTimes(1)
  }))

  it("bloque les doubles clics et toutes les modifications pendant le suspense", fakeAsync(() => {
    mouvementsReduits = false
    const hasard = spyOn(Math, "random").and.returnValue(0)
    tirer()
    tirer()
    ref.componentInstance.tirer()
    flushMicrotasks()
    fixture.detectChanges()
    expect(overlay.querySelector<HTMLSelectElement>("#tirage-type")!.disabled).toBeTrue()
    expect(overlay.querySelector<HTMLInputElement>("#tirage-effectif")!.disabled).toBeTrue()
    expect(caseJeu(10).disabled).toBeTrue()
    ref.componentInstance.changerType(TypeJeu.JEU_VIDEO)
    saisirEffectif("12")
    caseJeu(10).click()
    expect(ref.componentInstance.type).toBe("TOUS")
    expect(ref.componentInstance.nombreJoueurs).toBeNull()
    expect(ref.componentInstance.idsExclus.size).toBe(0)
    expect(hasard).toHaveBeenCalledTimes(1)
    tick(2000)
    fixture.detectChanges()
    expect(ref.componentInstance.resultat).toBe(jeux[0])
    expect(overlay.querySelector<HTMLButtonElement>(".tirer")!.disabled).toBeFalse()
  }))

  it("annule les temporisations dès la fermeture sans révéler de résultat", fakeAsync(() => {
    mouvementsReduits = false
    tirer()
    tick(120)
    const composant = ref.componentInstance
    ref.close()
    const defilant = composant.jeuDefilant
    tick(3000)
    expect(composant.resultat).toBeNull()
    expect(composant.jeuDefilant).toBe(defilant)
  }))

  it("annule aussi les temporisations à la destruction du composant", fakeAsync(() => {
    mouvementsReduits = false
    tirer()
    ref.componentRef!.destroy()
    tick(3000)
    expect(ref.componentInstance.resultat).toBeNull()
  }))

  it("révèle immédiatement le résultat en mode mouvements réduits sans timer", fakeAsync(() => {
    spyOn(Math, "random").and.returnValue(0)
    tirer()
    expect(mouvements).toHaveBeenCalledWith("(prefers-reduced-motion: reduce)")
    expect(ref.componentInstance.resultat).toBe(jeux[0])
    expect(ref.componentInstance.tirageEnCours).toBeFalse()
    expect(overlay.querySelector(".suspense")).toBeNull()
    tick(3000)
    expect(ref.componentInstance.resultat).toBe(jeux[0])
  }))
})
