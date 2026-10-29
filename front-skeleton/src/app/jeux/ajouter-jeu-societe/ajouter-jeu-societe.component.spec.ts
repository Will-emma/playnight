import { HttpErrorResponse } from "@angular/common/http"
import { ComponentFixture, TestBed } from "@angular/core/testing"
import { Router } from "@angular/router"
import { Subject, of } from "rxjs"
import { Jeu, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"
import { AjouterJeuSocieteComponent } from "./ajouter-jeu-societe.component"

describe("Ajout manuel d’un jeu de société", () => {
  let fixture: ComponentFixture<AjouterJeuSocieteComponent>
  let composant: AjouterJeuSocieteComponent
  let service: jasmine.SpyObj<JeuService>
  let router: jasmine.SpyObj<Router>
  const cree: Jeu = { id: 89, nom: "Mon jeu", type: TypeJeu.SOCIETE, plateforme: null, nbJoueursMin: 2, nbJoueursMax: 4, image: "http://localhost:8080/jeux/images/image.png" }

  beforeEach(async () => {
    service = jasmine.createSpyObj("JeuService", ["create", "createAvecImage"])
    router = jasmine.createSpyObj("Router", ["navigate"])
    router.navigate.and.resolveTo(true)
    service.create.and.returnValue(of(cree))
    service.createAvecImage.and.returnValue(of(cree))
    await TestBed.configureTestingModule({
      imports: [AjouterJeuSocieteComponent],
      providers: [{ provide: JeuService, useValue: service }, { provide: Router, useValue: router }],
    }).compileComponents()
    fixture = TestBed.createComponent(AjouterJeuSocieteComponent)
    composant = fixture.componentInstance
    composant.formulaire.patchValue({ nom: "Mon jeu", nbJoueursMin: 2, nbJoueursMax: 4 })
    fixture.detectChanges()
  })

  function selection(fichier: File): Event {
    return { target: { files: [fichier], value: "image" } } as unknown as Event
  }

  async function image(): Promise<File> {
    const canvas = document.createElement("canvas")
    canvas.width = canvas.height = 4
    const blob = await new Promise<Blob>(resolve => canvas.toBlob(b => resolve(b!), "image/png"))
    return new File([blob], "jeu.png", { type: "image/png" })
  }

  for (const [minimum, maximum] of [[2, 4], [1, 200], [200, 200]]) {
    it(`crée un jeu physique de ${minimum} à ${maximum} joueurs sans description ni image`, () => {
      composant.formulaire.patchValue({ nom: "  Mon jeu  ", nbJoueursMin: minimum, nbJoueursMax: maximum })
      composant.enregistrer()
      expect(service.create).toHaveBeenCalledOnceWith({
        nom: "Mon jeu", type: TypeJeu.SOCIETE, nbJoueursMin: minimum, nbJoueursMax: maximum,
        description: null, image: null, plateforme: null, dureeEstimee: null,
      })
      expect(router.navigate).toHaveBeenCalledWith(["/jeux", 89])
    })
  }

  for (const [minimum, maximum] of [
    [null, 4], [2, null], [0, 4], [2, 0], [-1, 4], [2, -1],
    [1.5, 4], [2, 4.5], [2, 201], [201, 201], [201, 200], [4, 2],
  ]) {
    it(`refuse les effectifs ${minimum} à ${maximum} avant tout appel HTTP`, () => {
      composant.formulaire.patchValue({ nbJoueursMin: minimum, nbJoueursMax: maximum })
      composant.enregistrer()
      fixture.detectChanges()
      expect(composant.formulaire.invalid).toBeTrue()
      expect(service.create).not.toHaveBeenCalled()
      expect(service.createAvecImage).not.toHaveBeenCalled()
      expect(fixture.nativeElement.querySelector("small.erreur").textContent).toContain(
        minimum === 4 && maximum === 2
          ? "Le maximum doit être supérieur ou égal au minimum."
          : "Indiquez un nombre de joueurs compris entre 1 et 200.",
      )
    })
  }

  for (const nom of ["", " \t\n "]) {
    it("refuse un nom vide ou composé d’espaces", () => {
      composant.formulaire.controls.nom.setValue(nom)
      composant.enregistrer()
      expect(composant.formulaire.controls.nom.invalid).toBeTrue()
      expect(service.create).not.toHaveBeenCalled()
      expect(service.createAvecImage).not.toHaveBeenCalled()
    })
  }

  it("conserve la description facultative et les bornes des champs numériques", () => {
    composant.formulaire.controls.description.setValue("  Un jeu d’ambiance.  ")
    composant.enregistrer()
    expect(service.create).toHaveBeenCalledWith(jasmine.objectContaining({ description: "Un jeu d’ambiance." }))
    for (const champ of Array.from(fixture.nativeElement.querySelectorAll('input[type="number"]') as NodeListOf<HTMLInputElement>)) {
      expect(champ.min).toBe("1")
      expect(champ.max).toBe("200")
      expect(champ.step).toBe("1")
    }
  })

  it("conserve la création JSON sans image ou avec URL", () => {
    composant.formulaire.controls.image.setValue("https://example.com/jeu.png")
    composant.enregistrer()
    expect(service.create).toHaveBeenCalledWith(jasmine.objectContaining({ image: "https://example.com/jeu.png" }))
    expect(service.createAvecImage).not.toHaveBeenCalled()
  })

  it("permet de continuer sans image même dans le choix fichier", () => {
    composant.choisirSourceImage("FICHIER")
    composant.enregistrer()
    expect(service.create).toHaveBeenCalledOnceWith(jasmine.objectContaining({ image: null }))
    expect(service.createAvecImage).not.toHaveBeenCalled()
  })

  it("prévisualise le fichier et le transmet séparément des données du jeu", async () => {
    composant.formulaire.controls.image.setValue("ancienne URL invalide")
    composant.choisirSourceImage("FICHIER")
    const fichier = await image()
    await composant.importerImage(selection(fichier))
    fixture.detectChanges()
    expect(fixture.nativeElement.querySelector(".apercu-image img").getAttribute("src")).toMatch(/^blob:/)
    expect(composant.formulaire.valid).toBeTrue()
    composant.enregistrer()
    expect(service.createAvecImage).toHaveBeenCalledOnceWith(jasmine.objectContaining({ type: TypeJeu.SOCIETE, image: null }), fichier)
    expect(service.create).not.toHaveBeenCalled()
    expect(router.navigate).toHaveBeenCalledWith(["/jeux", 89])
  })

  it("refuse une fausse image et un fichier trop lourd", async () => {
    composant.choisirSourceImage("FICHIER")
    for (const fichier of [new File(["texte"], "faux.png", { type: "image/png" }), new File([new Uint8Array(2 * 1024 * 1024 + 1)], "gros.png", { type: "image/png" })]) {
      await composant.importerImage(selection(fichier))
      expect(composant.erreurImage).not.toBe("")
      expect(composant.fichier).toBeNull()
      composant.enregistrer()
    }
    expect(service.createAvecImage).not.toHaveBeenCalled()
  })

  it("libère l’aperçu et réactive le champ URL en changeant de choix", async () => {
    composant.choisirSourceImage("FICHIER")
    await composant.importerImage(selection(await image()))
    const apercu = composant.apercuImage!
    const liberer = spyOn(URL, "revokeObjectURL").and.callThrough()
    composant.choisirSourceImage("URL")
    expect(liberer).toHaveBeenCalledWith(apercu)
    expect(composant.formulaire.controls.image.enabled).toBeTrue()
    expect(composant.fichier).toBeNull()
    expect(composant.apercuImage).toBeNull()
  })

  it("bloque les doubles envois et permet de réessayer après erreur", async () => {
    const reponse = new Subject<Jeu>()
    service.createAvecImage.and.returnValue(reponse)
    composant.choisirSourceImage("FICHIER")
    await composant.importerImage(selection(await image()))
    composant.enregistrer()
    composant.enregistrer()
    composant.choisirSourceImage("URL")
    fixture.detectChanges()
    expect(service.createAvecImage).toHaveBeenCalledTimes(1)
    expect(composant.sourceImage).toBe("FICHIER")
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue()
    reponse.error(new HttpErrorResponse({ status: 413 }))
    expect(composant.enregistrement).toBeFalse()
    expect(composant.erreur).toContain("2 Mo")
    expect(composant.fichier).not.toBeNull()
  })
})
