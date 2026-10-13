import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { catchError, forkJoin, map, of } from "rxjs"
import { Jeu } from "models/jeu.model"
import { Joueur } from "models/joueur.model"
import { JeuService } from "services/jeu.service"
import { JoueurService } from "services/joueur.service"

export interface NouvelleSoireePageData {
  joueurs: Joueur[]
  jeux: Jeu[]
  error: boolean
}

// Charge les joueurs (étape 1) et les jeux (étape 2) en parallèle, avant d'afficher le formulaire
export const NouvelleSoireeResolver: ResolveFn<NouvelleSoireePageData> = () =>
  forkJoin({
    joueurs: inject(JoueurService).findAll(),
    jeux: inject(JeuService).findAll(),
  }).pipe(
    map(({ joueurs, jeux }) => ({ joueurs, jeux, error: false })),
    catchError(() => of({ joueurs: [], jeux: [], error: true })),
  )
