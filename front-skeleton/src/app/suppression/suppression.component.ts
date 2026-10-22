import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from "@angular/core"
import { HttpErrorResponse } from "@angular/common/http"
import { Observable } from "rxjs"

// Bouton "Supprimer…" et sa fenêtre de confirmation, partagés par le détail d'une soirée, le profil d'un joueur et la fiche d'un jeu.
// La page fournit l'appel à l'API (action) et décide où aller une fois la suppression faite (supprime).
@Component({
  selector: "app-suppression",
  templateUrl: "./suppression.component.html",
})
export class SuppressionComponent {
  @Input({ required: true }) libelle!: string // texte du bouton : "Supprimer la soirée"
  @Input({ required: true }) titre!: string // titre de la fenêtre : "Supprimer « Chez Léa » ?"
  @Input({ required: true }) message!: string // conséquences de la suppression
  @Input({ required: true }) action!: () => Observable<void>
  // Affiché si l'API refuse la suppression (409) : par exemple un joueur qui a joué entre-temps
  @Input() messageConflit = "Cet élément est encore utilisé ailleurs : il ne peut pas être supprimé."
  @Output() supprime = new EventEmitter<void>()

  @ViewChild("fenetre") fenetre!: ElementRef<HTMLDialogElement>

  enCours = false
  erreur = ""

  ouvrir(): void {
    this.erreur = ""
    // showModal : la fenêtre passe au premier plan, bloque la page derrière et se ferme avec Échap
    this.fenetre.nativeElement.showModal()
  }

  fermer(): void {
    if (!this.enCours) this.fenetre.nativeElement.close()
  }

  // Échap pendant la suppression : on laisse la fenêtre ouverte jusqu'à la réponse de l'API
  empecherFermeture(evenement: Event): void {
    if (this.enCours) evenement.preventDefault()
  }

  confirmer(): void {
    this.enCours = true
    this.erreur = ""
    this.action().subscribe({
      next: () => {
        this.enCours = false
        this.fenetre.nativeElement.close()
        this.supprime.emit()
      },
      error: (erreur: HttpErrorResponse) => {
        this.erreur =
          erreur.status === 409
            ? this.messageConflit
            : "La suppression n’a pas pu être faite. Vérifiez que l’API PlayNight est lancée, puis réessayez."
        this.enCours = false
      },
    })
  }
}
