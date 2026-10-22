import { inject } from "@angular/core"
import { ResolveFn } from "@angular/router"
import { Observable, catchError, forkJoin, map, of, switchMap } from "rxjs"
import { Jeu } from "models/jeu.model"
import { Joueur } from "models/joueur.model"
import { SoireeDetail, SoireeResume } from "models/soiree.model"
import { JeuService } from "services/jeu.service"
import { JoueurService } from "services/joueur.service"
import { SoireeService } from "services/soiree.service"
import { JoueurClassement, comparerClassement } from "utils/classement"

export interface HomePageData {
  jeux: Jeu[]
  soirees: SoireeResume[] // de la plus récente à la plus ancienne (ordre renvoyé par le back)
  classement: JoueurClassement[] // tous les joueurs, déjà triés (voir utils/classement.ts)
  derniereSoiree: SoireeDetail | null // null : aucune soirée pour l'instant
  // "indisponible" : l'API ne répond pas ou a échoué ; la page reste affichée, sans les blocs de données
  erreur: "indisponible" | null
}

export const HomeResolver: ResolveFn<HomePageData> = () => {
  const joueurService = inject(JoueurService)
  const soireeService = inject(SoireeService)

  // Profil de chaque joueur, pour connaître ses victoires et ses parties (comme la page Joueurs)
  const chargerClassement = (joueurs: Joueur[]): Observable<JoueurClassement[]> =>
    // forkJoin sur une liste vide ne renverrait jamais rien : on répond tout de suite
    joueurs.length === 0
      ? of([])
      : forkJoin(
          joueurs.map((joueur) =>
            joueurService.getProfil(joueur.id).pipe(
              map((profil) => ({ joueur, nbVictoires: profil.nbVictoires, nbParties: profil.nbParties })),
            ),
          ),
        ).pipe(map((classement) => classement.sort(comparerClassement)))

  // forkJoin lance les appels en même temps et attend qu'ils soient tous terminés
  return forkJoin({
    jeux: inject(JeuService).findAll(),
    soirees: soireeService.findAll(),
    joueurs: joueurService.findAll(),
  }).pipe(
    // Deuxième vague : elle a besoin des joueurs et de l'id de la soirée la plus récente
    switchMap(({ jeux, soirees, joueurs }) =>
      forkJoin({
        classement: chargerClassement(joueurs),
        derniereSoiree: soirees.length > 0 ? soireeService.findById(soirees[0].id) : of(null),
      }).pipe(map(({ classement, derniereSoiree }): HomePageData => ({ jeux, soirees, classement, derniereSoiree, erreur: null }))),
    ),
    catchError(() =>
      of<HomePageData>({ jeux: [], soirees: [], classement: [], derniereSoiree: null, erreur: "indisponible" }),
    ),
  )
}
