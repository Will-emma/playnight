import { Observable, forkJoin, map, of } from "rxjs"
import { Joueur } from "models/joueur.model"
import { JoueurService } from "services/joueur.service"

// Une ligne du classement des joueurs (page Joueurs et podium de l'accueil)
export interface JoueurClassement {
  joueur: Joueur
  nbVictoires: number
  nbParties: number
}

// Règle commune aux deux pages : le plus de victoires d'abord, puis le plus de parties, puis le pseudo
export function comparerClassement(a: JoueurClassement, b: JoueurClassement): number {
  return (
    b.nbVictoires - a.nbVictoires ||
    b.nbParties - a.nbParties ||
    a.joueur.pseudo.localeCompare(b.joueur.pseudo, "fr", { sensitivity: "base" })
  )
}

// Profil de chaque joueur, pour connaître ses victoires et ses parties ; renvoie le classement déjà trié
export function chargerClassement(joueurService: JoueurService, joueurs: Joueur[]): Observable<JoueurClassement[]> {
  // forkJoin sur une liste vide ne renverrait jamais rien : on répond tout de suite
  if (joueurs.length === 0) return of([])
  return forkJoin(
    joueurs.map((joueur) =>
      joueurService.getProfil(joueur.id).pipe(
        map((profil) => ({ joueur, nbVictoires: profil.nbVictoires, nbParties: profil.nbParties })),
      ),
    ),
  ).pipe(map((classement) => classement.sort(comparerClassement)))
}
