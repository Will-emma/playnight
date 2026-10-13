import { HttpErrorResponse } from "@angular/common/http"
import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, map, of } from "rxjs"
import { FicheJeu } from "models/jeu.model"
import { JeuService } from "services/jeu.service"

export interface FicheJeuPageData {
  fiche: FicheJeu | null
  // "introuvable" : le jeu n'existe pas (404) ; "indisponible" : l'API ne répond pas ou a échoué
  erreur: "introuvable" | "indisponible" | null
}

export const FicheJeuResolver: ResolveFn<FicheJeuPageData> = (route) => {
  const id = Number(route.paramMap.get("id"))
  // Une adresse comme /jeux/abc ne peut correspondre à aucun jeu : inutile d'appeler l'API
  if (!Number.isInteger(id) || id < 1) return of({ fiche: null, erreur: "introuvable" })

  return inject(JeuService).getFiche(id).pipe(
    map((fiche): FicheJeuPageData => ({ fiche, erreur: null })),
    catchError((erreur: HttpErrorResponse) =>
      of<FicheJeuPageData>({ fiche: null, erreur: erreur.status === 404 ? "introuvable" : "indisponible" }),
    ),
  )
}
