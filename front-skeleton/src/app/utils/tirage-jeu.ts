import { Jeu, TypeJeu } from "models/jeu.model"

export interface CriteresTirage {
  type?: TypeJeu | "TOUS"
  nombreJoueurs?: number | null
  idsExclus?: ReadonlySet<number>
}

function entierPositif(valeur: number | null | undefined): valeur is number {
  return valeur != null && Number.isSafeInteger(valeur) && valeur >= 1
}

// Un nombre absent désactive le filtre d'effectif ; un nombre invalide ne donne
// aucun candidat. Ne jamais interpréter des limites inconnues comme une garantie
// de compatibilité. La liste, les jeux et les exclusions restent inchangés.
export function filtrerJeux(jeux: readonly Jeu[], criteres: CriteresTirage = {}): Jeu[] {
  const { type = "TOUS", nombreJoueurs, idsExclus } = criteres
  if (nombreJoueurs != null && !entierPositif(nombreJoueurs)) return []

  return jeux.filter((jeu) => {
    if (type !== "TOUS" && jeu.type !== type) return false
    if (idsExclus?.has(jeu.id)) return false
    if (nombreJoueurs == null) return true

    const minimum = jeu.nbJoueursMin
    const maximum = jeu.nbJoueursMax
    return entierPositif(minimum) && entierPositif(maximum)
      && minimum <= maximum && minimum <= nombreJoueurs && nombreJoueurs <= maximum
  })
}

// Chaque index occupe un intervalle de même longueur dans [0, 1).
// Le hasard injecté doit respecter le contrat de Math.random ; une source
// invalide est signalée plutôt que corrigée, ce qui biaiserait la sélection.
export function tirerJeu(jeuxEligibles: readonly Jeu[], hasard: () => number = Math.random): Jeu | null {
  if (jeuxEligibles.length === 0) return null
  if (jeuxEligibles.length === 1) return jeuxEligibles[0]

  const valeur = hasard()
  if (!Number.isFinite(valeur) || valeur < 0 || valeur >= 1) {
    throw new RangeError("La source de hasard doit renvoyer un nombre compris entre 0 inclus et 1 exclu.")
  }
  return jeuxEligibles[Math.floor(valeur * jeuxEligibles.length)]
}
