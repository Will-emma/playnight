import { HttpErrorResponse } from "@angular/common/http"
import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, forkJoin, map, of } from "rxjs"
import { Jeu, MeilleurJoueur } from "models/jeu.model"
import { JeuService } from "services/jeu.service"

export interface ClassementVictoiresPageData {
  jeuId: number | null
  jeu: Jeu | null
  classement: MeilleurJoueur[]
  erreur: "introuvable" | "indisponible" | null
}

export const ClassementVictoiresResolver: ResolveFn<ClassementVictoiresPageData> = (route) => {
  const id = Number(route.paramMap.get("id"))
  if (!Number.isSafeInteger(id) || id < 1) {
    return of({ jeuId: null, jeu: null, classement: [], erreur: "introuvable" })
  }
  const service = inject(JeuService)
  // Le détail simple donne le nom du jeu sans charger les parties ni le classement Elo.
  return forkJoin({ jeu: service.getById(id), classement: service.getClassementVictoires(id) }).pipe(
    map(({ jeu, classement }): ClassementVictoiresPageData => ({ jeuId: id, jeu, classement, erreur: null })),
    catchError((erreur: HttpErrorResponse) => of<ClassementVictoiresPageData>({
      jeuId: id, jeu: null, classement: [], erreur: erreur.status === 404 ? "introuvable" : "indisponible",
    })),
  )
}
