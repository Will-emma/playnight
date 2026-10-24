import { PartieHistorique } from "models/joueur.model"

export interface VictoiresParJeu { jeuId: number; nom: string; victoires: number }
export interface PartiesParMois { cle: string; libelle: string; parties: number }
export interface StatistiquesJoueur {
  victoires: VictoiresParJeu[]
  mois: PartiesParMois[]
  totalPeriode: number
}

// Calcul indépendant du canvas ; la date de référence est injectable dans les tests.
export function calculerStatistiques(historique: readonly PartieHistorique[], maintenant = new Date()): StatistiquesJoueur {
  const jeux = new Map<number, VictoiresParJeu>()
  const mois: PartiesParMois[] = []
  const format = new Intl.DateTimeFormat("fr-FR", { month: "short", year: "numeric" })
  for (let recul = 11; recul >= 0; recul--) {
    const date = new Date(maintenant.getFullYear(), maintenant.getMonth() - recul, 1)
    mois.push({
      cle: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`,
      libelle: format.format(date), parties: 0,
    })
  }
  const moisParCle = new Map(mois.map(m => [m.cle, m]))
  const partiesVues = new Set<number>()
  for (const partie of historique) {
    if (partiesVues.has(partie.partieId)) continue
    partiesVues.add(partie.partieId)
    const jeu = jeux.get(partie.jeu.id) ?? { jeuId: partie.jeu.id, nom: partie.jeu.nom, victoires: 0 }
    if (partie.rang === 1) jeu.victoires++
    jeux.set(jeu.jeuId, jeu)
    // Dates API sans fuseau : garder le mois écrit, sans Date.parse ni conversion UTC.
    const moisLocal = moisParCle.get(partie.date.slice(0, 7))
    if (moisLocal) moisLocal.parties++
  }
  return {
    victoires: [...jeux.values()].sort((a, b) => b.victoires - a.victoires || a.nom.localeCompare(b.nom, "fr") || a.jeuId - b.jeuId),
    mois, totalPeriode: mois.reduce((total, m) => total + m.parties, 0),
  }
}
