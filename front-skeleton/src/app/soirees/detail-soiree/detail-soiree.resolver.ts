import { HttpErrorResponse } from "@angular/common/http"
import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, map, of } from "rxjs"
import { SoireeDetail } from "models/soiree.model"
import { SoireeService } from "services/soiree.service"

export interface DetailSoireePageData {
  soiree: SoireeDetail | null
  // "introuvable" : la soirée n'existe pas (404) ; "indisponible" : l'API ne répond pas ou a échoué
  erreur: "introuvable" | "indisponible" | null
}

export const DetailSoireeResolver: ResolveFn<DetailSoireePageData> = (route) => {
  const id = Number(route.paramMap.get("id"))
  // Une adresse comme /soirees/abc ne peut correspondre à aucune soirée : inutile d'appeler l'API
  if (!Number.isInteger(id) || id < 1) return of({ soiree: null, erreur: "introuvable" })

  return inject(SoireeService).findById(id).pipe(
    map((soiree): DetailSoireePageData => ({ soiree, erreur: null })),
    catchError((erreur: HttpErrorResponse) =>
      of<DetailSoireePageData>({ soiree: null, erreur: erreur.status === 404 ? "introuvable" : "indisponible" }),
    ),
  )
}
