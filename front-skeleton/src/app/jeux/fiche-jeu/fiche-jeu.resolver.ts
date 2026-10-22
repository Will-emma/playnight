import { HttpErrorResponse } from "@angular/common/http"
import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, forkJoin, map, of } from "rxjs"
import { ClassementElo, FicheJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"

export interface FicheJeuPageData {
  fiche: FicheJeu | null
  classement: ClassementElo[]
  erreurClassement: boolean
  // "introuvable" : le jeu n'existe pas (404) ; "indisponible" : l'API ne répond pas ou a échoué
  erreur: "introuvable" | "indisponible" | null
}

export const FicheJeuResolver: ResolveFn<FicheJeuPageData> = (route) => {
  const id = Number(route.paramMap.get("id"))
  // Une adresse comme /jeux/abc ne peut correspondre à aucun jeu : inutile d'appeler l'API
  if (!Number.isInteger(id) || id < 1) return of({ fiche: null, classement: [], erreurClassement: false, erreur: "introuvable" })

  const service = inject(JeuService)
  return forkJoin({
    fiche: service.getFiche(id),
    elo: service.getClassement(id).pipe(
      map((classement) => ({ classement, erreurClassement: false })),
      catchError(() => of({ classement: [], erreurClassement: true })),
    ),
  }).pipe(
    map(({ fiche, elo }): FicheJeuPageData => ({ fiche, ...elo, erreur: null })),
    catchError((erreur: HttpErrorResponse) =>
      of<FicheJeuPageData>({ fiche: null, classement: [], erreurClassement: false, erreur: erreur.status === 404 ? "introuvable" : "indisponible" }),
    ),
  )
}
