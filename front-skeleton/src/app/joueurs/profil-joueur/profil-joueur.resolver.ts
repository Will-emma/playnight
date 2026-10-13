import { HttpErrorResponse } from "@angular/common/http"
import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, map, of } from "rxjs"
import { ProfilJoueur } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"

export interface ProfilJoueurPageData {
  profil: ProfilJoueur | null
  // "introuvable" : le joueur n'existe pas (404) ; "indisponible" : l'API ne répond pas ou a échoué
  erreur: "introuvable" | "indisponible" | null
}

export const ProfilJoueurResolver: ResolveFn<ProfilJoueurPageData> = (route) => {
  const id = Number(route.paramMap.get("id"))
  // Une adresse comme /joueurs/abc ne peut correspondre à aucun joueur : inutile d'appeler l'API
  if (!Number.isInteger(id) || id < 1) return of({ profil: null, erreur: "introuvable" })

  return inject(JoueurService).getProfil(id).pipe(
    map((profil): ProfilJoueurPageData => ({ profil, erreur: null })),
    catchError((erreur: HttpErrorResponse) =>
      of<ProfilJoueurPageData>({ profil: null, erreur: erreur.status === 404 ? "introuvable" : "indisponible" }),
    ),
  )
}
