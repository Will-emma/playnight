import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { Observable, catchError, forkJoin, map, of, switchMap } from "rxjs"
import { SoireeDetail } from "models/soiree.model"
import { SoireeService } from "services/soiree.service"

export interface SoireesPageData {
  soirees: SoireeDetail[] // de la plus récente à la plus ancienne (ordre renvoyé par le back)
  // "indisponible" : l'API ne répond pas ou a échoué (une liste ne peut pas être "introuvable", au pire elle est vide)
  erreur: "indisponible" | null
}

// La liste ne donne que le nombre de parties : on charge le détail de chaque soirée
// pour avoir les jeux joués, les vainqueurs et les durées (une requête par soirée)
// Utilisée par le resolver et par le bouton "Réessayer" de la page
export function chargerSoirees(soireeService: SoireeService): Observable<SoireeDetail[]> {
  return soireeService.findAll().pipe(
    // forkJoin sur une liste vide ne renverrait jamais rien : on répond tout de suite
    switchMap((soirees) =>
      soirees.length === 0 ? of([]) : forkJoin(soirees.map((soiree) => soireeService.findById(soiree.id))),
    ),
  )
}

export const SoireesResolver: ResolveFn<SoireesPageData> = () =>
  chargerSoirees(inject(SoireeService)).pipe(
    map((soirees): SoireesPageData => ({ soirees, erreur: null })),
    catchError(() => of<SoireesPageData>({ soirees: [], erreur: "indisponible" })),
  )
