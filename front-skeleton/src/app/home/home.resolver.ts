import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, forkJoin, map, of } from "rxjs"
import { SoireeResume } from "models/soiree.model"
import { JeuService } from "services/jeu.service"
import { SoireeService } from "services/soiree.service"

export interface HomePageData {
  nbJeux: number
  soirees: SoireeResume[] // de la plus récente à la plus ancienne (ordre renvoyé par le back)
  // "indisponible" : l'API ne répond pas ou a échoué ; la page reste affichée, sans les chiffres ni les soirées
  erreur: "indisponible" | null
}

export const HomeResolver: ResolveFn<HomePageData> = () =>
  // forkJoin lance les deux appels en même temps et attend qu'ils soient tous les deux terminés
  forkJoin({
    jeux: inject(JeuService).findAll(),
    soirees: inject(SoireeService).findAll(),
  }).pipe(
    map(({ jeux, soirees }): HomePageData => ({ nbJeux: jeux.length, soirees, erreur: null })),
    catchError(() => of<HomePageData>({ nbJeux: 0, soirees: [], erreur: "indisponible" })),
  )
