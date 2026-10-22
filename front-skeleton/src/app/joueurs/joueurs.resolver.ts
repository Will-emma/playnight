import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, map, of, switchMap } from "rxjs"
import { JoueurService } from "services/joueur.service"
import { JoueurClassement, chargerClassement } from "utils/classement"

export interface JoueursPageData {
  classement: JoueurClassement[] // tous les joueurs, déjà triés (voir utils/classement.ts)
  // "indisponible" : l'API ne répond pas ou a échoué
  erreur: "indisponible" | null
}

export const JoueursResolver: ResolveFn<JoueursPageData> = () => {
  const joueurService = inject(JoueurService)
  // Les joueurs, puis le profil de chacun pour ses victoires et ses parties
  return joueurService.findAll().pipe(
    switchMap((joueurs) => chargerClassement(joueurService, joueurs)),
    map((classement): JoueursPageData => ({ classement, erreur: null })),
    catchError(() => of<JoueursPageData>({ classement: [], erreur: "indisponible" })),
  )
}
