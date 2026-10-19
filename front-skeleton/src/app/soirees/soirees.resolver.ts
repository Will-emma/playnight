import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, map, of } from "rxjs"
import { SoireeResume } from "models/soiree.model"
import { SoireeService } from "services/soiree.service"

export interface SoireesPageData {
  soirees: SoireeResume[]
  // "indisponible" : l'API ne répond pas ou a échoué (une liste ne peut pas être "introuvable", au pire elle est vide)
  erreur: "indisponible" | null
}

export const SoireesResolver: ResolveFn<SoireesPageData> = () =>
  inject(SoireeService).findAll().pipe(
    map((soirees): SoireesPageData => ({ soirees, erreur: null })),
    catchError(() => of<SoireesPageData>({ soirees: [], erreur: "indisponible" })),
  )
