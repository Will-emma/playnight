import { Joueur } from "models/joueur.model"

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
