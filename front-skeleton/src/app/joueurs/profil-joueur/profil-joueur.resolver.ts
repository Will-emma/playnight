import { HttpErrorResponse } from "@angular/common/http"
import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, forkJoin, map, of } from "rxjs"
import { ClassementGlobal, ClassementJoueur, ProfilJoueur } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"

export interface ProfilJoueurPageData {
  profil: ProfilJoueur | null
  classementGlobal: ClassementGlobal | null
  classements: ClassementJoueur[]
  erreurClassements: boolean
  // "introuvable" : le joueur n'existe pas (404) ; "indisponible" : l'API ne répond pas ou a échoué
  erreur: "introuvable" | "indisponible" | null
}

export const ProfilJoueurResolver: ResolveFn<ProfilJoueurPageData> = (route) => {
  const id = Number(route.paramMap.get("id"))
  // Une adresse comme /joueurs/abc ne peut correspondre à aucun joueur : inutile d'appeler l'API
  if (!Number.isInteger(id) || id < 1) return of({ profil: null, classementGlobal: null, classements: [], erreurClassements: false, erreur: "introuvable" })

  const service = inject(JoueurService)
  return forkJoin({
    profil: service.getProfil(id),
    classementGlobal: service.getClassementGlobal(id).pipe(catchError(() => of(null))),
    elo: service.getClassements(id).pipe(
      map((classements) => ({ classements, erreurClassements: false })),
      catchError(() => of({ classements: [], erreurClassements: true })),
    ),
  }).pipe(
    map(({ profil, elo, classementGlobal }): ProfilJoueurPageData => ({ profil, ...elo, classementGlobal, erreur: null })),
    catchError((erreur: HttpErrorResponse) =>
      of<ProfilJoueurPageData>({ profil: null, classementGlobal: null, classements: [], erreurClassements: false, erreur: erreur.status === 404 ? "introuvable" : "indisponible" }),
    ),
  )
}
