import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, map, of } from "rxjs"
import { Joueur } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"

export interface JoueursPageData {
  joueurs: Joueur[]
  error: boolean
}

export const JoueursResolver: ResolveFn<JoueursPageData> = () =>
  inject(JoueurService).findAll().pipe(
    map((joueurs) => ({ joueurs, error: false })),
    catchError(() => of({ joueurs: [], error: true })),
  )
