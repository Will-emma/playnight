import { Component, Inject, OnDestroy } from "@angular/core"
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog"
import { HttpErrorResponse } from "@angular/common/http"
import { Joueur } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"
import { AVATARS, validerPhoto } from "utils/avatar"

@Component({
  selector: "app-avatar-dialog",
  templateUrl: "./avatar-dialog.component.html",
  styleUrls: ["./avatar-dialog.component.scss"],
})
export class AvatarDialogComponent implements OnDestroy {
  readonly avatars = AVATARS
  onglet = 0
  avatarId: number | null = null
  fichier: File | null = null
  photoUrl: string | null = null
  erreur = ""
  validation = false
  enregistrement = false
  apercuIndisponible = false
  private sequenceValidation = 0

  constructor(
    @Inject(MAT_DIALOG_DATA) readonly joueur: Joueur,
    private dialogRef: MatDialogRef<AvatarDialogComponent, Joueur>,
    private joueurService: JoueurService,
  ) {
    this.avatarId = AVATARS.find((a) => a.url === joueur.avatar)?.id ?? null
  }

  get apercu(): string | null {
    return (this.onglet === 0 ? AVATARS.find((a) => a.id === this.avatarId)?.url : this.photoUrl) ?? this.joueur.avatar
  }

  get peutEnregistrer(): boolean {
    return !this.validation && !this.enregistrement && (this.onglet === 0 ? this.avatarId !== null : this.fichier !== null)
  }

  changerOnglet(index: number): void {
    this.onglet = index
    this.erreur = ""
    this.apercuIndisponible = false
  }

  choisir(id: number): void {
    this.avatarId = id
    this.erreur = ""
    this.apercuIndisponible = false
  }

  async importer(event: Event): Promise<void> {
    const champ = event.target as HTMLInputElement
    const fichier = champ.files?.[0]
    champ.value = ""
    if (!fichier) return
    const sequence = ++this.sequenceValidation
    this.libererPhoto()
    this.fichier = null
    this.erreur = ""
    this.validation = true
    try {
      await validerPhoto(fichier)
      if (sequence !== this.sequenceValidation) return
      this.photoUrl = URL.createObjectURL(fichier)
      this.fichier = fichier
      this.apercuIndisponible = false
    } catch (e) {
      if (sequence === this.sequenceValidation) this.erreur = e instanceof Error ? e.message : "Cette image est invalide."
    } finally {
      if (sequence === this.sequenceValidation) this.validation = false
    }
  }

  enregistrer(): void {
    if (!this.peutEnregistrer) return
    this.enregistrement = true
    this.dialogRef.disableClose = true
    const requete = this.onglet === 0
      ? this.joueurService.choisirAvatar(this.joueur.id, this.avatarId!)
      : this.joueurService.importerAvatar(this.joueur.id, this.fichier!)
    requete.subscribe({
      next: (joueur) => this.dialogRef.close(joueur),
      error: (e: HttpErrorResponse) => {
        this.enregistrement = false
        this.dialogRef.disableClose = false
        this.erreur = e.status === 0 ? "Impossible de joindre le serveur. Vérifiez que l’API PlayNight est démarrée et à jour."
          : e.status === 404 ? "Le joueur ou le service d’avatar est introuvable. Rechargez la page ; si le problème persiste, redémarrez l’API PlayNight."
          : e.status === 405 ? "Le serveur doit être mis à jour puis redémarré pour modifier les avatars."
          : e.status === 413 ? "La photo ne doit pas dépasser 2 Mo."
          : e.status === 400 && typeof e.error?.message === "string" ? e.error.message
          : "L’avatar n’a pas pu être enregistré par le serveur. Réessayez."
      },
    })
  }

  ngOnDestroy(): void {
    this.sequenceValidation++
    this.libererPhoto()
  }

  private libererPhoto(): void {
    if (this.photoUrl) URL.revokeObjectURL(this.photoUrl)
    this.photoUrl = null
  }
}
