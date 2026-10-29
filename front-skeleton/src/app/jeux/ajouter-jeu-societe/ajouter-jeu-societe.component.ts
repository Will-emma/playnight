import { HttpErrorResponse } from "@angular/common/http"
import { Component, DestroyRef, EventEmitter, Output, inject } from "@angular/core"
import { takeUntilDestroyed } from "@angular/core/rxjs-interop"
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from "@angular/forms"
import { Router } from "@angular/router"
import { JeuCreation, TypeJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"
import { validerPhoto } from "utils/avatar"

function entier(control: AbstractControl): ValidationErrors | null {
  return control.value == null || Number.isInteger(control.value) ? null : { entier: true }
}

function effectifCoherent(control: AbstractControl): ValidationErrors | null {
  const min = control.get("nbJoueursMin")?.value
  const max = control.get("nbJoueursMax")?.value
  return min != null && max != null && max < min ? { effectif: true } : null
}

function urlImage(control: AbstractControl): ValidationErrors | null {
  const valeur = control.value.trim()
  if (!valeur) return null
  try {
    const url = new URL(valeur)
    return ["https:", "http:"].includes(url.protocol) && !!url.hostname ? null : { url: true }
  } catch {
    return { url: true }
  }
}

@Component({
  selector: "app-ajouter-jeu-societe",
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: "./ajouter-jeu-societe.component.html",
  styleUrls: ["./ajouter-jeu-societe.component.scss"],
})
export class AjouterJeuSocieteComponent {
  @Output() enregistrementChange = new EventEmitter<boolean>()
  private readonly fb = inject(FormBuilder)
  private readonly destroyRef = inject(DestroyRef)
  private readonly service = inject(JeuService)
  private readonly router = inject(Router)

  readonly formulaire = this.fb.group({
    nom: this.fb.nonNullable.control("", [Validators.required, Validators.pattern(/\S/)]),
    nbJoueursMin: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(200), entier]),
    nbJoueursMax: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.max(200), entier]),
    description: this.fb.nonNullable.control(""),
    image: this.fb.nonNullable.control("", urlImage),
  }, { validators: effectifCoherent })

  enregistrement = false
  erreur = ""
  sourceImage: "URL" | "FICHIER" = "URL"
  fichier: File | null = null
  apercuImage: string | null = null
  erreurImage = ""
  validationImage = false
  private sequenceImage = 0

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.sequenceImage++
      this.libererApercu()
    })
  }

  choisirSourceImage(source: "URL" | "FICHIER"): void {
    if (this.enregistrement || source === this.sourceImage) return
    this.sourceImage = source
    this.retirerImage()
    if (source === "FICHIER") this.formulaire.controls.image.disable()
    else this.formulaire.controls.image.enable()
  }

  async importerImage(event: Event): Promise<void> {
    if (this.enregistrement) return
    const champ = event.target as HTMLInputElement
    const fichier = champ.files?.[0]
    champ.value = ""
    if (!fichier) return
    this.retirerImage()
    const sequence = this.sequenceImage
    this.validationImage = true
    try {
      await validerPhoto(fichier)
      if (sequence !== this.sequenceImage) return
      this.fichier = fichier
      this.apercuImage = URL.createObjectURL(fichier)
    } catch (erreur) {
      if (sequence === this.sequenceImage) this.erreurImage = erreur instanceof Error ? erreur.message : "Cette image est invalide."
    } finally {
      if (sequence === this.sequenceImage) this.validationImage = false
    }
  }

  retirerImage(): void {
    if (this.enregistrement) return
    this.sequenceImage++
    this.validationImage = false
    this.fichier = null
    this.erreurImage = ""
    this.libererApercu()
  }

  private libererApercu(): void {
    if (this.apercuImage) URL.revokeObjectURL(this.apercuImage)
    this.apercuImage = null
  }

  aUneErreur(champ: keyof typeof this.formulaire.controls): boolean {
    const control = this.formulaire.controls[champ]
    return control.touched && control.invalid
  }

  enregistrer(): void {
    if (this.enregistrement || this.validationImage) return
    this.formulaire.markAllAsTouched()
    if (this.formulaire.invalid) return
    if (this.sourceImage === "FICHIER" && this.erreurImage) return

    const valeurs = this.formulaire.getRawValue()
    const jeu: JeuCreation = {
      nom: valeurs.nom.trim(),
      type: TypeJeu.SOCIETE,
      nbJoueursMin: valeurs.nbJoueursMin!,
      nbJoueursMax: valeurs.nbJoueursMax!,
      description: valeurs.description.trim() || null,
      image: this.sourceImage === "URL" ? valeurs.image.trim() || null : null,
      plateforme: null,
      dureeEstimee: null,
    }
    this.enregistrement = true
    this.enregistrementChange.emit(true)
    this.erreur = ""
    const requete = this.sourceImage === "FICHIER" && this.fichier
      ? this.service.createAvecImage(jeu, this.fichier)
      : this.service.create(jeu)
    requete.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (cree) => { this.router.navigate(["/jeux", cree.id]) },
      error: (erreur: HttpErrorResponse) => {
        this.enregistrement = false
        this.enregistrementChange.emit(false)
        this.erreur = erreur.status === 0
          ? "L’API PlayNight ne répond pas. Vérifiez qu’elle est lancée, puis réessayez."
          : erreur.status === 413
            ? "L’image ne doit pas dépasser 2 Mo."
          : erreur.status === 400 && typeof erreur.error?.message === "string"
            ? erreur.error.message
            : erreur.status === 409
              ? "Le jeu n’a pas pu être enregistré car certaines informations sont en conflit. Vérifiez les champs."
              : "Le jeu n’a pas pu être enregistré. Réessayez dans un instant."
      },
    })
  }
}
