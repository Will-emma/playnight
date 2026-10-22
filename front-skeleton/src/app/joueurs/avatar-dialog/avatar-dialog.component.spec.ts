import { Component } from "@angular/core"
import { ComponentFixture, TestBed } from "@angular/core/testing"
import { MatDialog, MatDialogModule, MatDialogRef } from "@angular/material/dialog"
import { MatTabsModule } from "@angular/material/tabs"
import { NoopAnimationsModule } from "@angular/platform-browser/animations"
import { OverlayContainer } from "@angular/cdk/overlay"
import { HttpErrorResponse } from "@angular/common/http"
import { of, throwError } from "rxjs"
import { Joueur } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"
import { AvatarDialogComponent } from "./avatar-dialog.component"

@Component({ template: "" })
class HoteTest {}

describe("Personnalisation de l’avatar", () => {
  let fixture: ComponentFixture<HoteTest>
  let ref: MatDialogRef<AvatarDialogComponent, Joueur>
  let service: jasmine.SpyObj<JoueurService>
  let overlay: HTMLElement
  const joueur: Joueur = { id: 42, pseudo: "Léa", avatar: null }

  beforeEach(async () => {
    service = jasmine.createSpyObj("JoueurService", ["choisirAvatar", "importerAvatar"])
    await TestBed.configureTestingModule({
      declarations: [HoteTest, AvatarDialogComponent],
      imports: [MatDialogModule, MatTabsModule, NoopAnimationsModule],
      providers: [{ provide: JoueurService, useValue: service }],
    }).compileComponents()
    fixture = TestBed.createComponent(HoteTest)
    ref = TestBed.inject(MatDialog).open(AvatarDialogComponent, { data: joueur, width: "560px", maxWidth: "94vw", panelClass: "avatar-dialog-panel" })
    overlay = TestBed.inject(OverlayContainer).getContainerElement()
    fixture.detectChanges()
    await fixture.whenStable()
  })

  afterEach(() => {
    TestBed.inject(MatDialog).closeAll()
    fixture.detectChanges()
    TestBed.inject(OverlayContainer).ngOnDestroy()
  })

  it("affiche 12 choix et prévisualise sans enregistrer", () => {
    const boutons = overlay.querySelectorAll<HTMLButtonElement>(".choix-avatar")
    expect(boutons.length).toBe(12)
    boutons[5].click()
    fixture.detectChanges()
    expect(overlay.querySelector<HTMLImageElement>(".apercu img")?.getAttribute("src")).toBe("/assets/avatars/avatar-06.png")
    expect(boutons[5].getAttribute("aria-pressed")).toBe("true")
    expect(service.choisirAvatar).not.toHaveBeenCalled()
  })

  it("enregistre le véritable id du joueur et renvoie le résultat API", () => {
    const resultat = { ...joueur, avatar: "/assets/avatars/avatar-06.png" }
    service.choisirAvatar.and.returnValue(of(resultat))
    const fermer = spyOn(ref, "close").and.callThrough()
    ref.componentInstance.choisir(6)
    ref.componentInstance.enregistrer()
    expect(service.choisirAvatar).toHaveBeenCalledWith(42, 6)
    expect(fermer).toHaveBeenCalledWith(resultat)
  })

  it("annule sans envoyer de requête", () => {
    ref.componentInstance.choisir(2)
    overlay.querySelector<HTMLButtonElement>(".annuler")!.click()
    expect(service.choisirAvatar).not.toHaveBeenCalled()
    expect(service.importerAvatar).not.toHaveBeenCalled()
  })

  it("conserve la modale et le choix en cas d’erreur serveur", () => {
    service.choisirAvatar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })))
    ref.componentInstance.choisir(3)
    ref.componentInstance.enregistrer()
    fixture.detectChanges()
    expect(overlay.querySelector('[role="alert"]')?.textContent).toContain("n’a pas pu être enregistré")
    expect(ref.componentInstance.avatarId).toBe(3)
    expect(ref.disableClose).toBeFalse()
  })

  it("explique un serveur inaccessible sans fermer la modale", () => {
    service.choisirAvatar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })))
    const fermer = spyOn(ref, "close").and.callThrough()
    ref.componentInstance.choisir(1)
    ref.componentInstance.enregistrer()
    expect(ref.componentInstance.erreur).toContain("Impossible de joindre le serveur")
    expect(fermer).not.toHaveBeenCalled()
  })

  it("explique une ancienne API sans exposer sa réponse technique", () => {
    service.choisirAvatar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404, error: { path: "/internal/path" } })))
    ref.componentInstance.choisir(1)
    ref.componentInstance.enregistrer()
    expect(ref.componentInstance.erreur).toContain("redémarrez l’API")
    expect(ref.componentInstance.erreur).not.toContain("/internal/path")
  })

  it("prévisualise une photo et libère son URL à la fermeture", async () => {
    const canvas = document.createElement("canvas")
    canvas.width = 8
    canvas.height = 8
    const blob = await new Promise<Blob>((resolve) => canvas.toBlob((b) => resolve(b!), "image/png"))
    const fichier = new File([blob], "photo.png", { type: "image/png" })
    ref.componentInstance.changerOnglet(1)
    await ref.componentInstance.importer({ target: { files: [fichier], value: "" } } as unknown as Event)
    expect(ref.componentInstance.photoUrl).toMatch(/^blob:/)
    expect(ref.componentInstance.peutEnregistrer).toBeTrue()
    const photoUrl = ref.componentInstance.photoUrl!
    const liberer = spyOn(URL, "revokeObjectURL").and.callThrough()
    ref.close()
    fixture.detectChanges()
    await fixture.whenStable()
    expect(liberer).toHaveBeenCalledWith(photoUrl)
    expect(service.importerAvatar).not.toHaveBeenCalled()
  })
})
