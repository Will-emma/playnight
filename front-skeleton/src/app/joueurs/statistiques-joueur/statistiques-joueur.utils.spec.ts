import { TypeJeu } from "models/jeu.model"
import { PartieHistorique } from "models/joueur.model"
import { calculerStatistiques } from "./statistiques-joueur.utils"

describe("Calcul des statistiques du joueur", () => {
  const maintenant = new Date(2026, 9, 24)
  function partie(id: number, jeuId: number, rang: number, date = "2026-10-03T20:00:00", nom = `Jeu ${jeuId}`): PartieHistorique {
    return { partieId: id, date, jeu: { id: jeuId, nom, type: TypeJeu.SOCIETE, image: null }, rang, points: null }
  }

  it("compte les rangs 1, y compris équipe ou ex aequo, sans utiliser les points", () => {
    const historique = [partie(1, 8, 1), partie(2, 8, 1), { ...partie(3, 8, 2), points: 999 }, partie(4, 9, 1)]
    expect(calculerStatistiques(historique, maintenant).victoires).toEqual([
      { jeuId: 8, nom: "Jeu 8", victoires: 2 }, { jeuId: 9, nom: "Jeu 9", victoires: 1 },
    ])
  })

  it("distingue deux jeux homonymes et inclut les jeux sans victoire", () => {
    const s = calculerStatistiques([partie(1, 10, 1, undefined, "Catan"), partie(2, 20, 2, undefined, "Catan")], maintenant)
    expect(s.victoires).toEqual([{ jeuId: 10, nom: "Catan", victoires: 1 }, { jeuId: 20, nom: "Catan", victoires: 0 }])
  })

  it("utilise plus de dix parties et trie les égalités par nom puis identifiant", () => {
    const historique = Array.from({ length: 14 }, (_, i) => partie(i + 1, 8, 1))
    historique.push(partie(30, 30, 1, undefined, "Zelda"), partie(31, 40, 1, undefined, "Azul"), partie(32, 20, 1, undefined, "Azul"))
    const s = calculerStatistiques(historique, maintenant)
    expect(s.victoires.map(j => j.jeuId)).toEqual([8, 20, 40, 30])
    expect(s.victoires[0].victoires).toBe(14)
    expect(s.totalPeriode).toBe(17)
  })

  it("produit douze mois chronologiques, mois courant compris, avec des zéros", () => {
    const s = calculerStatistiques([partie(1, 8, 2, "2025-11-01T00:00:00"), partie(2, 8, 1, "2026-10-31T23:59:59")], maintenant)
    expect(s.mois.length).toBe(12)
    expect(s.mois[0].cle).toBe("2025-11")
    expect(s.mois[11].cle).toBe("2026-10")
    expect(s.mois.map(m => m.parties)).toEqual([1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1])
  })

  it("respecte décembre/janvier et ignore les mois hors période pour la courbe", () => {
    const s = calculerStatistiques([
      partie(1, 8, 1, "2025-01-31T23:59:59"), partie(2, 8, 1, "2025-12-31T23:59:59"),
      partie(3, 8, 1, "2026-01-01T00:00:00"), partie(4, 8, 1, "2026-02-01T00:00:00"),
    ], new Date(2026, 0, 1))
    expect(s.mois[0].cle).toBe("2025-02")
    expect(s.mois[10].parties).toBe(1)
    expect(s.mois[11].parties).toBe(1)
    expect(s.totalPeriode).toBe(2)
    expect(s.victoires[0].victoires).toBe(4) // le diagramme en barres couvre tout l'historique
  })

  it("garde le mois local près de minuit sans décalage UTC", () => {
    const s = calculerStatistiques([partie(1, 8, 1, "2026-03-01T00:01:00"), partie(2, 8, 2, "2026-03-31T23:59:59")], maintenant)
    expect(s.mois.find(m => m.cle === "2026-03")?.parties).toBe(2)
    expect(s.mois.find(m => m.cle === "2026-02")?.parties).toBe(0)
    expect(s.mois.find(m => m.cle === "2026-04")?.parties).toBe(0)
  })

  it("ne double pas une partie et ne modifie pas l'historique reçu", () => {
    const p = Object.freeze(partie(1, 8, 1))
    const historique = Object.freeze([p, p])
    const s = calculerStatistiques(historique, maintenant)
    expect(s.victoires[0].victoires).toBe(1)
    expect(s.totalPeriode).toBe(1)
    expect(historique.length).toBe(2)
  })

  it("renvoie un historique vide et douze mois à zéro sans inventer de jeu", () => {
    const s = calculerStatistiques([], maintenant)
    expect(s.victoires).toEqual([])
    expect(s.mois.length).toBe(12)
    expect(s.totalPeriode).toBe(0)
  })
})
