import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, map, of } from "rxjs"
import { JeuService } from "services/jeu.service"
import { Jeu } from "models/jeu.model"

export interface JeuxPageData {
  jeux: Jeu[]
  error: boolean
}

export const JeuxResolver: ResolveFn<JeuxPageData> = () =>
  inject(JeuService).findAll().pipe(
    map((jeux) => ({ jeux, error: false })),
    catchError(() => of({ jeux: [], error: true })),
  )
